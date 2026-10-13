package com.mybetlive.ui

import com.mybetlive.data.TrackingApiFactory
import com.mybetlive.data.CalendarInfo
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone
import java.time.LocalDate

class TimezoneFormattingTest {
    @Test fun todayRollsOverUsingTheServerCalendarForTheUsersZone() {
        val current=AppState(date=LocalDate.parse("2026-10-07"))
        val next=current.withCalendar(CalendarInfo("Asia/Tokyo","2026-10-08","2026-10-07T15:01:00Z"))
        assertEquals(LocalDate.parse("2026-10-08"),next.date)
        assertEquals("Asia/Tokyo",next.timeZone)
    }
    @Test fun aDateChosenExplicitlyDoesNotJumpAtMidnightOrWhenTravelling() {
        val selected=AppState(date=LocalDate.parse("2026-10-05"),followingToday=false)
        val next=selected.withCalendar(CalendarInfo("Asia/Tokyo","2026-10-08","2026-10-07T15:01:00Z"))
        assertEquals(LocalDate.parse("2026-10-05"),next.date)
        assertEquals(LocalDate.parse("2026-10-08"),next.today)
    }
    @Test fun serverOffsetsKeepCorrectHoursEvenWhenTheDevicesTimezoneRulesAreOutdated() {
        val previous=TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"))
            assertTrue(kickoff("2026-10-08T01:00:00Z","2026-10-07T19:00:00-06:00").endsWith("19:00"))
            assertTrue(updatedTime("2026-10-08T01:00:00Z","2026-10-07T19:00:00-06:00").endsWith("19:00"))
        } finally {TimeZone.setDefault(previous)}
    }
    @Test fun requestsCarryTheIanaZoneAndPreserveLocalizedTimes()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            val api=TrackingApiFactory(session={null}).api(server.url("/").toString())
            server.enqueue(MockResponse().setBody("""{"timeZone":"America/Monterrey","date":"2026-10-07","serverTime":"2026-10-08T01:00:00Z"}"""))
            assertEquals("2026-10-07",api.calendar("America/Monterrey").date)
            assertEquals("America/Monterrey",server.takeRequest().requestUrl!!.queryParameter("timezone"))
            server.enqueue(MockResponse().setBody("[]"));api.events("MLB","2026-10-07","America/Monterrey")
            val request=server.takeRequest().requestUrl!!
            assertEquals("2026-10-07",request.queryParameter("date"))
            assertEquals("America/Monterrey",request.queryParameter("timezone"))
        } finally {server.shutdown()}
    }
}
