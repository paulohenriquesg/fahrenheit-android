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
import java.io.IOException

object ApiClient {
    private var apiService: ApiService? = null
    private var host: String? = null
    private var token: String? = null
    private var sessionManager: SessionManager? = null

    fun initialize(context: Context) {
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()
        host = userPreferences.host
        token = userPreferences.token
        sessionManager = SessionManager(sharedPreferencesHandler)

        val hostValue = host
        val tokenValue = token

        if (hostValue.isNullOrEmpty() || tokenValue.isNullOrEmpty()) {
            // Missing credentials, navigate to login
            sharedPreferencesHandler.clearPreferences()
            navigateToLogin(context)
            return
        }

        if (!hostValue.startsWith("http://") && !hostValue.startsWith("https://")) {
            // Invalid host format, clear and navigate to login
            sharedPreferencesHandler.clearPreferences()
            navigateToLogin(context)
            return
        }

        apiService = create(hostValue, tokenValue, sessionManager)
    }

    private fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun getApiService(): ApiService? {
        return apiService
    }

    fun getToken(): String? {
        // Read through the session manager so a refreshed token is picked up.
        return sessionManager?.accessToken() ?: token
    }

    fun getApiServiceForLogin(host: String): ApiService {
        return create(host)
    }

    /** Auth endpoints for a host the user is not signed in to yet. */
    fun createAuthApi(host: String): AuthApi =
        buildRetrofit(host, OkHttpClient.Builder().build()).create(AuthApi::class.java)

    fun generateFullUrl(path: String): String? {
        return host?.let { "$it$path" }
    }

    private fun create(
        baseUrl: String,
        token: String? = null,
        sessionManager: SessionManager? = null
    ): ApiService {
        val clientBuilder = OkHttpClient.Builder()

        // Add logging interceptor (BASIC level to prevent OOM with large responses)
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        clientBuilder.addInterceptor(logging)

        clientBuilder.addInterceptor { chain ->
            val requestBuilder: Request.Builder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")

            // Resolved per request, not captured once: a refresh replaces the token
            // mid-session and every later request must carry the new one.
            val currentToken = sessionManager?.accessToken() ?: token
            if (currentToken != null) {
                requestBuilder.addHeader("Authorization", "Bearer $currentToken")
            }

            chain.proceed(requestBuilder.build())
        }

        if (sessionManager != null) {
            clientBuilder.authenticator(
                TokenRefreshAuthenticator(sessionManager, refreshVia(baseUrl))
            )
        }

        return buildRetrofit(baseUrl, clientBuilder.build()).create(ApiService::class.java)
    }

    /**
     * Refreshes on a bare client: it must not carry the Authorization header that
     * just 401'd, nor recurse back into the authenticator.
     */
    private fun refreshVia(baseUrl: String): (String) -> AuthSession {
        val refreshApi = buildRetrofit(baseUrl, OkHttpClient.Builder().build())
            .create(AuthRefreshApi::class.java)
        return { refreshToken ->
            val response = refreshApi.refresh(refreshToken).execute()
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                throw IOException("Token refresh failed with HTTP ${response.code()}")
            }
            body.toAuthSession()
        }
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        // Custom Gson with ShelfDeserializer and ServerSettingsDeserializer
        val gson = GsonBuilder()
            .registerTypeAdapter(Shelf::class.java, ShelfDeserializer())
            .registerTypeAdapter(ServerSettings::class.java, ServerSettingsDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
