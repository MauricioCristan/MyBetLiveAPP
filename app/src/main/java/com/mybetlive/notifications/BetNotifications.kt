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
    companion object { private val stateLock = Any() }
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences("notification-state", Context.MODE_PRIVATE)
    init {
        manager.createNotificationChannel(NotificationChannel("tracking-live", "Seguimiento en vivo", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel("tracking-results", "Líneas y resultados", NotificationManager.IMPORTANCE_HIGH))
    }
    override fun show(bet: BetView) {
        show(mapOf("betId" to bet.id, "title" to bet.title(), "body" to "${bet.event.home.name} ${bet.event.homeScore ?: "—"} - ${bet.event.awayScore ?: "—"} ${bet.event.away.name} · ${bet.event.period} ${bet.event.clock.orEmpty()}",
            "status" to bet.status, "currentValue" to (bet.currentValue?.display() ?: ""), "line" to (bet.line?.display() ?: ""),
            "remaining" to (bet.remaining?.display() ?: ""), "updatedAt" to bet.updatedAt,
            "sport" to bet.event.sport, "market" to bet.market, "selection" to bet.selection, "stale" to bet.stale.toString(),
            "homeName" to bet.event.home.name, "awayName" to bet.event.away.name,
            "homeScore" to (bet.event.homeScore?.toString() ?: ""), "awayScore" to (bet.event.awayScore?.toString() ?: ""),
            "homeLogoUrl" to bet.event.home.logoUrl.orEmpty(), "awayLogoUrl" to bet.event.away.logoUrl.orEmpty(),
            "playerName" to bet.playerName.orEmpty(), "playerImageUrl" to bet.playerImageUrl.orEmpty(), "team" to bet.team.orEmpty(),
            "eventStatus" to bet.event.status, "period" to bet.event.period, "clock" to bet.event.clock.orEmpty(),
            "final" to (bet.status in settledStatuses).toString(), "important" to "false"))
    }
    fun show(data: Map<String, String>) { synchronized(stateLock) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33) return
        val id = data["betId"] ?: return
        if (prefs.getBoolean("deleted:$id", false)) return
        val existing=if(data["artworkRefresh"]=="true") manager.activeNotifications.find { it.tag==id && it.id==1 } else null
        // A late portrait/logo must never resurrect an alert the user already dismissed.
        if(data["artworkRefresh"]=="true" && existing==null) return
        val updated = runCatching { java.time.Instant.parse(data["updatedAt"]).toEpochMilli() }.getOrDefault(0L)
        if (updated < prefs.getLong("version:$id", 0)) return
        if (data["final"] == "true" && updated == prefs.getLong("version:$id", -1) &&
            prefs.getString("status:$id", null) in settledStatuses && data["artworkRefresh"] != "true") return
        prefs.edit().putLong("version:$id", updated).apply()
        val final = data["final"] == "true"
        val previousStatus = prefs.getString("status:$id", null)
        val important = (data["important"] == "true" || final && previousStatus != null && previousStatus !in settledStatuses) &&
            prefs.getLong("alerted:$id", -1) != updated
        prefs.edit().putString("status:$id", data["status"]).apply()
        if (important) prefs.edit().putLong("alerted:$id", updated).apply()
        if (!final && prefs.getBoolean("unpinned:$id", false)) return
        if (!final && !liveEnabled()) return
        val status = statusLabels[data["status"]] ?: data["status"].orEmpty()
        val body = data["body"].orEmpty()
        val presentation = marketPresentation(data["sport"] ?: "NFL",data["market"].orEmpty(),data["selection"].orEmpty(),
            data["line"]?.toDoubleOrNull(),data["currentValue"]?.toDoubleOrNull(),data["status"].orEmpty(),
            data["homeScore"]?.toIntOrNull(),data["awayScore"]?.toIntOrNull(),data["stale"]=="true")
        val isMoneyline = data["market"]=="MONEYLINE"
        val progress = marketProgress(data["sport"] ?: "NFL", data["market"].orEmpty(), data["selection"].orEmpty(),
            data["line"]?.toDoubleOrNull(),data["currentValue"]?.toDoubleOrNull(),data["status"].orEmpty(),
            data["homeScore"]?.toIntOrNull(),data["awayScore"]?.toIntOrNull(),data["stale"] == "true")
        val value = if(isMoneyline) presentation.value else progress?.valueLabel ?: "${presentation.value} ${presentation.unit}"
        val title = if(data["homeName"].isNullOrBlank()) data["title"] ?: "MyBetLive" else {
            val event=EventSnapshot(data["sport"]?:"NFL","PUSH","", "", Team("home",data["homeName"].orEmpty()),Team("away",data["awayName"].orEmpty()),
                null,null,data["eventStatus"].orEmpty(),data["period"].orEmpty(),data["clock"],data["updatedAt"].orEmpty())
            selectionTitle(data["market"].orEmpty(),data["selection"].orEmpty(),data["line"]?.toDoubleOrNull(),data["team"],data["playerName"]?.takeIf{it.isNotBlank()},event)
        }
        val time = runCatching { Instant.ofEpochMilli(updated).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("—")
        val details = listOf(body, value, presentation.status, "Último dato $time").filter { it.isNotBlank() }.joinToString("\n")
        val intent = Intent(context, MainActivity::class.java).setData(android.net.Uri.parse("mybetlive://bet/$id"))
            .putExtra("betId", id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val channel=existing?.notification?.channelId ?: if(important) "tracking-results" else "tracking-live"
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_mybet_notification).setContentTitle(title)
            .setContentText(presentation.status).setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(pending).setOnlyAlertOnce(!important).setOngoing(!final).setAutoCancel(final)
            .setSubText(listOf(data["sport"] ?: "NFL",data["period"].orEmpty(),data["clock"].orEmpty(),
                if(final) "Resultado final" else "Provisional").filter { it.isNotBlank() }.joinToString(" · "))
            .setWhen(updated.takeIf { it > 0 } ?: System.currentTimeMillis()).setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_mybet_notification).setContentTitle("MyBetLive")
                .setContentText("Contenido oculto · desbloquea para ver tu seguimiento")
                .setContentIntent(pending).setOngoing(!final).setAutoCancel(final)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build())
            .addAction(0, if(final) "Ver resultado" else "Ver seguimiento", pending)
        if(data["market"]?.startsWith("PLAYER_")==true)
            NotificationArtwork.cached(data["playerImageUrl"])?.let { builder.setLargeIcon(it) }
        if(isMoneyline && data.containsKey("homeScore") && (Build.VERSION.SDK_INT < 36 || final)){
            val small=RemoteViews(context.packageName,R.layout.notification_progress_small)
            small.setTextViewText(R.id.progress_title,title)
            small.setTextViewText(R.id.progress_value,"")
            small.setImageViewBitmap(R.id.progress_track,moneylineArtwork(data,presentation,true))
            small.setContentDescription(R.id.progress_track,"${presentation.value}. ${presentation.status}")
            val large=RemoteViews(context.packageName,R.layout.notification_progress_large)
            large.setTextViewText(R.id.progress_title,title);large.setTextViewText(R.id.progress_value,"")
            large.setImageViewBitmap(R.id.progress_track,moneylineArtwork(data,presentation,false))
            large.setContentDescription(R.id.progress_track,"${presentation.value}. ${presentation.status}")
            large.setTextViewText(R.id.progress_status,presentation.status)
            large.setTextColor(R.id.progress_status,presentation.tone.argb)
            large.setTextViewText(R.id.progress_details,"${data["period"].orEmpty()} ${data["clock"].orEmpty()} · ${presentation.caption}\nÚltimo dato $time")
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(large)
        }
        if(isMoneyline && Build.VERSION.SDK_INT >= 36 && !final) {
            builder.setContentText("${presentation.value} · ${presentation.status}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${presentation.value}\n$body\n${presentation.status}\nÚltimo dato $time"))
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
                    .setContentText("$value · ${progress.statusLabel}")
            } else {
                val small = RemoteViews(context.packageName, R.layout.notification_progress_small)
                small.setTextViewText(R.id.progress_title, title)
                small.setTextViewText(R.id.progress_value, value)
                small.setTextColor(R.id.progress_value, presentation.tone.argb)
                small.setImageViewBitmap(R.id.progress_track, ProgressTrackRenderer.bitmap(context, progress, compact = true))
                small.setContentDescription(R.id.progress_track, progress.description)
                val large = RemoteViews(context.packageName, R.layout.notification_progress_large)
                large.setTextViewText(R.id.progress_title, title)
                large.setTextViewText(R.id.progress_value, value)
                large.setTextColor(R.id.progress_value, presentation.tone.argb)
                large.setImageViewBitmap(R.id.progress_track, ProgressTrackRenderer.bitmap(context, progress))
                large.setContentDescription(R.id.progress_track, progress.description)
                large.setTextViewText(R.id.progress_status, progress.statusLabel)
                large.setTextColor(R.id.progress_status, presentation.tone.argb)
                large.setTextViewText(R.id.progress_details, "$body\nÚltimo dato $time · ${if(final) "Final" else "Provisional"}")
                playerAvatar(large,data)
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(small).setCustomBigContentView(large)
            }
        }
        if(!isMoneyline && progress == null && (Build.VERSION.SDK_INT < 36 || final)) {
            val small=RemoteViews(context.packageName,R.layout.notification_progress_small)
            val large=RemoteViews(context.packageName,R.layout.notification_progress_large)
            for(view in listOf(small,large)) {
                view.setTextViewText(R.id.progress_title,title)
                view.setTextViewText(R.id.progress_value,presentation.value)
                view.setTextColor(R.id.progress_value,presentation.tone.argb)
                view.setViewVisibility(R.id.progress_track,View.GONE)
                view.setViewVisibility(R.id.progress_status,View.VISIBLE)
                view.setTextViewText(R.id.progress_status,presentation.status)
            }
            large.setTextViewText(R.id.progress_details,"$body\nÚltimo dato $time · Sin datos no se interpreta como cero")
            playerAvatar(large,data)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(large)
        }
        if (!final) {
            val unpin = PendingIntent.getBroadcast(context, id.hashCode(), Intent(context, UnpinReceiver::class.java)
                .setData(android.net.Uri.parse("mybetlive://unpin/$id")).putExtra("betId", id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.setShortCriticalText(if(isMoneyline) presentation.value.replace(" : ",":").take(7) else data["currentValue"]?.take(7))
                .setDeleteIntent(unpin).addAction(0, "Desanclar", unpin)
            if (Build.VERSION.SDK_INT >= 36) builder.setRequestPromotedOngoing(true)
        }
        manager.notify(id, 1, builder.build())
        changed()
        val epoch=prefs.getLong("epoch",0)
        val artworkKeys = if(isMoneyline) listOf("homeLogoUrl","awayLogoUrl") else if(data["market"]?.startsWith("PLAYER_")==true) listOf("playerImageUrl") else emptyList()
        for(key in artworkKeys) NotificationArtwork.request(data[key]) {
            synchronized(stateLock) {
                if(prefs.getLong("epoch",0)==epoch && prefs.getLong("version:$id",0)==updated)
                    show(data + mapOf("important" to "false", "artworkRefresh" to "true"))
            }
        }
    } }
    private fun playerAvatar(view: RemoteViews, data: Map<String,String>) {
        if(data["market"]?.startsWith("PLAYER_")!=true) return
        view.setViewVisibility(R.id.progress_avatar,View.VISIBLE)
        view.setContentDescription(R.id.progress_avatar,data["playerName"].orEmpty())
        val photo=NotificationArtwork.cached(data["playerImageUrl"])
        if(photo!=null) view.setImageViewBitmap(R.id.progress_avatar,photo)
        else view.setImageViewResource(R.id.progress_avatar,R.drawable.ic_player_avatar)
    }
    override fun liveEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        manager.getNotificationChannel("tracking-live")?.importance != NotificationManager.IMPORTANCE_NONE
    override fun activeIds(): Set<String> = if(!liveEnabled()) emptySet() else manager.activeNotifications
        .filter { it.id == 1 && it.notification.flags and Notification.FLAG_ONGOING_EVENT != 0 }
        .mapNotNull { it.tag }.toSet()
    override fun observe(listener: () -> Unit): () -> Unit {
        val observer=SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if(key=="revision") listener() }
        prefs.registerOnSharedPreferenceChangeListener(observer)
        return { prefs.unregisterOnSharedPreferenceChangeListener(observer) }
    }
    private fun changed() { prefs.edit().putLong("revision",prefs.getLong("revision",0)+1).apply() }
    override fun pin(bet: BetView): Boolean = synchronized(stateLock) {
        val updated=runCatching { Instant.parse(bet.updatedAt).toEpochMilli() }.getOrDefault(0)
        if (!liveEnabled() || bet.status in settledStatuses || bet.status == "PENDING" || prefs.getBoolean("deleted:${bet.id}",false) ||
            updated < prefs.getLong("version:${bet.id}",0) || prefs.getString("status:${bet.id}",null) in settledStatuses) return@synchronized false
        prefs.edit().remove("unpinned:${bet.id}").apply()
        show(bet)
        // Android enqueues notify asynchronously; acceptance is separate from activeIds() visibility.
        true
    }
    override fun remove(id: String) { synchronized(stateLock) {
        prefs.edit().putBoolean("deleted:$id", true).apply()
        manager.cancel(id, 1)
        changed()
    } }
    override fun isUnpinned(id: String) = prefs.getBoolean("unpinned:$id", false)
    override fun unpin(id: String) { synchronized(stateLock) { prefs.edit().putBoolean("unpinned:$id", true).apply(); manager.cancel(id, 1); changed() } }
    override fun dismissAll() { synchronized(stateLock) { manager.cancelAll(); changed() } }
    override fun clear() { synchronized(stateLock) { manager.cancelAll(); val next=prefs.getLong("epoch",0)+1
        val revision=prefs.getLong("revision",0)+1; prefs.edit().clear().putLong("epoch",next).putLong("revision",revision).apply() } }
}
