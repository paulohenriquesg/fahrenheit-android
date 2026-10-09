package com.paulohenriquesg.fahrenheit.api

import android.content.Context
import com.paulohenriquesg.fahrenheit.auth.toAuthSession
import com.paulohenriquesg.fahrenheit.auth.TokenRefreshAuthenticator
import com.paulohenriquesg.fahrenheit.auth.SessionState
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.auth.AuthSession
import com.paulohenriquesg.fahrenheit.auth.AuthRefreshApi
import com.paulohenriquesg.fahrenheit.auth.AuthApi
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
    private var podcastApi: PodcastApi? = null
    private var playlistApi: PlaylistApi? = null
    private var podcastSettingsApi: PodcastSettingsApi? = null
    // Read on ExoPlayer's loading thread (AudioHttp), written on the main one.
    @Volatile
    private var audioClient: OkHttpClient? = null

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
            sharedPreferencesHandler.clearSession()
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
        podcastApi = buildRetrofit(
            hostValue,
            buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
        ).create(PodcastApi::class.java)
        playlistApi = buildRetrofit(
            hostValue,
            buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
        ).create(PlaylistApi::class.java)
        podcastSettingsApi = buildRetrofit(
            hostValue,
            buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
        ).create(PodcastSettingsApi::class.java)
        audioClient = buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
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
        podcastApi = null
        playlistApi = null
        podcastSettingsApi = null
        audioClient = null
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

    /** Podcast feed endpoints as suspend calls; null until a session is active. */
    fun getPodcastApi(): PodcastApi? = podcastApi

    /** The user's playlists, for Favourites (#180); null until a session is active. */
    fun getPlaylistApi(): PlaylistApi? = playlistApi
    /** A show's own settings (#182); null until a session is active. */
    fun getPodcastSettingsApi(): PodcastSettingsApi? = podcastSettingsApi

    /** The authenticated client for streaming audio; null until a session is active. */
    fun audioHttpClient(): OkHttpClient? = audioClient

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

    /**
     * Status only, for looking for servers on the local network (#101): one
     * client for all 254 addresses, and short timeouts, since an address
     * that does not connect within a second on a home network is not a server.
     * No redirects: whatever answers on the port could otherwise send the
     * probe anywhere, the internet included.
     */
    fun createProbeApi(host: String): AuthApi =
        buildRetrofit(host, probeClient).create(AuthApi::class.java)

    private val probeClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

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
