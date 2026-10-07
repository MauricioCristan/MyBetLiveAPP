package com.mybetlive.data
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AuthorizedCallsTest {
    @Test fun discardsAnOldAccountsResponseAfterSwitchOrLogout() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val url=server.url("/").toString();val current=AtomicReference<Session?>(Session(url,"one",Tokens("a","r")))
            val factory=TrackingApiFactory(session={current.get()});val calls=AuthorizedCalls({current.get()},{current.set(it)},factory::api)
            server.enqueue(MockResponse().setBody("[]").setBodyDelay(150,TimeUnit.MILLISECONDS))
            val result=async(Dispatchers.IO){calls.execute{it.sports()}}
            assertNotNull(server.takeRequest(2,TimeUnit.SECONDS))
            current.set(Session(url,"one",Tokens("new-account","other-refresh")))
            assertTrue(runCatching {result.await()}.exceptionOrNull() is CancellationException)
            assertEquals("new-account",current.get()!!.tokens.accessToken)
            val original=current.get()!!;assertTrue(original.sameOwner(original.copy(tokens=Tokens("rotation","rotation-r"))))
            assertFalse(original.sameOwner(null))
        }finally{server.shutdown()}
    }
    @Test fun refreshesAnExpiredTokenAndRetriesWithTheCurrentCredential() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val url=server.url("/").toString();var current:Session?=Session(url,"one",Tokens("old","refresh"))
            val factory=TrackingApiFactory(session={current});val calls=AuthorizedCalls({current},{current=it},factory::api)
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setBody("{\"accessToken\":\"renewed\",\"refreshToken\":\"rotated\"}"))
            server.enqueue(MockResponse().setBody("[]"))
            calls.execute{it.sports()}
            assertEquals("Bearer old",server.takeRequest().getHeader("Authorization"))
            val refresh=server.takeRequest();assertEquals("/api/v1/auth/refresh",refresh.path);assertNull(refresh.getHeader("Authorization"))
            assertEquals("Bearer renewed",server.takeRequest().getHeader("Authorization"))
        }finally{server.shutdown()}
    }
}
