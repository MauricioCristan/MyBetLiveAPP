package com.mybetlive.ads

import android.graphics.Color
import android.view.View
import android.widget.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxWidth
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*
import com.mybetlive.BuildConfig
import com.mybetlive.data.MySubscription

/** SDK assets own their clicks; the outer card never navigates or encourages clicks. */
@Composable fun NativeAdCard(subscription: MySubscription?, offline: Boolean) {
    val controller = LocalAdsController.current
    val unit = AdsPolicy.unitId(subscription, offline, BuildConfig.DEBUG, BuildConfig.ADMOB_APP_ID)
    val context = LocalContext.current
    var ad by remember(unit, controller?.ready, subscription?.playAccountId) { mutableStateOf<NativeAd?>(null) }
    if (unit == null || controller?.ready != true) return
    DisposableEffect(unit, controller, subscription?.playAccountId) {
        var disposed = false
        var loaded: NativeAd? = null
        AdLoader.Builder(context, unit).forNativeAd { received ->
            if (disposed) received.destroy() else { loaded?.destroy(); loaded = received; ad = received }
        }.withAdListener(object : AdListener() { override fun onAdFailedToLoad(error: LoadAdError) { ad = null } })
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build().loadAd(AdRequest.Builder().build())
        onDispose { disposed = true; loaded?.destroy(); ad = null }
    }
    ad?.let { native ->
        key(native) {
            AndroidView(modifier = Modifier.fillMaxWidth(), factory = { ctx ->
                fun dp(value: Int) = (value * ctx.resources.displayMetrics.density).toInt()
                val root = NativeAdView(ctx)
                val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)); setBackgroundColor(Color.rgb(23,35,46)) }
                root.addView(column, android.view.ViewGroup.LayoutParams(-1,-2))
                fun label(value: String?, size: Float) = TextView(ctx).apply { text = value; textSize = size; setTextColor(Color.WHITE); setPadding(0,dp(4),0,dp(4)) }
                column.addView(label(com.mybetlive.i18n.AppStrings.translate(if (BuildConfig.DEBUG) "Publicidad · anuncio de prueba" else "Publicidad",ctx.resources), 12f))
                val headline = label(native.headline, 17f); column.addView(headline); root.headlineView = headline
                val media = MediaView(ctx); column.addView(media, LinearLayout.LayoutParams(-1,dp(160))); root.mediaView = media
                media.mediaContent = native.mediaContent
                media.visibility = if (native.mediaContent == null) View.GONE else View.VISIBLE
                native.icon?.let { icon ->
                    val image = ImageView(ctx).apply { setImageDrawable(icon.drawable) }
                    column.addView(image,LinearLayout.LayoutParams(dp(40),dp(40))); root.iconView = image
                }
                native.body?.let { body -> val text = label(body,14f); column.addView(text); root.bodyView = text }
                native.advertiser?.let { advertiser -> val text = label(advertiser,12f); column.addView(text); root.advertiserView = text }
                native.callToAction?.let { action ->
                    val button = Button(ctx).apply { text = action }
                    column.addView(button); root.callToActionView = button
                }
                root.setNativeAd(native); root
            })
        }
    }
}
