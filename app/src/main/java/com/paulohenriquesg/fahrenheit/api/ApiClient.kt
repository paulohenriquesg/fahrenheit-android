package com.paulohenriquesg.fahrenheit.api

import android.content.Context
import com.google.gson.GsonBuilder
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import com.paulohenriquesg.fahrenheit.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ApiClient {
    private var apiService: ApiService? = null
    private var host: String? = null
    private var token: String? = null
    private var sessionManager: SessionManager? = null
    private var libraryApi: LibraryApi? = null
    private var browseApi: BrowseApi? = null

    /**
     * Loads stored credentials and reports whether they are usable.
     *
     * Deliberately does not navigate. This runs from Application.onCreate, so
     * starting an Activity here races the launcher Activity - and a network
     * client choosing what the user sees is what made this class untestable.
     */
    fun initialize(context: Context): SessionState {
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()
        val hostValue = userPreferences.host
        val tokenValue = userPreferences.token

        val usable = hostValue.isNotEmpty() &&
            tokenValue.isNotEmpty() &&
            (hostValue.startsWith("http://") || hostValue.startsWith("https://"))

        if (!usable) {
            forget()
            sharedPreferencesHandler.clearPreferences()
            return SessionState.NeedsLogin
        }

        host = hostValue
        token = tokenValue
        sessionManager = SessionManager(SharedPreferencesTokenStore(sharedPreferencesHandler))
        apiService = create(hostValue, sessionManager)
        libraryApi = buildRetrofit(
            hostValue,
            buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
        ).create(LibraryApi::class.java)
        browseApi = buildRetrofit(
            hostValue,
            buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
        ).create(BrowseApi::class.java)
        return SessionState.Ready
    }

    /**
     * Drops the signed-in session. Clearing stored credentials is not enough
     * on its own: the client built from them lives here, and kept working with
     * the old token for as long as the process did.
     */
    fun clearSession() = forget()

    private fun forget() {
        apiService = null
        libraryApi = null
        browseApi = null
        host = null
        token = null
        sessionManager = null
    }

    fun getApiService(): ApiService? {
        return apiService
    }

    /** Library endpoints as suspend calls; null until a session is active. */
    fun getLibraryApi(): LibraryApi? = libraryApi

    /** Browse endpoints as suspend calls; null until a session is active. */
    fun getBrowseApi(): BrowseApi? = browseApi

    fun getToken(): String? {
        // Read through the session manager so a refreshed token is picked up.
        return sessionManager?.accessToken() ?: token
    }

    fun getApiServiceForLogin(host: String): ApiService {
        return create(host)
    }

    /** Auth endpoints for a host the user is not signed in to yet. */
    fun createAuthApi(host: String): AuthApi =
        buildRetrofit(host, OkHttpClient.Builder().apply { applyTimeouts() }.build())
            .create(AuthApi::class.java)

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
                .apply { applyTimeouts() }
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

    /**
     * OkHttp defaults to 10 seconds, which one request for a whole library can
     * exceed: an unminified page of several hundred items over a TV's wifi,
     * from a server that may be a Raspberry Pi. Connecting is a separate
     * matter - if that has not happened in 15 seconds, it is not going to.
     */
    private fun OkHttpClient.Builder.applyTimeouts() = apply {
        connectTimeout(15, TimeUnit.SECONDS)
        readTimeout(60, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingPolicy.level(BuildConfig.DEBUG))
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
        .apply { applyTimeouts() }
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
        // Logging on purpose: without it the refresh is invisible in logcat, and a
        // silent refresh is indistinguishable from no refresh happening at all.
        // No auth interceptor and no authenticator here - it must not resend the
        // token that just 401'd, nor recurse back into itself.
        val refreshApi = buildRetrofit(
            baseUrl,
            OkHttpClient.Builder().apply { applyTimeouts() }
                .addInterceptor(loggingInterceptor()).build()
        ).create(AuthRefreshApi::class.java)
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
