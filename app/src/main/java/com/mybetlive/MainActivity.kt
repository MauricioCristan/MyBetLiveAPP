package com.mybetlive

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import com.mybetlive.ads.AdsController
import com.mybetlive.ads.LocalAdsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import android.animation.ValueAnimator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mybetlive.ui.*
import com.mybetlive.billing.PlayBillingManager
import com.mybetlive.data.StorePrice
import com.mybetlive.composition.TrackingViewModelFactory

class MainActivity : ComponentActivity() {
    private var startupVisible by mutableStateOf(false)
    private var contentReady by mutableStateOf(false)
    private var nativeSplashReady by mutableStateOf(Build.VERSION.SDK_INT < 31)
    private var emailLink by mutableStateOf<com.mybetlive.application.EmailAccountLink?>(null)
    private var emailRequest by mutableIntStateOf(0)
    private var notificationBetId by mutableStateOf<String?>(null)
    private var notificationRequest by mutableIntStateOf(0)
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); startupVisible=false;notificationBetId = intent.getStringExtra("betId"); notificationRequest++;emailLink=com.mybetlive.application.EmailAccountLink.parse(intent.dataString);emailRequest++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ needs the starting theme intact to resolve its animated drawable.
        if(Build.VERSION.SDK_INT < 31) setTheme(R.style.Theme_MyBetLive)
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= 31) splashScreen.setOnExitAnimationListener { splash ->
            val started=splash.iconAnimationStart?.toEpochMilli()
            val duration=splash.iconAnimationDuration?.toMillis() ?: 0L
            if(BuildConfig.DEBUG)android.util.Log.d("MyBetStartup","Native icon duration: $duration ms")
            val remaining=if(startupVisible && started!=null)
                (started+duration-System.currentTimeMillis()).coerceIn(0L,500L) else 0L
            splash.postDelayed({ nativeSplashReady=true;splash.remove() },remaining)
        }
        notificationBetId = intent.getStringExtra("betId")
        emailLink=com.mybetlive.application.EmailAccountLink.parse(intent.dataString)
        startupVisible=com.mybetlive.application.shouldShowStartupIntro(
            restored=savedInstanceState!=null,
            launcherEntry=intent.action==Intent.ACTION_MAIN&&intent.hasCategory(Intent.CATEGORY_LAUNCHER),
            hasDestination=emailLink!=null||notificationBetId!=null,
            animationsEnabled=ValueAnimator.areAnimatorsEnabled())
        contentReady=!startupVisible
        setContent {
            val factory=remember { TrackingViewModelFactory(application) }
            LaunchedEffect(Unit) {
                if(!contentReady) {
                    // Draw the intro before opening storage, notifications, or the session.
                    withFrameNanos { }
                    withFrameNanos { }
                    try { factory.prepare() } finally { contentReady=true }
                }
            }
            Box(Modifier.fillMaxSize().background(Ink)) {
                if(contentReady || !startupVisible) MainContent(factory)
                if(startupVisible)StartupIntro(
                    initialProgressMs=if(Build.VERSION.SDK_INT>=31)500 else 0,
                    animationReady=nativeSplashReady,
                    contentReady=contentReady) { startupVisible=false }
            }
        }
    }

    @Composable private fun MainContent(factory: TrackingViewModelFactory) {
            val model: TrackingViewModel = viewModel(factory=factory)
            LaunchedEffect(notificationRequest) { notificationBetId?.let(model::openNotification) }
            LaunchedEffect(emailRequest) { emailLink?.let(model::openEmailLink) }
            val state by model.ui.collectAsStateWithLifecycle()
            val adScope = rememberCoroutineScope()
            val ads = remember { AdsController(this, adScope) }
            LaunchedEffect(state.subscription, state.offline, state.loggedIn, startupVisible) { if(!startupVisible)ads.update(state.subscription.takeIf { state.loggedIn }, state.offline) }
            DisposableEffect(ads) { onDispose { ads.close() } }
            var prices by remember(state.subscription?.playAccountId) { mutableStateOf<Map<String, StorePrice>>(emptyMap()) }
            val billing = remember(state.subscription?.playAccountId, state.subscription?.billingEnabled) {
                if (state.subscription?.billingEnabled == true) PlayBillingManager(this,
                    { prices = it }, model::verifyPurchases, model::billingMessage) else null
            }
            DisposableEffect(billing) { onDispose { billing?.close() } }
            LaunchedEffect(billing, state.subscription?.availablePlans) {
                billing?.load(state.subscription?.availablePlans.orEmpty())
            }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.refreshNotificationState() }
            LaunchedEffect(state.loggedIn, startupVisible) {
                if (!startupVisible && state.loggedIn && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            DisposableEffect(lifecycle, billing) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) { model.setForeground(true); billing?.restore() }
                    if (event == Lifecycle.Event.ON_RESUME) model.refreshNotificationState()
                    if (event == Lifecycle.Event.ON_STOP) model.setForeground(false)
                }
                lifecycle.addObserver(observer)
                model.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                onDispose { lifecycle.removeObserver(observer); model.setForeground(false) }
            }
            CompositionLocalProvider(LocalAdsController provides ads) {
            Box(Modifier.fillMaxSize()) {
            MyBetLiveApp(state, model, prices,
                buy = { plan -> state.subscription?.takeIf { it.billingEnabled }?.let { billing?.buy(this@MainActivity, plan, it.playAccountId) } },
                restore = { billing?.restore() },
                manage = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))) })
            }
            }
    }
}
