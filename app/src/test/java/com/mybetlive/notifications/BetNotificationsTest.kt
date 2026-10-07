package com.mybetlive.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.mybetlive.R
import com.mybetlive.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BetNotificationsTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val manager get()=context.getSystemService(NotificationManager::class.java)
    private fun payload(status:String="LIVE_WINNING",time:String="2026-10-03T20:00:00Z")=mapOf(
        "betId" to "test-bet", "sport" to "SOCCER", "market" to "MONEYLINE", "selection" to "DRAW",
        "homeName" to "Local", "awayName" to "Visita", "homeScore" to "2", "awayScore" to "1",
        "status" to status, "updatedAt" to time, "final" to (status in settledStatuses).toString())
    private fun nbaBet(status: String="LIVE_WINNING", value: Double?=26.0) = BetView("nba-prop","nba-game",
        EventSnapshot("NBA","DEMO_TEST","game","2026-10-04T20:00:00Z",Team("home","Lakers"),Team("away","Celtics"),98,104,
            "LIVE","Q4","05:42","2026-10-04T20:00:00Z"),"PLAYER_POINTS","OVER",24.5,null,"player","LeBron James",
        null,null,null,status,value,null,"2026-10-04T20:00:00Z",false)
    @Before fun reset(){BetNotifications(context).clear()}
    @Test fun liveNotificationHasActionsAndMLArtworkInsteadOfAProgressBar(){
        BetNotifications(context).show(payload())
        val n=manager.activeNotifications.single().notification
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT!=0);assertNotNull(n.bigContentView)
        assertEquals(listOf("Ver seguimiento","Desanclar"),n.actions.map{it.title.toString()})
    }
    @Test fun unpinPreventsRepinningButAllowsFinalAndRejectsOlderSnapshot(){
        val notifications=BetNotifications(context);notifications.show(payload());notifications.unpin("test-bet")
        assertEquals(0,manager.activeNotifications.size);notifications.show(payload());assertEquals(0,manager.activeNotifications.size)
        notifications.show(payload("WON","2026-10-03T20:10:00Z"))
        assertEquals(1,manager.activeNotifications.size)
        val n=manager.activeNotifications.single().notification;assertEquals(0,n.flags and Notification.FLAG_ONGOING_EVENT)
        notifications.show(payload("LIVE_WINNING","2026-10-03T20:00:00Z"))
        assertEquals(0,manager.activeNotifications.single().notification.flags and Notification.FLAG_ONGOING_EVENT)
    }
    @Test fun dismissedFinalIsNotRepostedByForegroundPolling(){
        val notifications=BetNotifications(context)
        notifications.show(payload())
        val result=payload("WON","2026-10-03T20:10:00Z")
        notifications.show(result)
        assertEquals("tracking-results",manager.activeNotifications.single().notification.channelId)
        manager.cancel("test-bet",1)
        notifications.show(result)
        notifications.show(result+mapOf("artworkRefresh" to "true"))
        assertEquals(0,manager.activeNotifications.size)
    }
    @Test fun aLatePortraitRefreshKeepsTheFinalChannelAndNeverAlertsAgain() {
        val notifications=BetNotifications(context)
        notifications.show(payload())
        val result=payload("WON","2026-10-03T20:10:00Z")
        notifications.show(result)
        notifications.show(result+mapOf("artworkRefresh" to "true"))
        val n=manager.activeNotifications.single().notification
        assertEquals("tracking-results",n.channelId)
        assertTrue(n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
    }
    @Test fun explicitRepinSurvivesInstancesAndKeepsOtherTrackersUnpinned() {
        val notifications=BetNotifications(context)
        val bet=nbaBet()
        notifications.show(bet); notifications.show(bet.copy(id="other"))
        notifications.unpin(bet.id); notifications.unpin("other")
        val restarted=BetNotifications(context)
        restarted.show(bet)
        assertTrue(restarted.activeIds().isEmpty())
        assertTrue(restarted.pin(bet))
        assertEquals(setOf(bet.id),restarted.activeIds())
        assertTrue(restarted.isUnpinned("other"))
        assertFalse(restarted.isUnpinned(bet.id))
    }
    @Test fun deletedScheduledAndSettledTrackersCannotBeRepinned() {
        val notifications=BetNotifications(context)
        assertFalse(notifications.pin(nbaBet("PENDING",null)))
        assertFalse(notifications.pin(nbaBet("WON")))
        notifications.remove("nba-prop")
        assertFalse(notifications.pin(nbaBet()))
        assertTrue(notifications.activeIds().isEmpty())
    }
    @Test fun aNewerPushSnapshotCannotBeOverwrittenByAnOlderRepin() {
        val notifications=BetNotifications(context)
        notifications.show(nbaBet().copy(updatedAt="2026-10-04T20:01:00Z"))
        notifications.unpin("nba-prop")
        assertFalse(notifications.pin(nbaBet()))
        assertTrue(notifications.isUnpinned("nba-prop"))
        assertTrue(notifications.activeIds().isEmpty())
    }
    @Test fun disablingLiveChannelDoesNotClaimPinningAndKeepsTheUserPreference() {
        val notifications=BetNotifications(context)
        notifications.unpin("nba-prop")
        manager.createNotificationChannel(NotificationChannel("tracking-live","Seguimiento en vivo",NotificationManager.IMPORTANCE_NONE))
        assertFalse(notifications.liveEnabled())
        assertFalse(notifications.pin(nbaBet()))
        assertTrue(notifications.isUnpinned("nba-prop"))
        assertTrue(notifications.activeIds().isEmpty())
    }
    @Test @Config(sdk=[35]) fun permissionDeniedCannotProduceAnActiveNotification() {
        shadowOf(context).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val notifications=BetNotifications(context)
        assertFalse(notifications.liveEnabled()); assertFalse(notifications.pin(nbaBet()))
        notifications.show(nbaBet())
        assertTrue(notifications.activeIds().isEmpty())
    }
    @Test fun notificationActionUnpinsAndNotifiesAnOpenAppObserver() {
        val notifications=BetNotifications(context)
        notifications.show(nbaBet())
        var updated=false
        val stop=notifications.observe { updated=true }
        UnpinReceiver().onReceive(context,Intent().putExtra("betId","nba-prop"))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(updated); assertTrue(notifications.isUnpinned("nba-prop")); assertTrue(notifications.activeIds().isEmpty())
        stop()
    }
    @Test fun restrictedAccessDismissesNotificationsWithoutErasingUnpinPreferences() {
        val notifications=BetNotifications(context)
        notifications.show(nbaBet()); notifications.unpin("nba-prop")
        notifications.dismissAll()
        notifications.show(nbaBet())
        assertTrue(notifications.activeIds().isEmpty()); assertTrue(notifications.isUnpinned("nba-prop"))
    }
    @Test fun privateLockScreenHasNoPlayerSelectionScoresOrActions() {
        BetNotifications(context).show(nbaBet())
        val public=manager.activeNotifications.single().notification.publicVersion
        assertNotNull(public)
        assertEquals("MyBetLive",public.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertFalse(public.extras.toString().contains("LeBron")); assertFalse(public.extras.toString().contains("24.5"))
        assertTrue(public.actions.isNullOrEmpty()); assertNull(public.bigContentView)
    }
    @Test fun propsHaveCompactAndExpandedLayoutsWithAvatarAndUnknownIsDifferentFromZero() {
        val notifications=BetNotifications(context)
        notifications.show(nbaBet())
        val winning=manager.activeNotifications.single().notification
        val expanded=render(winning,"nba-prop-ampliada")
        assertEquals(View.VISIBLE,expanded.findViewById<View>(R.id.progress_avatar).visibility)
        assertEquals("26 / 24.5",expanded.findViewById<TextView>(R.id.progress_value).text.toString())
        render(winning,"nba-prop-compacta",compact=true)
        notifications.show(nbaBet("AWAITING_DATA",null).copy(updatedAt="2026-10-04T20:01:00Z"))
        val waiting=render(manager.activeNotifications.single().notification,"nba-prop-sin-datos")
        assertEquals("— / 24.5",waiting.findViewById<TextView>(R.id.progress_value).text.toString())
        assertEquals(View.GONE,waiting.findViewById<View>(R.id.progress_track).visibility)
        notifications.show(nbaBet("LIVE_LOSING",0.0).copy(updatedAt="2026-10-04T20:02:00Z"))
        val zero=render(manager.activeNotifications.single().notification,"nba-prop-cero")
        assertEquals("0 / 24.5",zero.findViewById<TextView>(R.id.progress_value).text.toString())
        assertEquals(View.VISIBLE,zero.findViewById<View>(R.id.progress_track).visibility)
    }
    private fun render(notification: Notification, name: String, compact: Boolean=false): View {
        val parent=FrameLayout(context)
        val view=(if(compact) notification.contentView else notification.bigContentView).apply(context,parent)
        val width=(328*context.resources.displayMetrics.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
        view.layout(0,0,width,view.measuredHeight)
        val custom=view.findViewById<View>(R.id.progress_track).parent as View
        assertTrue("Custom notification too tall: ${custom.height}",custom.height <= ((if(compact) 48 else 252)*context.resources.displayMetrics.density).toInt())
        val bitmap=Bitmap.createBitmap(width,view.height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file=File("build/notification-screenshots/$name.png");file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        return view
    }
    @Test fun nbaNotificationMarketStatesUseTheSameDataAndCompactExpandedBounds() {
        val notifications=BetNotifications(context)
        val base=nbaBet()
        val ml=base.copy(market="MONEYLINE",selection="HOME",line=null,playerId=null,playerName=null,currentValue=-6.0,status="LIVE_LOSING")
        val spread=ml.copy(market="SPREAD",line=6.5,status="LIVE_WINNING")
        val total=ml.copy(market="TOTAL",selection="UNDER",line=219.5,currentValue=202.0,status="LIVE_WINNING")
        val cases=mapOf("ml-desfavorable" to ml,
            "ml-favorable" to ml.copy(selection="AWAY",currentValue=6.0,status="LIVE_WINNING"),
            "ml-prorroga" to ml.copy(currentValue=0.0,status="LIVE_PUSH",event=ml.event.copy(homeScore=104,awayScore=104,period="OT1",clock="04:52")),
            "ml-guardado" to ml.copy(stale=true),
            "handicap-favorable" to spread,"handicap-push" to spread.copy(line=6.0,status="LIVE_PUSH"),
            "total-under-favorable" to total,"total-under-desfavorable" to total.copy(currentValue=222.0,status="LIVE_LOSING"),
            "prop-under-favorable" to base.copy(selection="UNDER",currentValue=18.0,status="LIVE_WINNING"),
            "prop-over-desfavorable" to base.copy(currentValue=18.0,status="LIVE_LOSING"),
            "prop-pra" to base.copy(market="PLAYER_POINTS_REBOUNDS_ASSISTS",line=39.5,currentValue=41.0),
            "prop-final" to base.copy(status="WON"))
        for((name,bet) in cases) {
            notifications.clear();notifications.show(bet)
            val notification=manager.activeNotifications.single().notification
            render(notification,"nba-$name-ampliada")
            render(notification,"nba-$name-compacta",compact=true)
            assertEquals(bet.status !in settledStatuses,notification.flags and Notification.FLAG_ONGOING_EVENT!=0)
        }
    }
    @Test @Config(sdk=[36]) fun android16UsesNativeTemplatesForLiveTrackingAndFinalIsDismissible() {
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val notifications=BetNotifications(context)
        notifications.show(nbaBet())
        val progress=manager.activeNotifications.single().notification
        assertEquals("android.app.Notification\$ProgressStyle",progress.extras.getString(Notification.EXTRA_TEMPLATE))
        assertNull(progress.contentView);assertNull(progress.bigContentView)
        notifications.show(nbaBet().copy(id="nba-ml",market="MONEYLINE",selection="HOME",line=null,playerName=null,currentValue=-6.0,status="LIVE_LOSING"))
        val ml=manager.activeNotifications.first { it.tag=="nba-ml" }.notification
        assertEquals("android.app.Notification\$BigTextStyle",ml.extras.getString(Notification.EXTRA_TEMPLATE))
        assertNull(ml.contentView);assertNull(ml.bigContentView)
        notifications.show(nbaBet("WON").copy(updatedAt="2026-10-04T20:10:00Z"))
        val final=manager.activeNotifications.first { it.tag=="nba-prop" }.notification
        assertEquals(0,final.flags and Notification.FLAG_ONGOING_EVENT)
        assertEquals(setOf("nba-ml"),notifications.activeIds())
    }
}
