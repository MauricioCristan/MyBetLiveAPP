package com.mybetlive

import com.mybetlive.ads.AdsPolicy
import com.mybetlive.data.*
import org.junit.Assert.*
import org.junit.Test

class AdsPolicyTest {
 private val free=Plan("f","FREE","Free","",true,null,null,1,false,true)
 private val premium=free.copy(id="p",code="PREMIUM")
 private fun customer(ads:AdsConfig=AdsConfig(enabled=true), grants:List<CustomerSubscription> = emptyList())=
  MySubscription(free,PlanAccess(1,false,true),0,grants,emptyList(),false,"customer",ads)
 private fun unit(s:MySubscription?,offline:Boolean=false,debug:Boolean=true,app:String=AdsPolicy.TEST_APP_ID)=AdsPolicy.unitId(s,offline,debug,app)
 @Test fun unknownOfflineDisabledAndPremiumNeverRequestAds(){
  assertNull(unit(null));assertNull(unit(customer(),offline=true));assertNull(unit(customer(AdsConfig())))
  assertNull(unit(customer(AdsConfig(enabled=true,eventsEnabled=false,historyEnabled=false,trackersEnabled=false))))
  assertEquals(AdsPolicy.TEST_NATIVE_ID,unit(customer(AdsConfig(enabled=true,eventsEnabled=false,historyEnabled=false,trackersEnabled=true))))
  for(source in listOf("MANUAL","GOOGLE_PLAY")) for(state in listOf("ACTIVE","CANCELED","IN_GRACE_PERIOD")) {
   val grant=CustomerSubscription("g",premium,source,state,"2099-01-01T00:00:00Z",true)
   assertNull(unit(customer(grants=listOf(grant))))
   assertEquals(AdsPolicy.TEST_NATIVE_ID,unit(customer(grants=listOf(grant.copy(active=false)))))
  }
 }
 @Test fun debugAlwaysUsesSampleUnitAndReleaseRequiresOwnMatchingPublisher(){
  val live=AdsConfig(enabled=true,testMode=false,nativeUnitId="ca-app-pub-1234567890123456/1234567890")
  assertEquals(AdsPolicy.TEST_NATIVE_ID,unit(customer(live)))
  assertNull(unit(customer(live),debug=false))
  val app="ca-app-pub-1234567890123456~0987654321"
  assertEquals(live.nativeUnitId,unit(customer(live),debug=false,app=app))
  assertNull(unit(customer(live.copy(testMode=true)),debug=false,app=app))
  assertNull(unit(customer(live.copy(nativeUnitId=AdsPolicy.TEST_NATIVE_ID)),debug=false,app=app))
  assertNull(unit(customer(live),debug=false,app="ca-app-pub-9999999999999999~0987654321"))
 }
 @Test fun placementCapsAdsAndHandlesShortEmptyAndInvalidLists(){
  assertEquals(setOf(2),AdsPolicy.positions(2,5,2));assertEquals(setOf(5,10),AdsPolicy.positions(100,5,2))
  assertEquals(setOf(3,6,9),AdsPolicy.positions(12,3,3));assertEquals(emptySet<Int>(),AdsPolicy.positions(0,5,2))
  assertEquals(emptySet<Int>(),AdsPolicy.positions(10,2,2));assertEquals(emptySet<Int>(),AdsPolicy.positions(10,5,4))
 }
 @Test fun rewardedAdsNeedServerEligibilityAndNeverUseRealUnitsInDebug(){
  val reward=RewardStatus(enabled=true,eligible=true,testAvailable=true,unitId="ca-app-pub-1234567890123456/1234567890")
  val s=customer().copy(reward=reward)
  assertEquals(AdsPolicy.TEST_REWARDED_ID,AdsPolicy.rewardedUnitId(s,false,true,AdsPolicy.TEST_APP_ID))
  assertNull(AdsPolicy.rewardedUnitId(s.copy(reward=reward.copy(testAvailable=false)),false,true,AdsPolicy.TEST_APP_ID))
  assertNull(AdsPolicy.rewardedUnitId(s,true,true,AdsPolicy.TEST_APP_ID))
  assertNull(AdsPolicy.rewardedUnitId(s.copy(reward=reward.copy(enabled=false)),false,true,AdsPolicy.TEST_APP_ID))
  assertNull(AdsPolicy.rewardedUnitId(s.copy(subscriptions=listOf(CustomerSubscription("g",premium,"MANUAL","ACTIVE","2099-01-01T00:00:00Z",true))),false,true,AdsPolicy.TEST_APP_ID))
  val live=s.copy(ads=s.ads!!.copy(testMode=false))
  assertEquals(reward.unitId,AdsPolicy.rewardedUnitId(live,false,false,"ca-app-pub-1234567890123456~0987654321"))
  assertNull(AdsPolicy.rewardedUnitId(live,false,false,AdsPolicy.TEST_APP_ID))
 }
}
