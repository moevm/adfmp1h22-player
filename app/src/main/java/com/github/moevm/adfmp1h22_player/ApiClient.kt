package com.github.moevm.adfmp1h22_player

import android.content.Context
import android.os.Build
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


class APIClient {
    private var retrofit: Retrofit? = null

    fun getClient(): Retrofit? {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val userAgent: String =
            "Radio Player " +
                    "${BuildConfig.VERSION_NAME} " +
                    "(${BuildConfig.APPLICATION_ID}; " +
                    "build:${BuildConfig.VERSION_CODE} " +
                    "Android SDK ${Build.VERSION.SDK_INT}) "

        val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val request: Request = chain.request()
                val newReq: Request = request.newBuilder()
                    .addHeader("User-Agent", userAgent)
                    .build()
                return chain.proceed(newReq)
            }
        }).build()
//            .addNetworkInterceptor(httpLoggingInterceptor)
        retrofit = Retrofit.Builder()
            .baseUrl("http://de1.api.radio-browser.info/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        Log.d("TAG", "ApiClient")
        return retrofit
    }
}