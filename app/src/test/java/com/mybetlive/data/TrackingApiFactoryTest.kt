package com.mybetlive.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class TrackingApiFactoryTest {
    @Test fun reusesApiAndReadsNewTokensWithoutLeakingAcrossServers() = runBlocking {
        val first = MockWebServer(); val second = MockWebServer()
        first.start(); second.start()
        try {
            val a = first.url("/").toString(); val b = second.url("/").toString()
            var current: Session? = Session(a, "one", Tokens("old", "refresh"))
            val factory = TrackingApiFactory(session = { current })
            val api = factory.api(a)
            assertSame(api, factory.api(a))
            first.enqueue(MockResponse().setBody("[]")); api.sports()
            assertEquals("Bearer old", first.takeRequest().getHeader("Authorization"))
            current = current!!.copy(tokens = Tokens("new", "refresh2"))
            first.enqueue(MockResponse().setBody("[]")); api.sports()
            assertEquals("Bearer new", first.takeRequest().getHeader("Authorization"))
            current = Session(b, "two", Tokens("other", "other-refresh"))
            first.enqueue(MockResponse().setBody("[]")); api.sports()
            assertNull(first.takeRequest().getHeader("Authorization"))
            second.enqueue(MockResponse().setBody("[]")); factory.api(b).sports()
            assertEquals("Bearer other", second.takeRequest().getHeader("Authorization"))
            current = null
            second.enqueue(MockResponse().setBody("[]")); factory.api(b).sports()
            assertNull(second.takeRequest().getHeader("Authorization"))
        } finally { first.shutdown(); second.shutdown() }
    }
}
