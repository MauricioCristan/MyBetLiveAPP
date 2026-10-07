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
    private var notificationBetId by mutableStateOf<String?>(null)
    private var notificationRequest by mutableIntStateOf(0)
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); notificationBetId = intent.getStringExtra("betId"); notificationRequest++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyBetLive)
        super.onCreate(savedInstanceState)
        notificationBetId = intent.getStringExtra("betId")
        setContent {
            val factory=remember { TrackingViewModelFactory(application) }
            val model: TrackingViewModel = viewModel(factory=factory)
            LaunchedEffect(notificationRequest) { notificationBetId?.let(model::openNotification) }
            val state by model.ui.collectAsStateWithLifecycle()
            val adScope = rememberCoroutineScope()
            val ads = remember { AdsController(this, adScope) }
            LaunchedEffect(state.subscription, state.offline, state.loggedIn) { ads.update(state.subscription.takeIf { state.loggedIn }, state.offline) }
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
            LaunchedEffect(state.loggedIn) {
                if (state.loggedIn && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            MyBetLiveApp(state, model, prices,
                buy = { plan -> state.subscription?.takeIf { it.billingEnabled }?.let { billing?.buy(this, plan, it.playAccountId) } },
                restore = { billing?.restore() },
                manage = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))) })
            }
        }
    }
}
