package com.mybetlive.application

import com.mybetlive.data.MySubscription
import com.mybetlive.data.RewardIntent
import kotlinx.coroutines.delay

enum class RewardStage { LOADING, CONFIRMING }
enum class RewardOutcome { CANCELED, CREDITED, PENDING, ACCOUNT_CHANGED }
data class RewardClaim(val outcome: RewardOutcome, val subscription: MySubscription? = null)

class ClaimTrackingReward(
    private val rewards: RewardOperations,
    private val subscriptions: SubscriptionOperations,
    private val wait: suspend () -> Unit = { delay(2000) }
) {
    suspend fun execute(show: suspend (RewardIntent) -> Boolean, current: () -> Boolean,
        progress: (RewardStage) -> Unit = {}): RewardClaim {
        val intent=rewards.rewardIntent()
        if(!current()) return RewardClaim(RewardOutcome.ACCOUNT_CHANGED)
        progress(RewardStage.LOADING)
        val earned=show(intent)
        if(!current()) return RewardClaim(RewardOutcome.ACCOUNT_CHANGED)
        if(!earned) return RewardClaim(RewardOutcome.CANCELED)
        if(intent.test) rewards.rewardTestComplete(intent.id)
        if(!current()) return RewardClaim(RewardOutcome.ACCOUNT_CHANGED)
        progress(RewardStage.CONFIRMING)
        var subscription=subscriptions.subscription()
        repeat(10) {
            if(!current()) return RewardClaim(RewardOutcome.ACCOUNT_CHANGED)
            if(subscription.reward?.creditAvailable==true) return RewardClaim(RewardOutcome.CREDITED,subscription)
            wait();subscription=subscriptions.subscription()
        }
        if(!current()) return RewardClaim(RewardOutcome.ACCOUNT_CHANGED)
        return RewardClaim(if(subscription.reward?.creditAvailable==true) RewardOutcome.CREDITED else RewardOutcome.PENDING,subscription)
    }
}
