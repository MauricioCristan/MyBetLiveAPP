package com.mybetlive.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** APIs share the transport pool; credentials are read when each request is sent. */
internal class TrackingApiFactory(
    private val session: () -> Session?,
    private val transport: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).build()
) {
    private val endpoints = ConcurrentHashMap<String, Any>()

    fun api(baseUrl: String): TrackingApi = client(baseUrl,TrackingApi::class.java)
    fun <T:Any> client(baseUrl:String,type:Class<T>):T = requireNotNull(type.cast(endpoints.computeIfAbsent(baseUrl+"|"+type.name) {
        val url=baseUrl
        val client = transport.newBuilder().addInterceptor(SessionAuthInterceptor {
            session()?.takeIf { it.baseUrl == url }?.tokens?.accessToken
        }).build()
        Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create())
            .build().create(type)
    }))
}
