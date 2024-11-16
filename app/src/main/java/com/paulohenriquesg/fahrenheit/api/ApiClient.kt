// ApiClient.kt
package com.paulohenriquesg.fahrenheit.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    fun create(baseUrl: String, token: String? = null): ApiService {
        val clientBuilder = OkHttpClient.Builder()

        // Add logging interceptor
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        clientBuilder.addInterceptor(logging)

        clientBuilder.addInterceptor { chain ->
            val requestBuilder: Request.Builder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")

            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        val client = clientBuilder.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }
}