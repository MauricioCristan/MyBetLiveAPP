package com.mybetlive.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mybetlive.data.*
import com.mybetlive.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneOffset

data class AppState(
    val loggedIn: Boolean = false, val username: String = "", val busy: Boolean = false,
    val error: String? = null, val bets: List<BetView> = emptyList(), val events: List<EventView> = emptyList(),
    val sports: List<SportInfo> = emptyList(), val players: List<Player> = emptyList(), val offline: Boolean = false,
    val playersEventId: String? = null,
    val date: LocalDate = LocalDate.now(ZoneOffset.UTC), val selectedSport: String = "NFL",
    val subscription: MySubscription? = null, val billingMessage: String? = null,val rewardMessage:String?=null,
    val notificationBetId: String? = null, val unpinnedBets: Set<String> = emptySet(), val notificationRequest: Long = 0,
    val pinnedBets: Set<String> = emptySet(), val deviceNotificationsEnabled: Boolean = true
)

class TrackingViewModel(app: Application,
    private val account: SessionOperations, private val tracking: TrackingOperations,
    private val subscriptions: SubscriptionOperations, private val rewards: RewardOperations,
    private val devices: DeviceOperations, private val notifications: TrackerNotifications,
    private val failures: FailureMessages
) : AndroidViewModel(app) {
    private val state = MutableStateFlow(AppState(loggedIn = account.session != null,
        username = account.session?.username.orEmpty(), bets = tracking.cachedBets,
        offline = account.session != null))
    val ui = state.asStateFlow()
    private var pollJob: Job? = null
    private val workLock = Mutex()
    private var notificationSyncJob: Job? = null
    private val stopNotificationObserver = notifications.observe(::notificationChanged)
    init { syncNotificationState(); if (state.value.loggedIn) refresh() }

    fun syncNotificationState() {
        val active=notifications.activeIds()
        val enabled=notifications.liveEnabled()
        state.update { current -> current.copy(deviceNotificationsEnabled=enabled,
            pinnedBets=current.bets.filter { it.id in active && it.status !in settledStatuses }.map { it.id }.toSet(),
            unpinnedBets=current.bets.filter { notifications.isUnpinned(it.id) }.map { it.id }.toSet()) }
    }
    private fun notificationChanged() {
        syncNotificationState()
        notificationSyncJob?.cancel()
        notificationSyncJob=viewModelScope.launch { delay(250);syncNotificationState() }
    }
    fun refreshNotificationState() {
        if(state.value.loggedIn && state.value.subscription?.access?.pushNotifications == true)
            publishNotifications()
        syncNotificationState()
    }
    private fun publishNotifications() {
        state.value.bets.filter { it.status != "PENDING" }.forEach { notifications.show(it.copy(stale=it.stale || state.value.offline)) }
        syncNotificationState()
    }
    override fun onCleared() { stopNotificationObserver(); super.onCleared() }

    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        viewModelScope.launch {
          workLock.withLock {
            state.update { it.copy(busy = true, error = null) }
            try { block() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                state.update { it.copy(error = failures.message(error), offline = it.offline || error is java.io.IOException) }
            } finally { state.update { it.copy(busy = false) } }
          }
        }
    }
    fun login(url: String, username: String, password: String) = action {
        account.login(url, username, password)
        state.update { it.copy(loggedIn = true, username = username, bets = emptyList(), offline = false) }
        load()
    }
    fun loginGoogle(url: String, credential: suspend (GoogleChallenge) -> String) = action {
        val challenge = account.googleChallenge(url)
        val token = credential(challenge)
        account.googleLogin(url, token, challenge.nonce)
        state.update { it.copy(loggedIn = true, username = account.session?.username.orEmpty(), bets = emptyList(), offline = false) }
        load()
    }
    private suspend fun load() {
        val subscription = subscriptions.subscription()
        state.update { it.copy(subscription = subscription) }
        if (!subscription.access.pushNotifications) notifications.dismissAll()
        val bets = tracking.bets()
        state.update { it.copy(bets = bets, offline = false, unpinnedBets = bets.filter { b -> notifications.isUnpinned(b.id) }.map { b -> b.id }.toSet()) }
        if (subscription.access.pushNotifications) publishNotifications() else syncNotificationState()
        if (state.value.sports.isEmpty()) {
            val sports = tracking.sports()
            state.update { it.copy(sports = sports) }
        }
        // Device registration must not depend on the sporting provider's quota/catalog.
        runCatching { devices.registerDevice() }
        val events = tracking.events(state.value.selectedSport, state.value.date)
        state.update { it.copy(events = events) }
    }
    fun refresh() = action { load() }
    fun watchReward(show:suspend(com.mybetlive.data.RewardIntent)->Boolean)=action {
        state.update{it.copy(rewardMessage=null)}
        try {
        val account=state.value.subscription?.playAccountId ?: return@action
        val result=ClaimTrackingReward(rewards,subscriptions).execute(show,
            current={state.value.subscription?.playAccountId==account},progress={stage ->
                state.update{it.copy(rewardMessage=if(stage==RewardStage.LOADING) "Cargando anuncio…" else "Confirmando recompensa…")}
            })
        if(result.outcome==RewardOutcome.ACCOUNT_CHANGED)return@action
        state.update{it.copy(subscription=result.subscription ?: it.subscription,rewardMessage=when(result.outcome){
            RewardOutcome.CANCELED -> "Anuncio cerrado sin recompensa. Puedes volver a intentarlo."
            RewardOutcome.CREDITED -> "¡Tienes 1 seguimiento extra! Elige tu selección y guárdala."
            else -> "Recompensa pendiente. Se actualizará al recibir la confirmación de AdMob."
        })}
        } catch(error:Exception){state.update{it.copy(rewardMessage=null)};throw error}
    }
    fun setForeground(active: Boolean) {
        pollJob?.cancel(); pollJob = null
        if(active) refreshNotificationState()
        if (active) pollJob = viewModelScope.launch {
            while (isActive) {
                delay(15000)
                if (!state.value.loggedIn || state.value.busy) continue
                val started=account.session ?: continue
                try {
                    val bets = tracking.bets()
                    val subscription = subscriptions.subscription()
                    if(!started.sameOwner(account.session))continue
                    state.update { it.copy(bets = bets, subscription = subscription, offline = false,
                        unpinnedBets = bets.filter { b -> notifications.isUnpinned(b.id) }.map { b -> b.id }.toSet()) }
                    if (subscription.access.pushNotifications) publishNotifications()
                    else { notifications.dismissAll(); syncNotificationState() }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    if(!started.sameOwner(account.session))continue
                    state.update { it.copy(offline = true) }
                    if (state.value.subscription?.access?.pushNotifications == true)
                        publishNotifications()
                }
            }
        }
    }
    fun date(date: LocalDate) = action {
        val events = tracking.events(state.value.selectedSport, date)
        state.update { it.copy(date = date, events = events) }
    }
    fun selectSport(sport: String) = action {
        require(state.value.sports.any { it.sport == sport && it.enabled }) { "El deporte aún no está habilitado" }
        val events = tracking.events(sport, state.value.date)
        state.update { it.copy(selectedSport = sport, events = events, players = emptyList(), playersEventId = null) }
    }
    fun players(eventId: String) = action {
        state.update { it.copy(players = emptyList(), playersEventId = null) }
        val players = tracking.players(eventId)
        state.update { it.copy(players = players, playersEventId = eventId) }
    }
    fun create(bet: CreateBet, onSaved: (BetView) -> Unit) = action {
        val created = tracking.create(bet)
        state.update { it.copy(bets = listOf(created) + it.bets.filterNot { b -> b.id == created.id },rewardMessage=null) }
        onSaved(created)
        refreshAfterMutation()
        if (state.value.subscription?.access?.pushNotifications == true)
            publishNotifications()
    }
    fun delete(id: String) = action {
        tracking.delete(id); notifications.remove(id)
        state.update { it.copy(bets = it.bets.filterNot { b -> b.id == id }, unpinnedBets = it.unpinnedBets - id, pinnedBets=it.pinnedBets-id) }
        refreshAfterMutation()
    }
    private suspend fun refreshAfterMutation() {
        // A successful write must stay successful even if a later read loses connectivity.
        state.update { it.copy(subscription = it.subscription?.copy(activeTrackers = it.bets.count { b -> b.status !in settledStatuses }.toLong())) }
        try {
            val bets = tracking.bets()
            val subscription = subscriptions.subscription()
            state.update { it.copy(bets = bets, subscription = subscription, offline = false) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            state.update { it.copy(offline = true, error = "El cambio se guardó. No se pudo actualizar la consulta; pulsa Actualizar para reintentar.") }
        }
    }
    fun openNotification(id: String) { state.update { it.copy(notificationBetId = id, notificationRequest = it.notificationRequest+1) } }
    fun unpin(id: String) { notifications.unpin(id); syncNotificationState() }
    fun pin(id: String) {
        val current=state.value
        if(current.subscription?.access?.pushNotifications != true) return
        val bet=current.bets.find { it.id == id } ?: return
        if(!notifications.pin(bet.copy(stale=bet.stale || current.offline)))
            state.update { it.copy(error=if(!notifications.liveEnabled()) "Habilita las notificaciones de MyBetLive en Android."
                else "No se pudo anclar este dato. Pulsa Actualizar para obtener el estado más reciente.") }
        syncNotificationState()
    }
    fun logout() = action {
        account.logout(); notifications.clear()
        state.value = AppState()
    }
    fun billingMessage(message: String) { state.update { it.copy(billingMessage = message) } }
    fun verifyPurchases(tokens: List<String>) {
        val account = state.value.subscription?.playAccountId ?: return
        viewModelScope.launch {
            workLock.withLock {
                if (state.value.subscription?.playAccountId != account || !state.value.loggedIn) return@withLock
                state.update { it.copy(busy = true, billingMessage = "Verificando compra…") }
                try {
                    for (token in tokens.distinct()) {
                        val subscription = subscriptions.purchase(token)
                        if (state.value.subscription?.playAccountId == account) state.update { it.copy(subscription = subscription) }
                    }
                    state.update { it.copy(billingMessage = "Compras verificadas. Tu plan está actualizado.") }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    val message = failures.serverMessage(error)
                    state.update { it.copy(billingMessage = message ?: "No se pudo verificar la compra. Usa Restaurar compras para reintentar.") }
                } finally { state.update { it.copy(busy = false) } }
            }
        }
    }
}
