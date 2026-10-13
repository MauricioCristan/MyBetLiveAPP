package com.mybetlive.data
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.google.gson.JsonParser

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class EmailAccountsContractTest {
    private lateinit var server:MockWebServer
    @Before fun start(){server=MockWebServer();server.start()}
    @After fun stop(){server.shutdown()}
    @Test fun publicEmailRoutesUseCorrectBodiesAndNeverSendStoredAccessToken()=runBlocking {
        repeat(5){server.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody("{\"success\":true,\"code\":\"OK\"}"))}
        val url=server.url("/").toString()
        val session=Session(url,"other-user",Tokens("expired-access-token","refresh-token"))
        val api=TrackingApiFactory(session={session}).api(url)
        api.registerEmail(RegisterEmail("mau","user@example.test","  password123  "))
        api.forgotPassword(EmailAddress("user@example.test"));api.resendVerification(EmailAddress("user@example.test"))
        api.verifyEmail(EmailToken("a".repeat(64)));api.resetPassword(NewPassword("b".repeat(64),"  newpassword123  "))
        val requests=List(5){server.takeRequest()}
        assertEquals(listOf("/api/v1/auth/register","/api/v1/auth/password/forgot","/api/v1/auth/email/resend","/api/v1/auth/email/verify","/api/v1/auth/password/reset"),requests.map{it.path})
        assertTrue(requests.all{it.getHeader("Authorization")==null})
        assertEquals("  password123  ",JsonParser.parseString(requests.first().body.readUtf8()).asJsonObject["password"].asString)
        assertEquals("  newpassword123  ",JsonParser.parseString(requests.last().body.readUtf8()).asJsonObject["newPassword"].asString)
    }
}
