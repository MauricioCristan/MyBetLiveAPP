package com.mybetlive.application

import com.mybetlive.data.*
import java.time.LocalDate

interface SessionOperations {
    val session: Session?
    suspend fun login(url: String, username: String, password: String)
    suspend fun googleChallenge(url: String): GoogleChallenge
    suspend fun googleLogin(url: String, token: String, nonce: String)
    suspend fun logout()
}
interface TrackingOperations {
    val cachedBets: List<BetView>
    suspend fun sports(): List<SportInfo>
    suspend fun events(sport: String, date: LocalDate): List<EventView>
    suspend fun players(id: String): List<Player>
    suspend fun bets(): List<BetView>
    suspend fun create(bet: CreateBet): BetView
    suspend fun delete(id: String)
}
interface SubscriptionOperations {
    suspend fun subscription(): MySubscription
    suspend fun purchase(token: String): MySubscription
}
interface RewardOperations {
    suspend fun rewardIntent(): RewardIntent
    suspend fun rewardTestComplete(id: String): RewardStatus
}
interface DeviceOperations { suspend fun registerDevice() }
interface TrackerNotifications {
    fun activeIds(): Set<String>
    fun liveEnabled(): Boolean
    fun observe(listener: () -> Unit): () -> Unit
    fun isUnpinned(id: String): Boolean
    fun show(bet: BetView)
    fun pin(bet: BetView): Boolean
    fun unpin(id: String)
    fun remove(id: String)
    fun dismissAll()
    fun clear()
}
interface FailureMessages {
    fun message(error: Throwable): String
    fun serverMessage(error: Throwable): String?
}
