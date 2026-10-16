package com.mybetlive.data
data class Plan(val id:String,val code:String,val name:String,val description:String,val published:Boolean,
 val playProductId:String?,val playBasePlanId:String?,val maxActiveTrackers:Int?,val playerProps:Boolean,val pushNotifications:Boolean)
data class PlanAccess(val maxActiveTrackers:Int?,val playerProps:Boolean,val pushNotifications:Boolean)
data class CustomerSubscription(val id:String,val plan:Plan,val source:String,val state:String,val expiresAt:String,val active:Boolean)
data class MySubscription(val plan:Plan,val access:PlanAccess,val activeTrackers:Long,val subscriptions:List<CustomerSubscription>,
 val availablePlans:List<Plan>,val billingEnabled:Boolean,val playAccountId:String,val ads:AdsConfig?=null,val reward:RewardStatus?=null,val bankAccess:BankAccess?=null)

data class RewardStatus(val enabled:Boolean=false,val eligible:Boolean=false,val creditAvailable:Boolean=false,
 val activeExtra:Long=0,val creditExpiresAt:String?=null,val unitId:String="",val testAvailable:Boolean=false)
data class RewardIntent(val id:String,val unitId:String,val accountId:String,val test:Boolean,val expiresAt:String)
data class PrepareReward(val test:Boolean)
data class AdsConfig(val enabled:Boolean=false,val testMode:Boolean=true,val eventsEnabled:Boolean=true,
 val historyEnabled:Boolean=true,val everyItems:Int=5,val maxAdsPerScreen:Int=2,val nativeUnitId:String="",val trackersEnabled:Boolean=true)
data class PurchaseRequest(val purchaseToken:String)
data class StorePrice(val price:String,val period:String,val autoRenewing:Boolean)
