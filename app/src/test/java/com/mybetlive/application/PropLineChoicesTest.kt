package com.mybetlive.application
import com.mybetlive.data.LineRule
import org.junit.Assert.*
import org.junit.Test

class PropLineChoicesTest {
    @Test fun choicesAreExactlyServerStepsWithNoQuarterOrOutOfRangeValues() {
        val rule=LineRule(0.0,10.5,.5,"goles",.5)
        val halves=propLineChoices(rule,true);val whole=propLineChoices(rule,false)
        assertEquals("0.5",halves.first());assertEquals("10.5",halves.last())
        assertEquals("0",whole.first());assertEquals("10",whole.last())
        assertFalse("223" in halves);assertFalse("2.25" in halves)
        (halves+whole).forEach {assertNull(propLineError(it,rule))}
        assertTrue("267.5" in propLineChoices(LineRule(0.0,800.5,.5,"yardas",267.5),true))
        val handicap=LineRule(-20.5,20.5,.5,"hándicap",-1.5)
        val signed=propLineChoices(handicap,true)
        assertTrue("-1.5" in signed);assertEquals("-20.5",signed.first());assertEquals("20.5",signed.last())
        signed.forEach {assertNull(propLineError(it,handicap))}
    }
    @Test fun missingInvalidOrUnboundedMetadataCannotProduceChoices() {
        assertTrue(propLineChoices(null,true).isEmpty())
        assertTrue(propLineChoices(LineRule(0.0,10.0,0.0,"goles",.5),true).isEmpty())
        assertTrue(propLineChoices(LineRule(0.0,Double.NaN,.5,"goles",.5),true).isEmpty())
        assertTrue(propLineChoices(LineRule(0.0,1e20,.5,"goles",.5),true).isEmpty())
    }
    @Test fun criteriaDistinguishOverUnderAndPush() {
        assertTrue(propCondition(".5","OVER")!!.contains("1 o más"))
        assertTrue(propCondition(".5","UNDER")!!.contains("0 o menos"))
        assertTrue(propCondition("1","OVER")!!.contains("Con 1 hay push"))
        assertTrue(propCondition("267.5","OVER")!!.contains("268 o más"))
    }
}
