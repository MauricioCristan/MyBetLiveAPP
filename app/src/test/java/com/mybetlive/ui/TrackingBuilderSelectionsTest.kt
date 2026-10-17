package com.mybetlive.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.mybetlive.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="es-rMX-w411dp-h891dp-xxhdpi")
class TrackingBuilderSelectionsTest {
 @get:Rule val compose=createComposeRule()
 private val event=EventSnapshot("SOCCER","TEST","1","2026-10-16T20:00:00Z",Team("n","Necaxa"),Team("a","Atlas"),1,1,"LIVE","2H",null,"2026-10-16T21:00:00Z")
 private val legs=listOf(TrackingBuilderSelection("BOTH_TEAMS_SCORE","YES",status="LIVE_WINNING",currentValue=2.0),TrackingBuilderSelection("TOTAL_CARDS","OVER",2.5,status="THRESHOLD_REACHED",currentValue=3.0,remaining=0.0))
 private fun bet()=BetView("id","game",event,"BOTH_TEAMS_SCORE","YES",null,null,null,null,100.0,3.65,"Casino","LIVE_WINNING",2.0,0.0,"2026-10-16T21:00:00Z",false,builderSelections=legs)
 @Test fun ticketListsEverySelectionAndCanRemoveOne(){
  val rows=mutableStateOf(legs)
  compose.setContent{MyBetLiveTheme{TrackingBuilderSelections(rows.value,event,true){index->rows.value=rows.value.filterIndexed{i,_->i!=index}}}}
  compose.onNodeWithText("Tu boleto · 2/8 selecciones").assertExists()
  compose.onNodeWithText("Ambos equipos anotan",substring=true).assertExists()
  compose.onNodeWithText("2.5",substring=true).assertExists()
  compose.onAllNodesWithText("Quitar")[0].performClick()
  compose.runOnIdle{assertEquals(1,rows.value.size);assertEquals("TOTAL_CARDS",rows.value[0].market)}
 }
 @Test fun parentPresentationUsesLegCountAndNeverTreatsItAsSoccerGoals(){
  val row=bet();assertEquals("2 / 2",row.presentation().value);assertEquals("selecciones",row.presentation().unit)
  assertNull(row.visualProgress());assertTrue(row.title().contains("2.5"));assertTrue(row.title().startsWith("Creador"))
  assertEquals("Sí",legs[0].asBet(row).presentation().value)
 }
 @Test fun detailShowsIndependentLegStates(){
  compose.setContent{MyBetLiveTheme{androidx.compose.foundation.layout.Column{MarketCardContent(bet(),false)}}}
  compose.onNodeWithText("1. "+legs[0].title(event)).assertExists()
  compose.onNodeWithText("2. "+legs[1].title(event)).assertExists()
  compose.onNodeWithText("Selección favorable por ahora",substring=true).assertExists()
 }
 @Test fun pushesUpdateEachCachedLegAndIgnoreMalformedOrMismatchedProgress(){
  val row=bet();val next=row.builderPush("""[{"status":"WON","currentValue":2},{"status":"LOST","currentValue":2,"remaining":1}]""")!!
  assertEquals("LOST",next[1].status);assertEquals(2.0,next[1].currentValue)
  assertEquals(legs,row.builderPush("[]"));assertEquals(legs,row.builderPush("not json"))
 }
 @Test fun notificationPresentationUsesTheSameBuilderCount(){
  val p=marketPresentation("SOCCER","BET_BUILDER","YES",2.0,1.0,"LIVE_LOSING",1,1)
  assertEquals("1 / 2",p.value);assertEquals(MarketVisual.OUTCOME,p.visual)
  assertNull(marketProgress("SOCCER","BET_BUILDER","YES",2.0,1.0,"LIVE_LOSING",1,1))
 }
}
