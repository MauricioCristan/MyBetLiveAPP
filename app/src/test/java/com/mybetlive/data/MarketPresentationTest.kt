package com.mybetlive.data

import org.junit.Assert.*
import org.junit.Test

class MarketPresentationTest {
    private fun ml(selection:String,home:Int?,away:Int?,stale:Boolean=false)=marketPresentation("SOCCER","MONEYLINE",selection,null,
        home?.let { away?.let { z->(it-z).toDouble() } },"LIVE_WINNING",home,away,stale)
    @Test fun selectedTeamHasStablePositionAndScalesWithScore() {
        val winning=ml("HOME",2,1);assertEquals(66,winning.home!!.size);assertEquals(44,winning.away!!.size)
        val losing=ml("HOME",1,2);assertEquals(36,losing.home!!.size);assertTrue(losing.home.faded);assertEquals(ProgressTone.RED,losing.tone)
        val visitor=ml("AWAY",1,2);assertEquals(66,visitor.away!!.size);assertTrue(visitor.away.selected);assertFalse(visitor.home!!.selected)
    }
    @Test fun drawSelectionControlsBothTeams() {
        val lost=ml("DRAW",2,1);assertEquals(ProgressTone.RED,lost.home!!.tone);assertEquals(ProgressTone.RED,lost.away!!.tone)
        val tie=ml("DRAW",1,1);assertEquals(ProgressTone.GREEN,tie.tone);assertEquals(tie.home!!.size,tie.away!!.size)
        val selectedTeamTie=ml("HOME",1,1);assertEquals(ProgressTone.AMBER,selectedTeamTie.tone)
    }
    @Test fun staleOrMissingScoreNeverLooksFavorable() {
        assertEquals(ProgressTone.MUTED,ml("HOME",2,1,true).tone)
        assertEquals("— : —",ml("HOME",null,null).value)
        assertEquals(ProgressTone.MUTED,ml("HOME",null,null).home!!.tone)
    }
    @Test fun marginIncludesTheHandicapAndPushAtZero() {
        val p=marketPresentation("MLB","SPREAD","HOME",-1.5,2.0,"LIVE_WINNING",5,3)
        assertEquals("Margen +0.5",p.value)
        val push=marketPresentation("NFL","SPREAD","HOME",3.0,-3.0,"LIVE_PUSH",17,20)
        assertEquals("Margen 0",push.value);assertEquals(ProgressTone.AMBER,push.tone)
    }
    @Test fun pendingPropIsNotZeroAndConfirmedZeroIsVisible() {
        val pending=marketPresentation("SOCCER","PLAYER_GOALS","OVER",.5,null,"AWAITING_DATA",0,0)
        assertEquals("— / 0.5",pending.value);assertEquals(ProgressTone.MUTED,pending.tone)
        val zero=marketPresentation("SOCCER","PLAYER_GOALS","OVER",.5,0.0,"LIVE_LOSING",0,0)
        assertEquals("0 / 0.5",zero.value)
    }
    @Test fun underRemainsProvisionalAndFinalResultOverridesLiveWording() {
        val under=marketPresentation("NFL","PLAYER_RECEIVING_YARDS","UNDER",65.5,52.0,"LIVE_WINNING",17,24)
        assertTrue(under.provisional);assertEquals(ProgressTone.GREEN,under.tone)
        val final=marketPresentation("NFL","TOTAL","OVER",45.5,47.0,"WON",20,27)
        assertFalse(final.provisional);assertEquals("Ganada",final.status)
    }
    @Test fun nbaMarketsHaveTheRightUnitsAndOverUnderRemainProvisional() {
        for((market,unit) in mapOf("PLAYER_POINTS" to "puntos","PLAYER_REBOUNDS" to "rebotes","PLAYER_ASSISTS" to "asistencias",
            "PLAYER_THREE_POINTERS_MADE" to "triples","PLAYER_POINTS_REBOUNDS_ASSISTS" to "PRA")) {
            val p=marketPresentation("NBA",market,"UNDER",24.5,18.0,"LIVE_WINNING",98,104)
            assertEquals(unit,p.unit);assertTrue(p.provisional);assertEquals(ProgressTone.GREEN,p.tone)
            assertEquals("18 / 24.5",p.value);assertTrue(p.caption.contains("por debajo"))
        }
        assertTrue(marketLabel("MONEYLINE","NBA").contains("prórroga"))
        val tie=marketPresentation("NBA","MONEYLINE","HOME",null,0.0,"LIVE_PUSH",104,104)
        assertEquals(ProgressTone.AMBER,tie.tone);assertEquals(tie.home!!.size,tie.away!!.size)
    }
    @Test fun nbaHandicapCentersCoverageAtZeroAndKeepsStaleValuesGray() {
        val event=EventSnapshot("NBA","TEST","id","",Team("h","Lakers"),Team("a","Celtics"),98,104,"LIVE","Q4","05:42","")
        val bet=BetView("bet","event",event,"SPREAD","HOME",6.5,null,null,null,null,null,null,"LIVE_WINNING",-6.0,null,"",false)
        val progress=bet.visualProgress()!!
        assertEquals(.5,progress.current,0.0);assertEquals(0.0,progress.target,0.0)
        assertEquals(.5f,progress.targetFraction,0f);assertTrue(progress.centered)
        assertEquals("Margen +0.5",progress.valueLabel)
        assertEquals(ProgressTone.MUTED,bet.visualProgress(offline=true)!!.tone)
        assertNull(bet.copy(market="MONEYLINE").visualProgress())
    }
}
