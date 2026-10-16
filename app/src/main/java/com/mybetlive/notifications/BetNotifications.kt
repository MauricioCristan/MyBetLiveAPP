package com.mybetlive.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import android.os.Build
import android.widget.RemoteViews
import android.view.View
import com.mybetlive.MainActivity
import com.mybetlive.R
import com.mybetlive.data.*
import com.mybetlive.presentation.ProgressTrackRenderer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class BetNotifications(private val context: Context) : com.mybetlive.application.TrackerNotifications {
    companion object {
        private val stateLock = Any()
        internal const val SUMMARY_TAG="tracking-summary"
        internal const val PREVIOUS="com.mybetlive.notifications.PREVIOUS"
        internal const val NEXT="com.mybetlive.notifications.NEXT"
        internal const val DISMISS="com.mybetlive.notifications.DISMISS"
    }
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences("notification-state", Context.MODE_PRIVATE)
    init {
        manager.activeNotifications.filter { it.id==1 && it.tag!=SUMMARY_TAG && it.notification.channelId in setOf("tracking-live","tracking-results") }.forEach { manager.cancel(it.tag,it.id) }
        // Previous per-tracker unpin choices no longer define a separate category.
        val legacy=prefs.all.keys.filter{it.startsWith("unpinned:")}
        if(legacy.isNotEmpty())prefs.edit().apply{legacy.forEach{remove(it)}}.apply()
        manager.createNotificationChannel(NotificationChannel("tracking-live",com.mybetlive.i18n.AppStrings.translate( "Seguimiento en vivo"), NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel("tracking-results",com.mybetlive.i18n.AppStrings.translate( "Líneas y resultados"), NotificationManager.IMPORTANCE_HIGH))
    }
    override fun show(bet: BetView) {
        val (home,away)=bet.scopedScores()
        show(mapOf("betId" to bet.id, "title" to bet.title(), "body" to "${bet.event.home.name} ${home ?: "—"} - ${away ?: "—"} ${bet.event.away.name} · ${bet.event.period} ${bet.event.clock.orEmpty()}",
            "status" to bet.status, "currentValue" to (bet.currentValue?.display() ?: ""), "line" to (bet.line?.display() ?: ""),
            "remaining" to (bet.remaining?.display() ?: ""), "updatedAt" to bet.updatedAt,
            "sport" to bet.event.sport, "market" to bet.market, "selection" to bet.selection, "stale" to bet.stale.toString(),
            "scope" to (bet.scope ?: "MATCH"),"targetScore" to bet.targetScore.orEmpty(),
            "scopeHomeScore" to (bet.scopedScores().first?.toString()?:""),"scopeAwayScore" to (bet.scopedScores().second?.toString()?:""),
            "homeName" to bet.event.home.name, "awayName" to bet.event.away.name,
            "homeScore" to (home?.toString() ?: ""), "awayScore" to (away?.toString() ?: ""),
            "homeLogoUrl" to bet.event.home.logoUrl.orEmpty(), "awayLogoUrl" to bet.event.away.logoUrl.orEmpty(),
            "playerName" to bet.playerName.orEmpty(), "playerImageUrl" to bet.playerImageUrl.orEmpty(), "team" to bet.team.orEmpty(),
            "eventStatus" to bet.event.status, "period" to bet.event.period, "clock" to bet.event.clock.orEmpty(),
            "final" to (bet.status in settledStatuses).toString(), "important" to "false"))
    }
    fun show(data: Map<String, String>) { synchronized(stateLock) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33) return
        val id = data["betId"] ?: return
        if (prefs.getBoolean("deleted:$id", false)) return
        val existing=if(data["artworkRefresh"]=="true") manager.activeNotifications.find { it.tag==SUMMARY_TAG && it.id==1 } else null
        // A late portrait/logo must never resurrect an alert the user already dismissed.
        if(data["artworkRefresh"]=="true" && existing==null) return
        val updated = runCatching { java.time.Instant.parse(data["updatedAt"]).toEpochMilli() }.getOrDefault(0L)
        if (updated < prefs.getLong("version:$id", 0)) return
        if (isFinal(data) && updated == prefs.getLong("version:$id", -1) &&
            prefs.getString("status:$id", null) in settledStatuses && data["artworkRefresh"] != "true") return
        prefs.edit().putLong("version:$id", updated).apply()
        val final = isFinal(data)
        val previousStatus = prefs.getString("status:$id", null)
        val important = (data["important"] == "true" || final && previousStatus != null && previousStatus !in settledStatuses) &&
            prefs.getLong("alerted:$id", -1) != updated
        prefs.edit().putString("status:$id", data["status"]).apply()
        if (important) prefs.edit().putLong("alerted:$id", updated).apply()
        if (!final && !liveEnabled()) return
        val rows=snapshots().toMutableMap()
        rows[id]=data.filterKeys{it!="artworkRefresh"&&it!="important"}
        saveSnapshots(rows)
        if(important && prefs.getString("selected",null)!=id){
            prefs.edit().putStringSet("unread",prefs.getStringSet("unread",emptySet()).orEmpty()+id).apply()
        }
        renderCurrent(important,data["artworkRefresh"]=="true")
    } }
    private fun snapshots():LinkedHashMap<String,Map<String,String>> = runCatching {
        val array=org.json.JSONArray(prefs.getString("snapshots","[]"))
        linkedMapOf<String,Map<String,String>>().apply {
            for(i in 0 until array.length()){
                val row=array.getJSONObject(i)
                val values=row.keys().asSequence().associateWith{row.getString(it)}
                values["betId"]?.let{put(it,values)}
            }
        }
    }.getOrDefault(linkedMapOf())
    private fun saveSnapshots(rows:Map<String,Map<String,String>>){
        prefs.edit().putString("snapshots",org.json.JSONArray(rows.values.map{org.json.JSONObject(it)}).toString()).apply()
    }
    private fun isFinal(data:Map<String,String>)=data["final"]=="true"||data["status"] in settledStatuses
    private fun navigation(action:String):PendingIntent {
        val epoch=prefs.getLong("epoch",0)
        val intent=Intent(context,TrackingNotificationReceiver::class.java).setAction(action)
            .setData(android.net.Uri.parse("mybetlive://notification/$action/$epoch")).putExtra("epoch",epoch)
        return PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    internal fun navigate(direction:Int,epoch:Long){synchronized(stateLock){
        if(epoch!=prefs.getLong("epoch",0)||manager.activeNotifications.none{it.tag==SUMMARY_TAG&&it.id==1})return
        val rows=snapshots().values.filter{isFinal(it)||liveEnabled()}
        if(rows.isEmpty())return
        val index=rows.indexOfFirst{it["betId"]==prefs.getString("selected",null)}.coerceAtLeast(0)
        val next=rows[Math.floorMod(index+direction,rows.size)]["betId"]!!
        prefs.edit().putString("selected",next).putStringSet("unread",prefs.getStringSet("unread",emptySet()).orEmpty()-next).apply()
        renderCurrent(false,true)
    }}
    internal fun dismissed(epoch:Long){synchronized(stateLock){
        if(epoch!=prefs.getLong("epoch",0))return
        prefs.edit().remove("snapshots").remove("selected").remove("unread").apply();changed()
    }}
    private fun renderCurrent(important:Boolean=false,preserveChannel:Boolean=true){
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled() || Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val rows=snapshots().values.filter{isFinal(it)||liveEnabled()}
        if(rows.isEmpty()){manager.cancel(SUMMARY_TAG,1);changed();return}
        val data=rows.find{it["betId"]==prefs.getString("selected",null)}?:rows.first()
        val id=data["betId"]!!
        prefs.edit().putString("selected",id).apply()
        val final=isFinal(data)
        val ongoing=rows.any{!isFinal(it)}
        val updated=runCatching{Instant.parse(data["updatedAt"]).toEpochMilli()}.getOrDefault(0L)
        val existing=if(preserveChannel)manager.activeNotifications.find{it.tag==SUMMARY_TAG&&it.id==1}else null
        val counter=com.mybetlive.i18n.AppStrings.translate("Seguimiento")+" ${rows.indexOf(data)+1} / ${rows.size}"
        val unread=prefs.getStringSet("unread",emptySet()).orEmpty().count{key->rows.any{it["betId"]==key}}
        val overview=counter+if(unread>0)" · "+com.mybetlive.i18n.AppStrings.translate("Novedades")+": $unread" else ""
        val body = data["body"].orEmpty()
        val presentation = marketPresentation(data["sport"] ?: "NFL",data["market"].orEmpty(),data["selection"].orEmpty(),
            data["line"]?.toDoubleOrNull(),data["currentValue"]?.toDoubleOrNull(),data["status"].orEmpty(),
            (if(data["sport"]=="NHL")data["scopeHomeScore"] else data["homeScore"])?.toIntOrNull(),(if(data["sport"]=="NHL")data["scopeAwayScore"] else data["awayScore"])?.toIntOrNull(),data["stale"]=="true",data["scope"]?:"MATCH",data["targetScore"])
        val isMoneyline = presentation.visual==MarketVisual.MONEYLINE
        val progress = marketProgress(data["sport"] ?: "NFL", data["market"].orEmpty(), data["selection"].orEmpty(),
            data["line"]?.toDoubleOrNull(),data["currentValue"]?.toDoubleOrNull(),data["status"].orEmpty(),
            data["homeScore"]?.toIntOrNull(),data["awayScore"]?.toIntOrNull(),data["stale"] == "true")
        val value = if(isMoneyline) presentation.value else progress?.valueLabel ?: "${presentation.value} ${presentation.unit}"
        val title = if(data["homeName"].isNullOrBlank()) data["title"] ?: "MyBetLive" else {
            val event=EventSnapshot(data["sport"]?:"NFL","PUSH","", "", Team("home",data["homeName"].orEmpty()),Team("away",data["awayName"].orEmpty()),
                null,null,data["eventStatus"].orEmpty(),data["period"].orEmpty(),data["clock"],data["updatedAt"].orEmpty())
            selectionTitle(data["market"].orEmpty(),data["selection"].orEmpty(),data["line"]?.toDoubleOrNull(),data["team"],data["playerName"]?.takeIf{it.isNotBlank()},event,data["scope"]?:"MATCH",data["targetScore"]?.takeIf{it.isNotBlank()})
        }
        val time = runCatching { Instant.ofEpochMilli(updated).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("—")
        val details = listOf(body, value, presentation.status, "Último dato $time").filter { it.isNotBlank() }.joinToString("\n")
        val intent = Intent(context, MainActivity::class.java).setData(android.net.Uri.parse("mybetlive://bet/$id"))
            .putExtra("betId", id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val channel=existing?.notification?.channelId ?: if(important) "tracking-results" else "tracking-live"
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_mybet_notification).setContentTitle(com.mybetlive.i18n.AppStrings.translate(title))
            .setContentText(com.mybetlive.i18n.AppStrings.translate(presentation.status)).setStyle(NotificationCompat.BigTextStyle().bigText(com.mybetlive.i18n.AppStrings.translate(details)))
            .setContentIntent(pending).setOnlyAlertOnce(!important).setOngoing(ongoing).setAutoCancel(!ongoing)
            .setSubText(overview)
            .setDeleteIntent(navigation(DISMISS))
            .setWhen(updated.takeIf { it > 0 } ?: System.currentTimeMillis()).setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_mybet_notification).setContentTitle(com.mybetlive.i18n.AppStrings.translate("MyBetLive"))
                .setContentText(com.mybetlive.i18n.AppStrings.translate("Contenido oculto · desbloquea para ver tu seguimiento"))
                .setContentIntent(pending).setOngoing(ongoing).setAutoCancel(!ongoing)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build())
        if(rows.size>1){
            builder.addAction(0,com.mybetlive.i18n.AppStrings.translate("Anterior"),navigation(PREVIOUS))
                .addAction(0,com.mybetlive.i18n.AppStrings.translate("Siguiente"),navigation(NEXT))
        }
        builder.addAction(0,com.mybetlive.i18n.AppStrings.translate(if(final) "Ver resultado" else "Ver seguimiento"),pending)
        if(data["market"]?.startsWith("PLAYER_")==true)
            NotificationArtwork.cached(data["playerImageUrl"])?.let { builder.setLargeIcon(it) }
        if(isMoneyline && data.containsKey("homeScore") && (Build.VERSION.SDK_INT < 36 || final)){
            val small=RemoteViews(context.packageName,R.layout.notification_progress_small)
            small.setTextViewText(R.id.progress_title,com.mybetlive.i18n.AppStrings.translate(title))
            small.setTextViewText(R.id.progress_value,com.mybetlive.i18n.AppStrings.translate(""))
            small.setImageViewBitmap(R.id.progress_track,moneylineArtwork(data,presentation,true))
            small.setContentDescription(R.id.progress_track,com.mybetlive.i18n.AppStrings.translate("${presentation.value}. ${presentation.status}"))
            val large=RemoteViews(context.packageName,R.layout.notification_progress_large)
            large.setTextViewText(R.id.progress_title,com.mybetlive.i18n.AppStrings.translate(title));large.setTextViewText(R.id.progress_value,com.mybetlive.i18n.AppStrings.translate(""))
            large.setImageViewBitmap(R.id.progress_track,moneylineArtwork(data,presentation,false))
            large.setContentDescription(R.id.progress_track,com.mybetlive.i18n.AppStrings.translate("${presentation.value}. ${presentation.status}"))
            large.setTextViewText(R.id.progress_status,com.mybetlive.i18n.AppStrings.translate(presentation.status))
            large.setTextColor(R.id.progress_status,presentation.tone.argb)
            large.setTextViewText(R.id.progress_details,com.mybetlive.i18n.AppStrings.translate("${data["period"].orEmpty()} ${data["clock"].orEmpty()} · ${presentation.caption}\nÚltimo dato $time"))
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(large)
        }
        if(isMoneyline && Build.VERSION.SDK_INT >= 36 && !final) {
            builder.setContentText(com.mybetlive.i18n.AppStrings.translate("${presentation.value} · ${presentation.status}"))
                .setStyle(NotificationCompat.BigTextStyle().bigText(com.mybetlive.i18n.AppStrings.translate("${presentation.value}\n$body\n${presentation.status}\nÚltimo dato $time")))
            val teamKey=if(data["selection"]=="AWAY") "awayLogoUrl" else "homeLogoUrl"
            NotificationArtwork.cached(data[teamKey])?.let { builder.setLargeIcon(it) }
        }
        if (progress != null) {
            builder.setColor(progress.tone.argb)
            if (Build.VERSION.SDK_INT >= 36 && !final) {
                val style=NotificationCompat.ProgressStyle()
                if(progress.centered) {
                    val muted=progress.tone==ProgressTone.MUTED
                    style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(500).setColor(if(muted) ProgressTone.MUTED.argb else ProgressTone.RED.argb))
                        .addProgressSegment(NotificationCompat.ProgressStyle.Segment(500).setColor(if(muted) ProgressTone.MUTED.argb else ProgressTone.GREEN.argb))
                } else style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(1000).setColor(progress.tone.argb))
                builder.setStyle(style.addProgressPoint(NotificationCompat.ProgressStyle.Point((progress.targetFraction * 1000).roundToInt()).setColor(ProgressTone.MUTED.argb))
                    .setProgress((progress.fraction * 1000).roundToInt())
                    .setProgressTrackerIcon(IconCompat.createWithResource(context, ProgressTrackRenderer.sportIcon(progress.sport))))
                    .setContentText(com.mybetlive.i18n.AppStrings.translate("$value · ${progress.statusLabel}"))
            } else {
                val small = RemoteViews(context.packageName, R.layout.notification_progress_small)
                small.setTextViewText(R.id.progress_title,com.mybetlive.i18n.AppStrings.translate( title))
                small.setTextViewText(R.id.progress_value,com.mybetlive.i18n.AppStrings.translate( value))
                small.setTextColor(R.id.progress_value, presentation.tone.argb)
                small.setImageViewBitmap(R.id.progress_track, ProgressTrackRenderer.bitmap(context, progress, compact = true))
                small.setContentDescription(R.id.progress_track,com.mybetlive.i18n.AppStrings.translate( progress.description))
                val large = RemoteViews(context.packageName, R.layout.notification_progress_large)
                large.setTextViewText(R.id.progress_title,com.mybetlive.i18n.AppStrings.translate( title))
                large.setTextViewText(R.id.progress_value,com.mybetlive.i18n.AppStrings.translate( value))
                large.setTextColor(R.id.progress_value, presentation.tone.argb)
                large.setImageViewBitmap(R.id.progress_track, ProgressTrackRenderer.bitmap(context, progress))
                large.setContentDescription(R.id.progress_track,com.mybetlive.i18n.AppStrings.translate( progress.description))
                large.setTextViewText(R.id.progress_status,com.mybetlive.i18n.AppStrings.translate( progress.statusLabel))
                large.setTextColor(R.id.progress_status, presentation.tone.argb)
                large.setTextViewText(R.id.progress_details,com.mybetlive.i18n.AppStrings.translate( "$body\nÚltimo dato $time · ${if(final) "Final" else "Provisional"}"))
                playerAvatar(large,data)
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(small).setCustomBigContentView(large)
            }
        }
        if(!isMoneyline && progress == null && (Build.VERSION.SDK_INT < 36 || final)) {
            val small=RemoteViews(context.packageName,R.layout.notification_progress_small)
            val large=RemoteViews(context.packageName,R.layout.notification_progress_large)
            for(view in listOf(small,large)) {
                view.setTextViewText(R.id.progress_title,com.mybetlive.i18n.AppStrings.translate(title))
                view.setTextViewText(R.id.progress_value,com.mybetlive.i18n.AppStrings.translate(presentation.value))
                view.setTextColor(R.id.progress_value,presentation.tone.argb)
                view.setViewVisibility(R.id.progress_track,View.GONE)
                view.setViewVisibility(R.id.progress_status,View.VISIBLE)
                view.setTextViewText(R.id.progress_status,com.mybetlive.i18n.AppStrings.translate(presentation.status))
            }
            large.setTextViewText(R.id.progress_details,com.mybetlive.i18n.AppStrings.translate("$body\nÚltimo dato $time · Sin datos no se interpreta como cero"))
            playerAvatar(large,data)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(large)
        }
        if (!final && rows.size==1) {
            builder.setShortCriticalText(if(isMoneyline) presentation.value.replace(" : ",":").take(7) else data["currentValue"]?.take(7))
            if (Build.VERSION.SDK_INT >= 36) builder.setRequestPromotedOngoing(true)
        }
        manager.notify(SUMMARY_TAG, 1, builder.build())
        changed()
        val epoch=prefs.getLong("epoch",0)
        val artworkKeys = if(isMoneyline) listOf("homeLogoUrl","awayLogoUrl") else if(data["market"]?.startsWith("PLAYER_")==true) listOf("playerImageUrl") else emptyList()
        for(key in artworkKeys) NotificationArtwork.request(data[key]) {
            synchronized(stateLock) {
                if(prefs.getLong("epoch",0)==epoch && prefs.getLong("version:$id",0)==updated && prefs.getString("selected",null)==id && manager.activeNotifications.any{it.tag==SUMMARY_TAG&&it.id==1})
                    renderCurrent(false,true)
            }
        }
    }
    private fun playerAvatar(view: RemoteViews, data: Map<String,String>) {
        if(data["market"]?.startsWith("PLAYER_")!=true) return
        view.setViewVisibility(R.id.progress_avatar,View.VISIBLE)
        view.setContentDescription(R.id.progress_avatar,com.mybetlive.i18n.AppStrings.translate(data["playerName"].orEmpty()))
        val photo=NotificationArtwork.cached(data["playerImageUrl"])
        if(photo!=null) view.setImageViewBitmap(R.id.progress_avatar,photo)
        else view.setImageViewResource(R.id.progress_avatar,R.drawable.ic_player_avatar)
    }
    override fun liveEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        manager.getNotificationChannel("tracking-live")?.importance != NotificationManager.IMPORTANCE_NONE
    override fun activeIds():Set<String> = synchronized(stateLock){
        if(!liveEnabled()||manager.activeNotifications.none{it.tag==SUMMARY_TAG&&it.id==1})emptySet()
        else snapshots().filterValues{!isFinal(it)}.keys.toSet()
    }
    override fun observe(listener: () -> Unit): () -> Unit {
        val observer=SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if(key=="revision") listener() }
        prefs.registerOnSharedPreferenceChangeListener(observer)
        return { prefs.unregisterOnSharedPreferenceChangeListener(observer) }
    }
    private fun changed() { prefs.edit().putLong("revision",prefs.getLong("revision",0)+1).apply() }
    override fun remove(id: String) { synchronized(stateLock) {
        prefs.edit().putBoolean("deleted:$id", true).apply()
        val rows=snapshots().toMutableMap();rows.remove(id);saveSnapshots(rows)
        prefs.edit().putStringSet("unread",prefs.getStringSet("unread",emptySet()).orEmpty()-id).apply()
        if(manager.activeNotifications.any{it.tag==SUMMARY_TAG&&it.id==1})renderCurrent()else changed()
    } }
    override fun dismissAll() { synchronized(stateLock) { manager.cancelAll(); prefs.edit().remove("snapshots").remove("selected").remove("unread").apply();changed() } }
    override fun clear() { synchronized(stateLock) { manager.cancelAll(); val next=prefs.getLong("epoch",0)+1
        val revision=prefs.getLong("revision",0)+1; prefs.edit().clear().putLong("epoch",next).putLong("revision",revision).apply() } }
}
