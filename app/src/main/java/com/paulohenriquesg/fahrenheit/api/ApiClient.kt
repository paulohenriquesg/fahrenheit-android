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

        apiService = create(hostValue, sessionManager)
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
        sessionManager: SessionManager? = null
    ): ApiService {
        val client = if (sessionManager != null) {
            buildAuthenticatedClient(sessionManager, refreshVia(baseUrl))
        } else {
            // Pre-login: there is no session to authenticate with yet.
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor())
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .build()
                    )
                }
                .build()
        }
        return buildRetrofit(baseUrl, client).create(ApiService::class.java)
    }

    /** BASIC level on purpose: full bodies OOM on large library responses. */
    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BASIC)
    }

    /**
     * The OkHttp stack for authenticated calls: bearer injection plus 401 refresh.
     *
     * Exposed separately so the interceptor, the authenticator and the retry can
     * be exercised together against a real socket - the wiring is what breaks,
     * not the pieces.
     */
    internal fun buildAuthenticatedClient(
        sessionManager: SessionManager,
        refreshSession: (String) -> AuthSession
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor())
        .addInterceptor { chain ->
            val requestBuilder: Request.Builder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")

            // Resolved per request, not captured once: a refresh replaces the
            // token mid-session and every later request must carry the new one.
            sessionManager.accessToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            chain.proceed(requestBuilder.build())
        }
        .authenticator(TokenRefreshAuthenticator(sessionManager, refreshSession))
        .build()

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
