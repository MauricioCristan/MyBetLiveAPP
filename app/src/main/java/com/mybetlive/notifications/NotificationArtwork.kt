package com.mybetlive.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import com.caverock.androidsvg.SVG
import android.util.LruCache
import kotlinx.coroutines.*
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Public provider artwork only, bounded off-main loading; no sports API calls or credentials. */
internal object NotificationArtwork {
    private val images = LruCache<String, Bitmap>(40)
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private val retryAfter = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun cached(url: String?) = url?.let { images.get(it) }
    fun request(url: String?, ready: () -> Unit) {
        if(url.isNullOrBlank() || cached(url)!=null) return
        val source = runCatching { URL(url) }.getOrNull() ?: return
        if(source.protocol != "https" || source.host !in setOf("media.api-sports.io","cdn.nba.com","ak-static.cms.nba.com","upload.wikimedia.org") ||
            source.userInfo != null || source.port !in setOf(-1,443)) return
        if((retryAfter[url] ?: 0L) > System.currentTimeMillis() || !pending.add(url)) return
        scope.launch {
            try {
                val image = runCatching {
                    val connection = source.openConnection() as javax.net.ssl.HttpsURLConnection
                    connection.connectTimeout=2000;connection.readTimeout=2000;connection.instanceFollowRedirects=false
                    connection.setRequestProperty("User-Agent", "MyBetLive/1.0 (Android)")
                    try { if(connection.responseCode!=200) return@runCatching null
                        val bytes=connection.inputStream.use { input ->
                            val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                            while(out.size()<=512_000){val count=input.read(buffer);if(count<0)break;out.write(buffer,0,count)}
                            out.toByteArray()
                        }
                        if(bytes.size>512_000) return@runCatching null
                        decode(bytes, source.path.endsWith(".svg", ignoreCase = true))
                    } finally { connection.disconnect() }
                }.getOrNull()
                if(image!=null) { images.put(url,image); ready() }
                else retryAfter[url]=System.currentTimeMillis()+600_000
            } finally { pending.remove(url) }
        }
    }
    internal fun decode(bytes: ByteArray, svg: Boolean = false): Bitmap? = runCatching {
        if (bytes.size > 512_000) return@runCatching null
        if (svg) {
            val document = SVG.getFromInputStream(bytes.inputStream())
            Bitmap.createBitmap(160,160,Bitmap.Config.ARGB_8888).also {
                document.renderToCanvas(Canvas(it), RectF(0f,0f,160f,160f))
            }
        } else {
            val bounds=BitmapFactory.Options().also { it.inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            val options=BitmapFactory.Options().also { it.inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/160).coerceAtLeast(1) }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        }
    }.getOrNull()
}
