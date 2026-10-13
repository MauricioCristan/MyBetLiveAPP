package com.mybetlive.application

import com.mybetlive.data.*
import org.junit.Test
import org.junit.Assert.*

class FloatingCompactPresentationTest {
    private fun bet(market:String="MONEYLINE",selection:String="DRAW",status:String="LIVE_WINNING",home:Int?=1,away:Int?=1,value:Double?=0.0,line:Double?=null)=
        BetView("id","event",EventSnapshot("SOCCER","fake","game","",Team("h","Home"),Team("a","Away"),home,away,"LIVE","2H","65",""),
            market,selection,line,null,null,null,null,null,null,status,value,null,"",false)
    @Test fun drawShowsBothScoresAndFavorableOutlineOnlyWhenTied() {
        val tie=bet().compactFloating();assertEquals("1 : 1",tie.score);assertEquals("Empate",tie.label);assertEquals(ProgressTone.GREEN,tie.tone);assertNull(tie.value)
        val losing=bet(home=2,away=1).compactFloating();assertEquals("2 : 1",losing.score);assertEquals(ProgressTone.RED,losing.tone)
        assertEquals(ProgressTone.AMBER,bet(selection="HOME",status="LIVE_PUSH").compactFloating().tone)
    }
    @Test fun totalsPropsAndHandicapKeepTheLineAndEvaluatedValue() {
        val total=bet("TOTAL","OVER",value=2.0,line=2.5).compactFloating();assertEquals("2 / 2.5",total.value);assertEquals("Over · goles",total.label)
        assertEquals(80,total.widthDp)
        val prop=bet("PLAYER_GOALS","UNDER",value=0.0,line=0.5).compactFloating();assertEquals("0 / 0.5",prop.value)
        assertEquals(80,prop.widthDp)
        val spread=bet("SPREAD","HOME",value=2.0,line=-1.5).compactFloating();assertEquals("Hándicap -1.5",spread.label);assertEquals("Margen +0.5",spread.value)
        assertEquals(80,spread.widthDp)
    }
    @Test fun footballMetricsKeepTheCompactSizeAndActualCount() {
        for(market in listOf("TOTAL_CORNERS","TOTAL_CARDS","TOTAL_SHOTS","TOTAL_SHOTS_ON_TARGET")) {
            val p=bet(market,"OVER",value=8.0,line=7.5).compactFloating()
            assertEquals("8 / 7.5",p.value);assertEquals(80,p.widthDp)
            assertFalse(p.label.contains("goles"))
        }
    }
    @Test fun finalAndOfflineDoNotReplaceTheScoreOrInventZero() {
        val final=bet(status="WON",home=2,away=2).compactFloating();assertEquals("2 : 2",final.score);assertEquals("Ganada",final.phase)
        val missing=bet(status="AWAITING_DATA",home=null,away=null,value=null).compactFloating();assertEquals("— : —",missing.score);assertEquals(ProgressTone.MUTED,missing.tone)
        val saved=bet().compactFloating(offline=true);assertEquals("1 : 1",saved.score);assertEquals("Dato guardado",saved.phase);assertEquals(ProgressTone.AMBER,saved.tone)
    }
    @Test fun hockeyUsesTheSelectedPeriodScore() {
        val row=bet().copy(event=bet().event.copy(sport="NHL",homeScore=3,awayScore=2,periodScores=mapOf("P1" to PeriodScore(1,1))),market="RESULT_3WAY",scope="P1")
        assertEquals("1 : 1",row.compactFloating().score)
        assertEquals(ProgressTone.GREEN,row.compactFloating().tone)
    }
}
