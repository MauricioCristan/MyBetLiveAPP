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
            ,"/api/v1/auth/register","/api/v1/auth/password/forgot","/api/v1/auth/password/reset",
            "/api/v1/auth/email/verify","/api/v1/auth/email/resend"
        )
        val builder = request.newBuilder().removeHeader("Authorization")
        builder.header("Accept-Language",com.mybetlive.i18n.AppStrings.language())
        if (!publicAuth) accessToken()?.let { builder.header("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}
