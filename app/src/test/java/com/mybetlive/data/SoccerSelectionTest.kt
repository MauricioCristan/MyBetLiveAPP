package com.mybetlive.data
import org.junit.Assert.*
import org.junit.Test
class SoccerSelectionTest {
    @Test fun drawHasItsOwnTitleAndGreenTargetWhenTheScoresAreEqual() {
        val event=EventSnapshot("SOCCER","test","1","2026-10-02T00:00:00Z",Team("1","A"),Team("2","B"),1,1,"FINAL","FT",null,"2026-10-02T00:00:00Z")
        val bet=CreateBet("id","MONEYLINE","DRAW",null,null,null,null,null,null)
        assertTrue(bet.title(event,null).contains("Empate"))
        val progress=betProgress("SOCCER","MONEYLINE","DRAW",0.0,null,"WON")!!
        assertEquals(ProgressTone.GREEN,progress.tone);assertEquals(0.5f,progress.targetFraction,0.001f)
    }
    @Test fun soccerMarginUsesGoalsAndItsSportIconInput() {
        val progress=betProgress("SOCCER","SPREAD","HOME",1.0,-0.5,"LIVE_WINNING")!!
        assertEquals("SOCCER",progress.sport);assertTrue(progress.caption.contains("goles"));assertEquals(-1.0,progress.start,0.001)
        assertEquals(ProgressTone.GREEN,betProgress("SOCCER","MONEYLINE","DRAW",0.0,null,"WON",true)!!.tone)
        assertEquals(ProgressTone.MUTED,betProgress("SOCCER","MONEYLINE","DRAW",0.0,null,"LIVE_WINNING",true)!!.tone)
    }
}
