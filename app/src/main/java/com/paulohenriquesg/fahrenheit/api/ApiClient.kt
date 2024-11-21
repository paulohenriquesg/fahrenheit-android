package com.paulohenriquesg.fahrenheit.api

import android.content.Context
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var apiService: ApiService? = null
    private var host: String? = null
    private var token: String? = null

    fun initialize(context: Context) {
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()
        host = userPreferences.host
        token = userPreferences.token

        if (host != null && token != null) {
            apiService = create(host!!, token!!)
        }
    }

    fun getApiService(): ApiService? {
        return apiService
    }

    fun getApiServiceForLogin(host: String): ApiService {
        return create(host)
    }

    fun generateFullUrl(path: String): String? {
        return if (host != null && token != null) {
            "$host$path?token=$token"
        } else {
            null
        }
    }

    private fun create(baseUrl: String, token: String? = null): ApiService {
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