package com.mybetlive.data

import org.junit.Assert.*
import org.junit.Test

class SelectionTest {
    private val event = EventSnapshot("NFL", "API_SPORTS_NFL", "21561", "2026-10-02T00:15:00Z",
        Team("9", "Cleveland Browns"), Team("22", "Pittsburgh Steelers"), 27, 24,
        "FINAL", "FT", null, "2026-10-02T01:00:00Z")

    @Test fun receivingPlayerCannotBeSelectedForPassingStatistics() {
        val receiver = Player("1645", "DK Metcalf", "22", listOf("PLAYER_RECEIVING_YARDS", "PLAYER_RECEPTIONS"))
        assertTrue(receiver.supports("PLAYER_RECEPTIONS"))
        assertFalse(receiver.supports("PLAYER_PASSING_YARDS"))
    }
    @Test fun unknownRosterCapabilitiesStayCompatibleWithOlderBackends() {
        assertTrue(Player("609", "Deshaun Watson", "9").supports("PLAYER_PASSING_YARDS"))
    }
    @Test fun teamTotalPreviewIncludesTheSelectedTeam() {
        val draft = CreateBet("event", "TEAM_TOTAL", "UNDER", 24.5, "AWAY", null, null, null, null)
        assertEquals("Pittsburgh Steelers · Total del equipo · Under · 24.5", draft.title(event, null))
    }
    @Test fun propPreviewKeepsPlayerMetricAndLineTogether() {
        val draft = CreateBet("event", "PLAYER_RECEPTIONS", "OVER", 4.5, null, "1645", null, null, null)
        assertEquals("DK Metcalf · Recepciones · Over · 4.5", draft.title(event, "DK Metcalf"))
    }
    @Test fun freshnessFollowsTheConfiguredBackendInterval() {
        val bet = BetView("bet", "event", event.copy(status = "LIVE"), "TOTAL", "OVER", 40.5,
            null, null, null, null, null, null, "LIVE_WINNING", 51.0, 0.0, "2026-10-02T01:00:00Z", false)
        assertFalse(bet.isStale())
        assertTrue(bet.copy(stale = true).isStale())
    }
}
