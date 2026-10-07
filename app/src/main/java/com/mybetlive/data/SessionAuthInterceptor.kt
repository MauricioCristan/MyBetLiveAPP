package com.mybetlive.data

import okhttp3.Interceptor
import okhttp3.Response

/** Public authentication endpoints must remain reachable even when the stored access token expires. */
internal class SessionAuthInterceptor(private val accessToken: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val publicAuth = request.url.encodedPath in setOf(
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
            "/api/v1/auth/google", "/api/v1/auth/google/challenge"
        )
        val builder = request.newBuilder().removeHeader("Authorization")
        if (!publicAuth) accessToken()?.let { builder.header("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}
