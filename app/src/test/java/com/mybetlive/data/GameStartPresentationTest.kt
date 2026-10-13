package com.mybetlive.data
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
class GameStartPresentationTest {
    val start=Instant.parse("2026-10-10T18:00:00Z")
    val event=EventSnapshot("NFL","TEST","1",start.toString(),Team("a","A"),Team("b","B"),null,null,"SCHEDULED","NS",null,"")
    @Test fun onlyTheLastFifteenMinutesAreAboutToStart(){
        assertEquals("Programado",event.scheduledLabel(start.minusSeconds(901)))
        assertEquals("Por comenzar",event.scheduledLabel(start.minusSeconds(900)))
        assertEquals("Por comenzar",event.scheduledLabel(start.minusSeconds(1)))
        assertEquals("Esperando inicio",event.scheduledLabel(start))
        assertEquals("Esperando inicio",event.scheduledLabel(start.plusSeconds(3600)))
    }
    @Test fun timeZonesRepresentTheSameInstantAndInvalidDatesDoNotInventAnImmediateStart(){
        assertEquals("Por comenzar",event.copy(startsAt="2026-10-10T12:00:00-06:00").scheduledLabel(start.minusSeconds(60)))
        assertEquals("Programado",event.copy(startsAt="invalid").scheduledLabel(start))
    }
}
