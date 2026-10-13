package com.mybetlive.application

import com.mybetlive.data.*
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking

class LiveTrackingStoreTest {
    private fun bet(id:String="a",status:String="LIVE_WINNING",time:String="2026-10-10T20:00:00Z")=BetView(id,"event",
        EventSnapshot("NBA","fake","event","",Team("h","Home"),Team("a","Away"),20,18,"LIVE","Q1",null,time),
        "TOTAL","OVER",40.5,null,null,null,null,null,null,status,38.0,null,time,false)
    @Test fun partialResponsesKeepHistoryAndSelectedFinalButRemoveMissingActive() {
        val s=LiveTrackingStore();s.bind("one");s.accept("one",listOf(bet(),bet("b"),bet("history","WON")))
        s.accept("one",listOf(bet(status="WON",time="2026-10-10T20:01:00Z")),partial=true)
        assertEquals(setOf("a","history"),s.state.value.bets.map{it.id}.toSet())
        assertTrue(s.state.value.bets.all{it.status=="WON"})
    }
    @Test fun lateResponsesAndPushesCannotCrossAccountsOrResurrectDeletedOrFinalPicks() {
        val s=LiveTrackingStore();s.bind("one");s.accept("one",listOf(bet(status="WON")))
        s.accept("one",listOf(bet()));assertEquals("WON",s.state.value.bets.single().status)
        s.accept("one",listOf(bet(time="2026-10-10T20:05:00Z")));assertEquals("WON",s.state.value.bets.single().status)
        s.remove("a");s.accept("one",listOf(bet()));assertTrue(s.state.value.bets.isEmpty())
        s.bind("two");s.accept("one",listOf(bet()));assertTrue(s.state.value.bets.isEmpty())
        s.push("one",mapOf("betId" to "a"));assertTrue(s.state.value.bets.isEmpty())
    }
    @Test fun pushDistinguishesMissingZeroAndScopedHockeyScores() {
        val s=LiveTrackingStore();s.bind("one");s.accept("one",listOf(bet().copy(event=bet().event.copy(sport="NHL"),scope="REGULATION")))
        s.push("one",mapOf("betId" to "a","status" to "LIVE_LOSING","currentValue" to "0","updatedAt" to "2026-10-10T20:01:00Z","scopeHomeScore" to "2","scopeAwayScore" to "3"))
        assertEquals(0.0,s.state.value.bets.single().currentValue!!,0.0);assertEquals(2 to 3,s.state.value.bets.single().scopedScores())
        s.push("one",mapOf("betId" to "a","status" to "AWAITING_DATA","currentValue" to "","updatedAt" to "2026-10-10T20:02:00Z","scopeHomeScore" to "","scopeAwayScore" to ""))
        assertNull(s.state.value.bets.single().currentValue);assertEquals(null to null,s.state.value.bets.single().scopedScores())
    }
    @Test fun foregroundDisablesBackgroundReads()=runBlocking {
        val c=LiveTrackingCoordinator();var reads=0
        c.foreground.value=true;c.backgroundRead{reads++};assertEquals(0,reads)
        c.foreground.value=false;c.backgroundRead{reads++};assertEquals(1,reads)
    }
}
