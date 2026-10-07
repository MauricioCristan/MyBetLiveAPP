package com.mybetlive

import com.google.gson.Gson
import com.mybetlive.data.Team
import org.junit.Assert.*
import org.junit.Test

class TeamModelTest {
    @Test fun oldCachedTeamsWithoutALogoStillDeserialize() {
        val team = Gson().fromJson("""{"id":"9","name":"Cleveland Browns"}""", Team::class.java)
        assertEquals("Cleveland Browns", team.name)
        assertNull(team.logoUrl)
    }
    @Test fun logosSurviveTheSameJsonRoundTripUsedByTheSessionCache() {
        val team = Team("9", "Cleveland Browns", "https://media.api-sports.io/american-football/teams/9.png")
        assertEquals(team, Gson().fromJson(Gson().toJson(team), Team::class.java))
    }
}
