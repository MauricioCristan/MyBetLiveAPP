package com.mybetlive.data

import android.content.Context
import com.mybetlive.application.*
import com.mybetlive.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import retrofit2.http.*
import java.time.LocalDate
import java.time.ZoneId

interface TrackingApi {
    @POST("api/v1/auth/register") suspend fun registerEmail(@Body request:RegisterEmail)
    @POST("api/v1/auth/password/forgot") suspend fun forgotPassword(@Body request:EmailAddress)
    @POST("api/v1/auth/email/resend") suspend fun resendVerification(@Body request:EmailAddress)
    @POST("api/v1/auth/email/verify") suspend fun verifyEmail(@Body request:EmailToken)
    @POST("api/v1/auth/password/reset") suspend fun resetPassword(@Body request:NewPassword)
    @POST("api/v1/rewards/intents") suspend fun rewardIntent(@Body request:PrepareReward):RewardIntent
    @POST("api/v1/rewards/intents/{id}/test-complete") suspend fun rewardTestComplete(@Path("id") id:String):RewardStatus
    @GET("api/v1/subscriptions/me") suspend fun subscription(): MySubscription
    @POST("api/v1/subscriptions/google-play") suspend fun purchase(@Body request: PurchaseRequest): MySubscription
    @POST("api/v1/auth/login") suspend fun login(@Body login: Login): Tokens
    @POST("api/v1/auth/google/challenge") suspend fun googleChallenge(): GoogleChallenge
    @POST("api/v1/auth/google") suspend fun googleLogin(@Body login: GoogleLogin): GoogleSession
    @POST("api/v1/auth/refresh") suspend fun refresh(@Body token: Refresh): Tokens
    @POST("api/v1/auth/logout") suspend fun logout(@Body token: Refresh)
    @GET("api/v1/tracking/sports") suspend fun sports(): List<SportInfo>
    @GET("api/v1/tracking/calendar") suspend fun calendar(@Query("timezone") timezone: String): CalendarInfo
    @GET("api/v1/tracking/events") suspend fun events(@Query("sport") sport: String, @Query("date") date: String, @Query("timezone") timezone: String = "UTC"): List<EventView>
    @GET("api/v1/tracking/events/{id}/players") suspend fun players(@Path("id") id: String): List<Player>
    @GET("api/v1/tracking/bets") suspend fun bets(@Query("timezone") timezone: String = "UTC"): List<BetView>
    @GET("api/v1/tracking/bets") suspend fun activeBets(@Query("activeOnly") activeOnly: Boolean = true,
        @Query("includeBetIds") includeBetIds: String?, @Query("timezone") timezone: String = "UTC"): List<BetView>
    @POST("api/v1/tracking/bets") suspend fun create(@Body bet: CreateBet,@Query("timezone") timezone: String = "UTC"): BetView
    @DELETE("api/v1/tracking/bets/{id}") suspend fun delete(@Path("id") id: String)
    @PUT("api/v1/tracking/devices/{id}") suspend fun device(@Path("id") id: String, @Body token: DeviceToken)
    @DELETE("api/v1/tracking/devices/{id}") suspend fun removeDevice(@Path("id") id: String)
}

class TrackingRepository(context: Context,private val deviceTokens:DeviceTokenSource,private val credentials:CredentialCleaner) : SessionOperations, TrackingOperations,
    SubscriptionOperations, RewardOperations, DeviceOperations, EmailAccountOperations {
    private val appContext=context.applicationContext
    override suspend fun rewardIntent()=authorized {it.rewardIntent(PrepareReward(BuildConfig.DEBUG))}
    override suspend fun rewardTestComplete(id:String)=authorized {it.rewardTestComplete(id)}
    val vault = SessionVault(context)
    override val session get() = vault.session
    override val cachedBets get() = vault.cachedBets
    private val endpoints = TrackingApiFactory(session = { vault.session })
    private val publicEndpoints=TrackingApiFactory(session={null})
    override suspend fun registerEmail(url:String,username:String,email:String,password:String) {
        publicEndpoints.api(baseUrl(url)).registerEmail(RegisterEmail(username.trim(),email.trim(),password))
    }
    override suspend fun forgotPassword(url:String,email:String){publicEndpoints.api(baseUrl(url)).forgotPassword(EmailAddress(email.trim()))}
    override suspend fun resendVerification(url:String,email:String){publicEndpoints.api(baseUrl(url)).resendVerification(EmailAddress(email.trim()))}
    override suspend fun verifyEmail(url:String,token:String){publicEndpoints.api(baseUrl(url)).verifyEmail(EmailToken(token))}
    override suspend fun resetPassword(url:String,token:String,password:String){publicEndpoints.api(baseUrl(url)).resetPassword(NewPassword(token,password))}
    private val calls=AuthorizedCalls({vault.session},{vault.session=it},endpoints::api) { started ->
        if(started.sameOwner(vault.session)) {
            com.mybetlive.floating.FloatingTrackingController(context=appContext).stop()
            vault.clear();com.mybetlive.composition.ClientDependencies.live.bind(null)
        }
    }
    private fun api(baseUrl: String): TrackingApi = endpoints.api(baseUrl)
    override suspend fun login(url: String, username: String, password: String) {
        val baseUrl = baseUrl(url)
        val tokens = api(baseUrl).login(Login(username.trim(), password))
        vault.cachedBets = emptyList()
        vault.session = Session(baseUrl, username.trim(), tokens)
        runCatching { registerDevice() }
    }
    private fun baseUrl(url: String): String {
        val uri = java.net.URI(url.trim())
        require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null && (uri.scheme == "https" || BuildConfig.DEBUG && uri.scheme == "http")) {
            "Ingresa una URL HTTPS válida (HTTP local disponible en debug)"
        }
        return url.trim().trimEnd('/') + "/"
    }
    override suspend fun googleChallenge(url: String) = api(baseUrl(url)).googleChallenge()
    override suspend fun googleLogin(url: String, token: String, nonce: String) {
        val baseUrl = baseUrl(url)
        val signedIn = api(baseUrl).googleLogin(GoogleLogin(token, nonce))
        vault.cachedBets = emptyList()
        vault.session = Session(baseUrl, signedIn.displayName, signedIn.tokens)
        runCatching { registerDevice() }
    }
    private suspend fun <T> authorized(action: suspend (TrackingApi) -> T): T {
        return calls.execute(action)
    }
    override suspend fun sports() = authorized { it.sports() }
    override suspend fun calendar() = authorized { it.calendar(ZoneId.systemDefault().id) }
    override suspend fun subscription() = authorized { it.subscription() }
    override suspend fun purchase(token: String) = authorized { it.purchase(PurchaseRequest(token)) }
    override suspend fun events(sport: String, date: LocalDate) = authorized { it.events(sport, date.toString(), ZoneId.systemDefault().id) }
    override suspend fun players(id: String) = authorized { it.players(id) }
    override suspend fun bets(): List<BetView> = com.mybetlive.composition.ClientDependencies.sync.read {
        val owner = session?.trackingOwner() ?: error("Inicia sesión")
        val live = com.mybetlive.composition.ClientDependencies.live
        live.bind(owner,vault.cachedBets)
        authorized { it.bets(ZoneId.systemDefault().id) }.let { rows -> live.accept(owner,rows); live.state.value.bets.also { vault.cachedBets=it } }
    }
    suspend fun activeBets(includeIds: List<String>): List<BetView> {
        val owner=session?.trackingOwner() ?: error("Inicia sesión")
        val live=com.mybetlive.composition.ClientDependencies.live
        live.bind(owner,vault.cachedBets)
        val rows=authorized { it.activeBets(includeBetIds=includeIds.joinToString(",").takeIf{v->v.isNotEmpty()},timezone=ZoneId.systemDefault().id) }
        live.accept(owner,rows,partial=true);vault.cachedBets=live.state.value.bets
        return rows
    }
    override suspend fun create(bet: CreateBet) = authorized { it.create(bet,ZoneId.systemDefault().id) }.also { saved ->
        vault.cachedBets = listOf(saved) + vault.cachedBets.filterNot { it.id == saved.id }
    }
    override suspend fun delete(id: String) = authorized { it.delete(id) }.also {
        com.mybetlive.composition.ClientDependencies.live.remove(id)
        vault.cachedBets = vault.cachedBets.filterNot { it.id == id }
    }
    override suspend fun registerDevice() {
        deviceTokens.token()?.let {vault.fcmToken=it}
        vault.fcmToken?.let { token -> authorized { it.device(vault.installationId, DeviceToken(token)) } }
    }
    override suspend fun logout() {
        com.mybetlive.floating.FloatingTrackingController(context = appContext).stop()
        val current = vault.session
        if (current != null) {
            // Unregister first: a new account must never receive the old account's tracked bets.
            if (vault.fcmToken != null) try {
                authorized { it.removeDevice(vault.installationId) }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                // Expired/revoked sessions or offline devices must still be able to sign out.
                // Incoming pushes require a local session with the matching owner; the next
                // registration reassigns this installation/token to its authenticated owner.
            }
            val latest = vault.session ?: current
            runCatching { api(latest.baseUrl).logout(Refresh(latest.tokens.refreshToken)) }
        }
        vault.clear()
        com.mybetlive.composition.ClientDependencies.live.bind(null)
        runCatching {credentials.clear()}
    }
}
