package com.mybetlive.ads

import com.mybetlive.data.MySubscription

object AdsPolicy {
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    fun rewardedUnitId(subscription:MySubscription?,offline:Boolean,debug:Boolean,appId:String):String? {
        val reward=subscription?.reward ?: return null
        if(offline || !reward.enabled || subscription.ads?.enabled!=true || subscription.subscriptions.any{it.active&&it.plan.code!="FREE"})return null
        if(debug)return TEST_REWARDED_ID.takeIf{reward.testAvailable}
        if(subscription.ads.testMode || appId==TEST_APP_ID || !appId.matches(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}")))return null
        return reward.unitId.takeIf{it.matches(Regex("ca-app-pub-[0-9]{16}/[0-9]{10}")) && !it.startsWith("ca-app-pub-3940256099942544/") && it.substringBefore('/')==appId.substringBefore('~')}
    }
    fun unitId(subscription: MySubscription?, offline: Boolean, debug: Boolean, appId: String): String? {
        val ads = subscription?.ads ?: return null
        if (offline || !ads.enabled || (!ads.eventsEnabled && !ads.historyEnabled && !ads.trackersEnabled) || subscription.subscriptions.any { it.active && it.plan.code != "FREE" }) return null
        if (debug) return TEST_NATIVE_ID // Debug can never send requests to a real ad unit.
        if (ads.testMode || appId == TEST_APP_ID || !appId.matches(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}"))) return null
        return ads.nativeUnitId.takeIf { it.matches(Regex("ca-app-pub-[0-9]{16}/[0-9]{10}")) &&
            !it.startsWith("ca-app-pub-3940256099942544/") && it.substringBefore('/') == appId.substringBefore('~') }
    }
    fun positions(count: Int, every: Int, max: Int): Set<Int> {
        if (count <= 0 || every !in 3..50 || max !in 1..3) return emptySet()
        return if (count < every) setOf(count) else (every..count step every).take(max).toSet()
    }
}
