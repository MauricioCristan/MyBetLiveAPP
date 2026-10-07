package com.mybetlive.ads

import android.app.Activity
import androidx.compose.runtime.*
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.mybetlive.data.RewardIntent
import com.google.android.ump.*
import com.mybetlive.BuildConfig
import com.mybetlive.data.MySubscription
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

val LocalAdsController = staticCompositionLocalOf<AdsController?> { null }

class AdsController(private val activity: Activity, private val scope: CoroutineScope) {
    private companion object {
        val sdkStarted = AtomicBoolean(false)
        val sdkReady = MutableStateFlow(false)
    }
    var ready by mutableStateOf(false); private set
    var privacyRequired by mutableStateOf(false); private set
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private var generation = 0
    private var currentKey: String? = null
    private var rewardCompletion:CompletableDeferred<Boolean>?=null
    fun update(subscription: MySubscription?, offline: Boolean) {
        val unit = AdsPolicy.unitId(subscription, offline, BuildConfig.DEBUG, BuildConfig.ADMOB_APP_ID)
            ?: AdsPolicy.rewardedUnitId(subscription,offline,BuildConfig.DEBUG,BuildConfig.ADMOB_APP_ID)
        val key = unit?.let { "$it:${subscription?.playAccountId}" }
        if (key == currentKey) return
        currentKey = key; val attempt = ++generation; ready = false
        if (key == null) {rewardCompletion?.complete(false);return}
        if (BuildConfig.DEBUG) { initialize(attempt); return }
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (generation == attempt && !activity.isDestroyed) {
                privacyRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consent.canRequestAds()) initialize(attempt)
                }
            }
        }, { if (consent.canRequestAds()) initialize(attempt) })
    }
    private fun initialize(attempt: Int) {
        if (generation != attempt || activity.isDestroyed) return
        scope.launch(Dispatchers.IO) {
            if (sdkStarted.compareAndSet(false,true)) MobileAds.initialize(activity.applicationContext) { sdkReady.value = true }
            sdkReady.first { it }
            withContext(Dispatchers.Main) { if (generation == attempt && !activity.isDestroyed) ready = true }
        }
    }
    fun privacyOptions() {
        ready = false
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            if (currentKey != null && consent.canRequestAds()) initialize(generation)
        }
    }
    suspend fun watchReward(intent:RewardIntent):Boolean {
        check(ready&&!activity.isDestroyed&&currentKey!=null){"Los anuncios todavía no están disponibles. Vuelve a intentar."}
        check(if(BuildConfig.DEBUG) intent.test&&intent.unitId==AdsPolicy.TEST_REWARDED_ID else !intent.test&&intent.unitId.substringBefore('/')==BuildConfig.ADMOB_APP_ID.substringBefore('~')){"Bloque de recompensa inválido"}
        check(rewardCompletion?.isActive!=true){"Ya hay un anuncio en curso"}
        val completion=CompletableDeferred<Boolean>();rewardCompletion=completion;val attempt=generation
        val loaded=CompletableDeferred<Unit>()
        var earned=false
        RewardedAd.load(activity,intent.unitId,AdRequest.Builder().build(),object:RewardedAdLoadCallback(){
            override fun onAdFailedToLoad(error:LoadAdError){loaded.completeExceptionally(IllegalStateException("No hay un anuncio disponible. Tu seguimiento normal se conserva; intenta después."))}
            override fun onAdLoaded(ad:RewardedAd){
                if(!completion.isActive||generation!=attempt||activity.isDestroyed){completion.complete(false);return}
                loaded.complete(Unit)
                ad.setServerSideVerificationOptions(ServerSideVerificationOptions.Builder().setCustomData(intent.id).setUserId(intent.accountId).build())
                ad.fullScreenContentCallback=object:FullScreenContentCallback(){
                    override fun onAdDismissedFullScreenContent(){completion.complete(earned)}
                    override fun onAdFailedToShowFullScreenContent(error:AdError){completion.completeExceptionally(IllegalStateException("No se pudo mostrar el anuncio. Puedes reintentar."))}
                }
                ad.show(activity){earned=true}
            }
        })
        try{
            withTimeoutOrNull(180000){loaded.await()} ?: throw IllegalStateException("El anuncio tardó demasiado. Puedes volver a intentarlo.")
            return completion.await()
        }
        finally{loaded.cancel();completion.cancel();if(rewardCompletion===completion)rewardCompletion=null}
    }
    fun close() { ++generation; currentKey = null; ready = false;rewardCompletion?.complete(false) }
}
