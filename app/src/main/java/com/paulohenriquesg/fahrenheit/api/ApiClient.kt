package com.paulohenriquesg.fahrenheit.api

import android.content.Context
import android.content.Intent
import com.google.gson.GsonBuilder
import com.paulohenriquesg.fahrenheit.login.LoginActivity
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

        val hostValue = host
        val tokenValue = token

        if (hostValue == null || tokenValue == null) {
            // Missing credentials, navigate to login
            sharedPreferencesHandler.clearPreferences()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            return
        }

        if (!hostValue.startsWith("http://") && !hostValue.startsWith("https://")) {
            // Invalid host format, clear and navigate to login
            sharedPreferencesHandler.clearPreferences()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            return
        }

        apiService = create(hostValue, tokenValue)
    }

    fun getApiService(): ApiService? {
        return apiService
    }

    fun getToken(): String? {
        return token
    }

    fun getApiServiceForLogin(host: String): ApiService {
        return create(host)
    }

    fun generateFullUrl(path: String): String? {
        return host?.let { "$it$path" }
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

        // Create custom Gson with ShelfDeserializer and ServerSettingsDeserializer
        val gson = GsonBuilder()
            .registerTypeAdapter(Shelf::class.java, ShelfDeserializer())
            .registerTypeAdapter(ServerSettings::class.java, ServerSettingsDeserializer())
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return retrofit.create(ApiService::class.java)
    }
}