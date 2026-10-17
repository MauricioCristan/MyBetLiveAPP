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
import java.time.ZoneId

data class AppState(
    val loggedIn: Boolean = false, val username: String = "", val busy: Boolean = false,
    val emailMessage:String?=null,val emailLink:EmailAccountLink?=null,val authUrl:String?=null,
    val error: String? = null, val bets: List<BetView> = emptyList(), val events: List<EventView> = emptyList(),
    val sports: List<SportInfo> = emptyList(), val players: List<Player> = emptyList(), val offline: Boolean = false,
    val playersEventId: String? = null,
    val history:List<BetView>?=null,val historyPage:Int=-1,val historyTotal:Long?=null,val historySport:String="ALL",
    val historyHasMore:Boolean=false,val historyLoading:Boolean=false,val historyError:String?=null,
    val date: LocalDate = LocalDate.now(), val selectedSport: String = "NFL",
    val today: LocalDate = LocalDate.now(), val timeZone: String = ZoneId.systemDefault().id, val followingToday: Boolean = true,
    val subscription: MySubscription? = null, val billingMessage: String? = null,val rewardMessage:String?=null,
    val trackingBanks:List<BankView> = emptyList(),val trackingBanksLoading:Boolean=false,val trackingBanksError:String?=null,
    val notificationBetId: String? = null, val notificationRequest: Long = 0,
    val pinnedBets: Set<String> = emptySet(), val deviceNotificationsEnabled: Boolean = true
)

class TrackingViewModel(app: Application,
    private val account: SessionOperations, private val tracking: TrackingOperations,
    private val subscriptions: SubscriptionOperations, private val rewards: RewardOperations,
    private val devices: DeviceOperations, private val notifications: TrackerNotifications,
    private val failures: FailureMessages, private val banks:BankOperations
) : AndroidViewModel(app) {
    private val state = MutableStateFlow(AppState(loggedIn = account.session != null,
        username = account.session?.username.orEmpty(), bets = tracking.cachedBets,
        offline = account.session != null))
    val ui = state.asStateFlow()
    private var historyJob:Job?=null
    private var historyRequest=0L
    private var pollJob: Job? = null
    private val workLock = Mutex()
    private var notificationSyncJob: Job? = null
    private val stopNotificationObserver = notifications.observe(::notificationChanged)
    init { syncNotificationState(); if (state.value.loggedIn) refresh() }
    init {
        viewModelScope.launch {
            com.mybetlive.composition.ClientDependencies.live.state.collect { live ->
                if(state.value.loggedIn && account.session==null) {
                    notifications.clear()
                    state.value=AppState(error="Tu sesión expiró. Inicia sesión de nuevo.")
                }
                if(state.value.loggedIn && live.owner==account.session?.trackingOwner())
                    state.update { val byId=live.bets.associateBy{b->b.id};it.copy(bets=live.bets,history=it.history?.mapNotNull{b->byId[b.id]},offline=live.offline) }
            }
        }
    }

    fun syncNotificationState() {
        val active=notifications.activeIds()
        val enabled=notifications.liveEnabled()
        state.update { current -> current.copy(deviceNotificationsEnabled=enabled,
            pinnedBets=current.bets.filter { it.id in active && it.status !in settledStatuses }.map { it.id }.toSet()) }
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
        state.value.bets.forEach { notifications.show(it.copy(stale=it.stale || state.value.offline)) }
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
                if(error is SessionExpiredException) {
                    notifications.clear();state.value=AppState(error=error.message)
                    return@withLock
                }
                state.update { it.copy(error = failures.message(error), offline = it.offline || error is java.io.IOException) }
            } finally { state.update { it.copy(busy = false) } }
          }
        }
    }
    fun login(url: String, username: String, password: String) = action {
        account.login(url, username, password)
        state.update { it.copy(loggedIn = true, username = username, bets = emptyList(), offline = false,history=null,historyTotal=null,historyPage=-1,historyLoading=false,historyHasMore=false,historyError=null) }
        load()
    }
    private fun emailAccounts()=account as? EmailAccountOperations ?: error("El registro por correo no está disponible.")
    fun clearEmailFeedback(){state.update{it.copy(error=null,emailMessage=null)}}
    fun registerEmail(url:String,username:String,email:String,password:String)=action {
        emailAccounts().registerEmail(url,username,email,password)
        state.update{it.copy(emailMessage="Te enviamos un enlace para confirmar tu correo. Revisa también Spam.")}
    }
    fun forgotPassword(url:String,email:String)=action {
        emailAccounts().forgotPassword(url,email)
        state.update{it.copy(emailMessage="Si el correo tiene una cuenta, recibirás un enlace para recuperar tu contraseña.")}
    }
    fun resendVerification(url:String,email:String)=action {
        emailAccounts().resendVerification(url,email)
        state.update{it.copy(emailMessage="Si hay una cuenta pendiente, recibirás un enlace de confirmación.")}
    }
    fun openEmailLink(link:EmailAccountLink){if(state.value.emailLink!=link)state.update{it.copy(emailLink=link,authUrl=com.mybetlive.BuildConfig.AUTH_BACKEND_URL,error=null,emailMessage=null)}}
    fun closeEmailLink(){state.update{it.copy(emailLink=null,error=null,emailMessage=null)}}
    fun verifyEmail()=action {
        val link=state.value.emailLink?:return@action
        emailAccounts().verifyEmail(com.mybetlive.BuildConfig.AUTH_BACKEND_URL,link.token)
        state.update{it.copy(emailMessage="Correo confirmado. Ya puedes iniciar sesión.")}
    }
    fun resetPassword(password:String)=action {
        val link=state.value.emailLink?:return@action
        emailAccounts().resetPassword(com.mybetlive.BuildConfig.AUTH_BACKEND_URL,link.token,password)
        runCatching{account.logout()};notifications.clear()
        state.update{it.copy(loggedIn=false,username="",bets=emptyList(),subscription=null,emailMessage="Contraseña actualizada. Inicia sesión con tu nueva contraseña.")}
    }
    fun loginGoogle(url: String, credential: suspend (GoogleChallenge) -> String) = action {
        val challenge = account.googleChallenge(url)
        val token = credential(challenge)
        account.googleLogin(url, token, challenge.nonce)
        state.update { it.copy(loggedIn = true, username = account.session?.username.orEmpty(), bets = emptyList(), offline = false,history=null,historyTotal=null,historyPage=-1,historyLoading=false,historyHasMore=false,historyError=null) }
        load()
    }
    private suspend fun load() {
        updateCalendar()
        val subscription = subscriptions.subscription()
        state.update { it.copy(subscription = subscription) }
        if (!subscription.access.pushNotifications) notifications.dismissAll()
        val bets = tracking.bets()
        state.update { it.copy(bets = bets, offline = false) }
        if (subscription.access.pushNotifications) publishNotifications() else syncNotificationState()
        loadHistory(state.value.historySport)
        val sports = tracking.sports()
        state.update { it.copy(sports = sports) }
        // Device registration must not depend on the sporting provider's quota/catalog.
        runCatching { devices.registerDevice() }
        val events = tracking.events(state.value.selectedSport, state.value.date)
        state.update { it.copy(events = events) }
    }
    fun loadHistory(sport:String){
        historyJob?.cancel();historyRequest++
        state.update{it.copy(historySport=sport,historyPage=-1,historyTotal=null,historyHasMore=false,historyLoading=false,historyError=null,history=if(it.historySport==sport)it.history else emptyList())}
        fetchHistory(0)
    }
    fun loadMoreHistory(){
        val current=state.value
        if(current.historyLoading||!current.historyHasMore)return
        fetchHistory(current.historyPage+1)
    }
    private fun fetchHistory(page:Int){
        val started=account.session?:return
        val sport=state.value.historySport
        val request=++historyRequest
        state.update{it.copy(historyLoading=true,historyError=null)}
        historyJob=viewModelScope.launch{
            try{
                val result=tracking.history(sport.takeUnless{it=="ALL"},page)
                if(request!=historyRequest||!started.sameOwner(account.session))return@launch
                state.update{current->current.copy(history=(if(page==0)result.items else current.history.orEmpty()+result.items).distinctBy{it.id},
                    historyPage=result.page,historyTotal=result.total,historyHasMore=result.hasMore,historyLoading=false,historyError=null,offline=false)}
            }catch(error:Exception){
                if(error is CancellationException)throw error
                if(request!=historyRequest||!started.sameOwner(account.session))return@launch
                state.update{it.copy(historyLoading=false,historyError=failures.message(error),offline=it.offline||error is java.io.IOException)}
            }finally{
                if(request==historyRequest&&started.sameOwner(account.session))state.update{it.copy(historyLoading=false)}
            }
        }
    }
    fun refresh() = action { load() }
    private suspend fun updateCalendar(): Boolean {
        val before=state.value
        val calendar=tracking.calendar()
        state.update { it.withCalendar(calendar) }
        return before.date!=state.value.date || before.timeZone!=state.value.timeZone
    }
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
        com.mybetlive.composition.ClientDependencies.sync.foreground.value=active
        pollJob?.cancel(); pollJob = null
        if(active) refreshNotificationState()
        if (active) pollJob = viewModelScope.launch {
            while (isActive) {
                delay(15000)
                if (!state.value.loggedIn || state.value.busy) continue
                val started=account.session ?: continue
                try {
                    if(updateCalendar()) {
                        val current=state.value
                        val events=tracking.events(current.selectedSport,current.date)
                        if(!started.sameOwner(account.session))continue
                        state.update { it.copy(events=events) }
                    }
                    val bets = tracking.bets()
                    val subscription = subscriptions.subscription()
                    if(!started.sameOwner(account.session))continue
                    state.update { it.copy(bets = bets, subscription = subscription, offline = false) }
                    if (subscription.access.pushNotifications) publishNotifications()
                    else { notifications.dismissAll(); syncNotificationState() }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    if(error is SessionExpiredException) {
                        notifications.clear();state.value=AppState(error=error.message);continue
                    }
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
        state.update { it.copy(date = date, events = events, followingToday = date == it.today) }
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
    private var bankDestinationsJob:Job?=null
    fun loadTrackingBanks(){
        val started=account.session?:return
        bankDestinationsJob?.cancel()
        state.update{it.copy(trackingBanks=emptyList(),trackingBanksLoading=true,trackingBanksError=null)}
        bankDestinationsJob=viewModelScope.launch {
            try {
                val rows=banks.call{it.list()}
                if(started.sameOwner(account.session))state.update{it.copy(trackingBanks=rows,trackingBanksLoading=false)}
            } catch(error:Exception) {
                if(error is CancellationException)throw error
                if(started.sameOwner(account.session))state.update{it.copy(trackingBanksLoading=false,trackingBanksError=failures.message(error))}
            }
        }
    }
    fun create(bet: CreateBet, onSaved: (BetView) -> Unit) = action {
        if(bet.bankId!=null && (bet.stake==null||bet.decimalOdds==null||bet.operationId==null||bet.bankVersion==null))
            throw IllegalArgumentException("Selecciona un bank y revisa importe y momio.")
        val created = tracking.create(bet)
        state.update { it.copy(bets = listOf(created) + it.bets.filterNot { b -> b.id == created.id },rewardMessage=null) }
        onSaved(created)
        refreshAfterMutation()
        if (state.value.subscription?.access?.pushNotifications == true)
            publishNotifications()
    }
    fun delete(id: String) = action {
        tracking.delete(id); notifications.remove(id)
        state.update { it.copy(bets = it.bets.filterNot { b -> b.id == id }, history=it.history?.filterNot{b->b.id==id}, pinnedBets=it.pinnedBets-id) }
        refreshAfterMutation()
    }
    private suspend fun refreshAfterMutation() {
        if(state.value.history!=null)loadHistory(state.value.historySport)
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
    fun openNotification(id: String) {
        state.update{it.copy(notificationBetId=id,notificationRequest=it.notificationRequest+1)}
        if(state.value.bets.none{it.id==id}){
            val started=account.session?:return
            viewModelScope.launch{try{val row=tracking.bet(id);if(started.sameOwner(account.session))state.update{it.copy(bets=(it.bets+row).distinctBy{b->b.id})}}
                catch(error:Exception){if(error is CancellationException)throw error;if(started.sameOwner(account.session))state.update{it.copy(error=failures.message(error))}}}
        }
    }
    fun logout() = action {
        historyRequest++;historyJob?.cancel()
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
