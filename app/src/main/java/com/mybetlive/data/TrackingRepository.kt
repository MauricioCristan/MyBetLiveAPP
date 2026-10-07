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
import java.time.ZoneOffset

interface TrackingApi {
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
    @GET("api/v1/tracking/events") suspend fun events(@Query("sport") sport: String, @Query("date") date: String): List<EventView>
    @GET("api/v1/tracking/events/{id}/players") suspend fun players(@Path("id") id: String): List<Player>
    @GET("api/v1/tracking/bets") suspend fun bets(): List<BetView>
    @POST("api/v1/tracking/bets") suspend fun create(@Body bet: CreateBet): BetView
    @DELETE("api/v1/tracking/bets/{id}") suspend fun delete(@Path("id") id: String)
    @PUT("api/v1/tracking/devices/{id}") suspend fun device(@Path("id") id: String, @Body token: DeviceToken)
    @DELETE("api/v1/tracking/devices/{id}") suspend fun removeDevice(@Path("id") id: String)
}

class TrackingRepository(context: Context,private val deviceTokens:DeviceTokenSource,private val credentials:CredentialCleaner) : SessionOperations, TrackingOperations,
    SubscriptionOperations, RewardOperations, DeviceOperations {
    override suspend fun rewardIntent()=authorized {it.rewardIntent(PrepareReward(BuildConfig.DEBUG))}
    override suspend fun rewardTestComplete(id:String)=authorized {it.rewardTestComplete(id)}
    val vault = SessionVault(context)
    override val session get() = vault.session
    override val cachedBets get() = vault.cachedBets
    private val endpoints = TrackingApiFactory(session = { vault.session })
    private val calls=AuthorizedCalls({vault.session},{vault.session=it},endpoints::api)
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
    override suspend fun subscription() = authorized { it.subscription() }
    override suspend fun purchase(token: String) = authorized { it.purchase(PurchaseRequest(token)) }
    override suspend fun events(sport: String, date: LocalDate) = authorized { it.events(sport, date.toString()) }
    override suspend fun players(id: String) = authorized { it.players(id) }
    override suspend fun bets(): List<BetView> = authorized { it.bets() }.also { vault.cachedBets = it }
    override suspend fun create(bet: CreateBet) = authorized { it.create(bet) }.also { saved ->
        vault.cachedBets = listOf(saved) + vault.cachedBets.filterNot { it.id == saved.id }
    }
    override suspend fun delete(id: String) = authorized { it.delete(id) }.also {
        vault.cachedBets = vault.cachedBets.filterNot { it.id == id }
    }
    override suspend fun registerDevice() {
        deviceTokens.token()?.let {vault.fcmToken=it}
        vault.fcmToken?.let { token -> authorized { it.device(vault.installationId, DeviceToken(token)) } }
    }
    override suspend fun logout() {
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
        runCatching {credentials.clear()}
    }
}
