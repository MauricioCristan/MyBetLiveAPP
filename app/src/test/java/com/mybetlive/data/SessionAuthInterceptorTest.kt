package com.mybetlive.data

import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test

class SessionAuthInterceptorTest {
    @Test fun expiredAccessCanBeRenewedAndProtectedRequestsUseTheNewToken() {
        val received = mutableListOf<Pair<String, String?>>()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!
                val header = request.getHeader("Authorization")
                received.add(path to header)
                val status = if (path == "/api/v1/auth/refresh") {
                    if (header == null) 200 else 401
                } else if (header == "Bearer fresh") 200 else 401
                return MockResponse().setResponseCode(status)
            }
        }
        server.start()
        try {
            val base = server.url("/").toString().trimEnd('/')
            var token = "expired"
            val client = OkHttpClient.Builder().addInterceptor(SessionAuthInterceptor { token }).build()
            fun call(path: String, post: Boolean = false) = client.newCall(Request.Builder().url(base + path).apply {
                if (post) post("{}".toRequestBody())
            }.build()).execute().use { it.code }
            assertEquals(401, call("/api/v1/tracking/sports"))
            assertEquals(200, call("/api/v1/auth/refresh", true))
            token = "fresh"
            assertEquals(200, call("/api/v1/tracking/sports"))
            assertEquals(listOf("/api/v1/tracking/sports" to "Bearer expired", "/api/v1/auth/refresh" to null,
                "/api/v1/tracking/sports" to "Bearer fresh"), received)
            for (path in listOf("/api/v1/auth/login", "/api/v1/auth/google/challenge", "/api/v1/auth/google", "/api/v1/auth/logout")) {
                call(path, true)
                assertNull(received.last().second)
            }
        } finally { server.shutdown() }
    }
}
