package com.mybetlive.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mybetlive.application.BankOperations
import com.mybetlive.bank.*
import com.mybetlive.data.*
import com.mybetlive.i18n.AppStrings

/** Instrumentation-only preview. Not exported, absent from release, and never accesses a server/session. */
class BankDevicePreviewActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  AppStrings.resources=resources
  val bank=BankView("device-fixture","Demo local","MXN","America/Mexico_City","2026-10-01T06:00:00Z","10000.00",version=1,capital="11250.00",reserved="1500.00",available="9750.00",profit="1250.00",roi="10.00",writable=true)
  val bet=BankBet("bet-fixture",bank.id,"2026-10-12T18:00:00Z","BACK","500.00","1.85",bookmaker="Casino de muestra",result="WON",payout="925.00",exposure="500.00",profit="425.00",settledAt="2026-10-13T07:00:00Z",selections=listOf(BankLeg(title="Atlas · Más de 8.5 córners",sport="SOCCER")))
  val summary=BankSummary(bank,BankAccess(true,null,bank.id),listOf(bet),listOf(BankPoint("2026-10-01","0"),BankPoint("2026-10-05","-300"),BankPoint("2026-10-12","1250")),emptyList(),3,40,"2026-10-13T08:00:00Z",mapOf("benefit" to "1250.00","roi" to "10.00","bets" to "40","successPercent" to "60.00"))
  val state=BankState(listOf(bank),summary,BankPage(listOf(bet),0,1,1),from="2026-10-01",to="2026-10-13")
  val model=BankViewModel(object:BankOperations{
   override fun owner()="isolated-device-preview"
   override fun cached()=BankCache()
   override fun remember(cache:BankCache){}
   override suspend fun <T> call(action:suspend(BankApi)->T):T=error("Device preview cannot access any API")
  })
  setContent { MyBetLiveTheme {
   DashboardScreen(AppState(loggedIn=true,username="Demo local"),emptyMap(),{},{},{},{},{},{},{},{},{},bankContent={_,_->BankScreen(state,model,emptyList(),{})})
  }}
 }
}
