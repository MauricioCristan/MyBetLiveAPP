package com.mybetlive.data
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AuthorizedCallsTest {
    @Test fun renewsBeforeSendingAnExpiredJwt() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val url=server.url("/").toString();val payload=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"exp\":1}".toByteArray())
            var current:Session?=Session(url,"one",Tokens("header.$payload.signature","refresh"))
            val factory=TrackingApiFactory(session={current});val calls=AuthorizedCalls({current},{current=it},factory::api)
            server.enqueue(MockResponse().setBody("{\"accessToken\":\"new\",\"refreshToken\":\"rotated\"}"));server.enqueue(MockResponse().setBody("[]"))
            calls.execute{it.sports()}
            assertEquals("/api/v1/auth/refresh",server.takeRequest().path);assertEquals("Bearer new",server.takeRequest().getHeader("Authorization"))
            assertFalse(accessTokenNeedsRenewal("malformed"))
        }finally{server.shutdown()}
    }
    @Test fun simultaneousFailuresRotateOnlyOnce() = runBlocking {
        val server=MockWebServer();val rotations=java.util.concurrent.atomic.AtomicInteger();val oldCalls=java.util.concurrent.CountDownLatch(2)
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            if(request.path=="/api/v1/auth/refresh") {rotations.incrementAndGet();return MockResponse().setBody("{\"accessToken\":\"new\",\"refreshToken\":\"rotated\"}")}
            if(request.getHeader("Authorization")=="Bearer old"){oldCalls.countDown();oldCalls.await(3,TimeUnit.SECONDS);return MockResponse().setResponseCode(401)}
            return MockResponse().setBody("[]")
        }};server.start()
        try {
            val url=server.url("/").toString();val current=AtomicReference<Session?>(Session(url,"one",Tokens("old","refresh")))
            val factory=TrackingApiFactory(session={current.get()});val calls=AuthorizedCalls({current.get()},{current.set(it)},factory::api)
            coroutineScope{List(2){async(Dispatchers.IO){calls.execute{it.sports()}}}.awaitAll()}
            assertEquals(1,rotations.get());assertEquals("rotated",current.get()!!.tokens.refreshToken)
        }finally{server.shutdown()}
    }
    @Test fun rejectedRefreshEndsSessionButTemporaryFailurePreservesIt() = runBlocking {
        for(status in listOf(401,403,503)) {
            val server=MockWebServer();server.start()
            try {
                val url=server.url("/").toString();var current:Session?=Session(url,"one",Tokens("old","refresh"))
                val factory=TrackingApiFactory(session={current});val calls=AuthorizedCalls({current},{current=it},factory::api){current=null}
                server.enqueue(MockResponse().setResponseCode(401));server.enqueue(MockResponse().setResponseCode(status))
                val failure=runCatching{calls.execute{it.sports()}}.exceptionOrNull()
                if(status==503){assertTrue(failure is retrofit2.HttpException);assertNotNull(current)}
                else {assertTrue(failure is com.mybetlive.application.SessionExpiredException);assertNull(current)}
                assertEquals(2,server.requestCount)
            }finally{server.shutdown()}
        }
    }
    @Test fun aSecondUnauthorizedResponseDoesNotLoop() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val url=server.url("/").toString();var current:Session?=Session(url,"one",Tokens("old","refresh"))
            val factory=TrackingApiFactory(session={current});val calls=AuthorizedCalls({current},{current=it},factory::api){current=null}
            server.enqueue(MockResponse().setResponseCode(401));server.enqueue(MockResponse().setBody("{\"accessToken\":\"new\",\"refreshToken\":\"rotated\"}"));server.enqueue(MockResponse().setResponseCode(401))
            assertTrue(runCatching{calls.execute{it.sports()}}.exceptionOrNull() is com.mybetlive.application.SessionExpiredException)
            assertEquals(3,server.requestCount);assertNull(current)
        }finally{server.shutdown()}
    }
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
