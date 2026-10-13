package com.mybetlive.data
import org.junit.Test
import org.junit.Assert.*
class HockeyMarketsTest {
    private val e=EventSnapshot("NHL","API_SPORTS_NHL","1","",Team("1","A"),Team("2","B"),3,2,"FINAL","AOT",null,"",periodScores=mapOf("P1" to PeriodScore(0,1),"P2" to PeriodScore(2,0),"P3" to PeriodScore(0,1)))
    @Test fun scoreScopesSeparateRegulationAndOvertimeAndPreserveMissing() {
        assertEquals(PeriodScore(2,2),e.hockeyScore("REGULATION"));assertEquals(PeriodScore(3,2),e.hockeyScore("MATCH"));assertEquals(PeriodScore(0,1),e.hockeyScore("P1"))
        assertNull(e.copy(periodScores=emptyMap()).hockeyScore("REGULATION"))
        assertEquals(listOf("MATCH"),hockeyScopes("MONEYLINE"));assertFalse(hockeyScopes("CORRECT_SCORE").contains("MATCH"))
    }
    @Test fun titlesAndOutcomePresentationKeepTheScopeAndTargetScore() {
        val draft=CreateBet("event","CORRECT_SCORE","YES",null,null,null,null,null,null,"P1","0-1")
        assertTrue(draft.title(e,null).contains("0-1"));assertTrue(draft.title(e,null).contains("1.er período"))
        val p=marketPresentation("NHL","CORRECT_SCORE","YES",null,null,"WON",0,1,false,"P1","0-1")
        assertEquals(MarketVisual.OUTCOME,p.visual);assertEquals("0 : 1",p.value);assertEquals(ProgressTone.GREEN,p.tone);assertFalse(p.provisional)
        val pending=marketPresentation("NHL","OVERTIME","YES",null,null,"PENDING",2,2,false)
        assertEquals("—",pending.value)
    }
    @Test fun cachedTrackersWithoutScopeRemainReadable() {
        val bet=BetView("bet","event",e.copy(sport="MLB"),"MONEYLINE","HOME",null,null,null,null,null,null,null,"WON",1.0,null,"",false)
        val gson=com.google.gson.Gson()
        val saved=gson.toJsonTree(bet).asJsonObject.apply { remove("scope");remove("targetScore") }
        val restored=gson.fromJson(saved,BetView::class.java)
        assertTrue(restored.title().contains("Ganador"));assertEquals(ProgressTone.GREEN,restored.presentation().tone)
    }
}
