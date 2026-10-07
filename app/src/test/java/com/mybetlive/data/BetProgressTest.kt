package com.mybetlive.data

import org.junit.Assert.*
import org.junit.Test

class BetProgressTest {
    private fun over(value: Double, line: Double = 45.0, status: String = "LIVE_LOSING", stale: Boolean = false) =
        betProgress("NFL", "PLAYER_RUSHING_YARDS", "OVER", value, line, status, stale)!!

    @Test fun partialOverUsesActualLineAndRedPosition() {
        val progress = over(15.0)
        assertEquals(1f / 3, progress.fraction, .0001f)
        assertEquals(45.0, progress.target, 0.0)
        assertEquals(ProgressTone.RED, progress.tone)
        assertTrue(progress.caption.contains("31"))
    }
    @Test fun integerEqualityIsAmberRatherThanWinning() {
        val progress = over(45.0, status = "LIVE_PUSH")
        assertEquals(ProgressTone.AMBER, progress.tone)
        assertTrue(progress.caption.contains("necesita superar"))
        assertEquals(ProgressTone.AMBER, over(45.0, status = "PUSH").tone)
    }
    @Test fun crossingHalfPointLineIsGreenAndExpandsTheScale() {
        val progress = over(45.0, line = 44.5, status = "THRESHOLD_REACHED")
        assertEquals(ProgressTone.GREEN, progress.tone)
        assertEquals(45.0, progress.end, 0.0)
        assertTrue(progress.targetFraction < progress.fraction)
        assertTrue(progress.caption.contains("provisional"))
    }
    @Test fun underCanGoInFavorButStaysProvisionalBeforeFinal() {
        val live = betProgress("NFL", "TOTAL", "UNDER", 15.0, 45.0, "LIVE_WINNING")!!
        assertEquals(ProgressTone.GREEN, live.tone)
        assertTrue(live.caption.contains("Debe terminar por debajo"))
        val lost = betProgress("NFL", "TOTAL", "UNDER", 46.0, 45.0, "LOST")!!
        assertEquals(ProgressTone.RED, lost.tone)
    }
    @Test fun finalResultsStillHaveAVisibleTrack() {
        assertEquals(ProgressTone.GREEN, over(46.0, status = "WON").tone)
        assertTrue(over(46.0, status = "WON").caption.contains("finalizado"))
        assertEquals(ProgressTone.RED, over(15.0, status = "LOST").tone)
    }
    @Test fun staleDataCannotKeepAGreenWinningIndicator() {
        val progress = over(46.0, status = "THRESHOLD_REACHED", stale = true)
        assertEquals(ProgressTone.MUTED, progress.tone)
        assertTrue(progress.caption.contains("pendiente de actualizar"))
    }
    @Test fun zeroLineAndNegativeYardsHaveFinitePositions() {
        assertEquals(0f, over(0.0, line = 0.0, status = "LIVE_PUSH").fraction, 0f)
        val negative = over(-5.0)
        assertEquals(-5.0, negative.start, 0.0)
        assertEquals(0f, negative.fraction, 0f)
    }
    @Test fun spreadShowsScoreMarginAgainstTheHandicap() {
        val progress = betProgress("NFL", "SPREAD", "HOME", 3.0, -3.5, "LIVE_LOSING")!!
        assertEquals(3.5, progress.target, 0.0)
        assertTrue(progress.fraction < progress.targetFraction)
        assertEquals(ProgressTone.RED, progress.tone)
    }
    @Test fun moneylineShowsMarginAroundTheTie() {
        val progress = betProgress("NFL", "MONEYLINE", "AWAY", -3.0, null, "LIVE_LOSING")!!
        assertEquals(0.0, progress.target, 0.0)
        assertEquals(.5f, progress.targetFraction, 0f)
        assertTrue(progress.fraction < .5f)
    }
    @Test fun missingCanceledOrInvalidDataHasNoInventedProgress() {
        assertNull(betProgress("NFL", "TOTAL", "OVER", null, 45.0, "AWAITING_DATA"))
        assertNull(betProgress("NFL", "TOTAL", "OVER", 15.0, 45.0, "VOID"))
        assertNull(betProgress("NFL", "TOTAL", "OVER", Double.NaN, 45.0, "LIVE_LOSING"))
        assertNull(betProgress("NFL", "TOTAL", "OVER", 15.0, -1.0, "LIVE_LOSING"))
        assertNull(betProgress("NFL", "", "", 15.0, 45.0, "LIVE_LOSING"))
    }
}
