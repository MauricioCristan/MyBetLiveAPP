package com.mybetlive.application
import com.mybetlive.data.LineRule
import org.junit.Assert.*
import org.junit.Test

class PropLineValidationTest {
    private val goals=LineRule(0.0,10.5,0.5,"goles",0.5)
    @Test fun invalidGoalsShowTheAllowedRangeWhileHalfAndWholeLinesRemainValid() {
        assertTrue(propLineError("223",goals)!!.contains("10.5"))
        assertNull(propLineError("0,5",goals));assertNull(propLineError("1",goals));assertNull(propLineError("0",goals));assertNull(propLineError("10.5",goals))
        assertNotNull(propLineError("11",goals));assertNotNull(propLineError("-1",goals));assertNotNull(propLineError("0.25",goals))
    }
    @Test fun limitsComeFromTheServerAndDoNotTreatYardsLikeGoals() {
        assertNull(propLineError("223",LineRule(0.0,800.5,0.5,"yardas de pase",267.5)))
        assertNotNull(propLineError("223",goals))
        assertNotNull(propLineError("1",null));assertNotNull(propLineError("NaN",goals));assertNotNull(propLineError("Infinity",goals))
        assertNotNull(propLineError("1e-2147483647",goals));assertNull(propLineError(".5",goals))
    }
}
