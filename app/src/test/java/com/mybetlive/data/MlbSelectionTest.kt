package com.mybetlive.data
import org.junit.Assert.*
import org.junit.Test
class MlbSelectionTest {
    @Test fun runLineTitleUsesRunsAndHasNoDraw() {
        val event=EventSnapshot("MLB","test","1","2026-10-02T00:00:00Z",Team("3","Atlanta"),Team("27","Philadelphia"),6,2,"FINAL","FT",null,"2026-10-02T00:00:00Z")
        assertEquals("Atlanta · Run Line · juego completo · -1.5",CreateBet("id","SPREAD","HOME",-1.5,null,null,null,null,null).title(event,null))
        assertEquals("Total de carreras",marketLabel("TOTAL","MLB"))
    }
    @Test fun baseballBallUsesRunMarginAndExtraInningsCaption() {
        val progress=betProgress("MLB","SPREAD","HOME",2.0,-1.5,"LIVE_WINNING")!!
        assertEquals("MLB",progress.sport);assertTrue(progress.caption.contains("carreras"));assertTrue(progress.caption.contains("entradas extra"))
        assertEquals(ProgressTone.GREEN,progress.tone);assertEquals(-2.0,progress.start,0.001)
        assertNull(betProgress("MLB","MONEYLINE","DRAW",0.0,null,"WON"))
    }
}
