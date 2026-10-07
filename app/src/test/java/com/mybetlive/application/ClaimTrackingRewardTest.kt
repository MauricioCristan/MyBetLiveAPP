package com.mybetlive.application
import com.mybetlive.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ClaimTrackingRewardTest {
    private val plan=Plan("free","FREE","Free","",true,null,null,1,false,true)
    private fun account(credit:Boolean)=MySubscription(plan,PlanAccess(if(credit)2 else 1,false,true),1,emptyList(),listOf(plan),false,"owner",reward=RewardStatus(creditAvailable=credit))
    private class Rewards(private val test:Boolean):RewardOperations {
        var completed=0
        override suspend fun rewardIntent()=RewardIntent("intent","unit","owner",test,"later")
        override suspend fun rewardTestComplete(id:String):RewardStatus {completed++;return RewardStatus(creditAvailable=true)}
    }
    private class Subscriptions(private val read:suspend()->MySubscription):SubscriptionOperations {
        override suspend fun subscription()=read()
        override suspend fun purchase(token:String)=error("Purchase not part of reward use case")
    }
    @Test fun canceledVideoDoesNotConfirmOrReadAReward()=runBlocking {
        val rewards=Rewards(true);var reads=0
        val result=ClaimTrackingReward(rewards,Subscriptions {reads++;account(false)},wait={}).execute({false},{true})
        assertEquals(RewardOutcome.CANCELED,result.outcome);assertEquals(0,rewards.completed);assertEquals(0,reads)
    }
    @Test fun productionWaitsForServerCreditWithoutClientConfirmation()=runBlocking {
        val rewards=Rewards(false);var reads=0;var waits=0
        val result=ClaimTrackingReward(rewards,Subscriptions {reads++;account(reads>=2)},wait={waits++}).execute({true},{true})
        assertEquals(RewardOutcome.CREDITED,result.outcome);assertEquals(0,rewards.completed);assertEquals(1,waits)
    }
    @Test fun changingAccountDuringTheVideoCannotConfirmForTheNewAccount()=runBlocking {
        val rewards=Rewards(true);var current=true
        val result=ClaimTrackingReward(rewards,Subscriptions {account(true)},wait={}).execute({current=false;true},{current})
        assertEquals(RewardOutcome.ACCOUNT_CHANGED,result.outcome);assertEquals(0,rewards.completed)
    }
    @Test fun completedTestVideoConfirmsOnceAndMissingSsvStaysPending()=runBlocking {
        val rewards=Rewards(true);var waits=0
        val result=ClaimTrackingReward(rewards,Subscriptions {account(false)},wait={waits++}).execute({true},{true})
        assertEquals(RewardOutcome.PENDING,result.outcome);assertEquals(1,rewards.completed);assertEquals(10,waits)
    }
}
