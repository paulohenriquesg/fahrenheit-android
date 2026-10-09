package com.paulohenriquesg.fahrenheit.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.main.MainActivity
import com.paulohenriquesg.fahrenheit.api.ApiClient
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.elements.RecentCovers
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.utils.StartupTimeline

class LoginActivity : ComponentActivity() {
    /** Frame 1 (#101): the Audiobookshelf servers answering on this network. */
    private suspend fun findServers(onFound: (FoundServer) -> Unit) {
        val own = withContext(Dispatchers.IO) { LocalNetwork.ownAddress() } ?: return
        ServerScan(probe = { url -> ApiClient.createProbeApi(url).status() }).scan(own, onFound)
    }

    private lateinit var loginHandler: LoginHandler

    /**
     * Whether the stored session still works (#191), asked of the server
     * through the app's own client, so an expired access token is refreshed
     * here, on the launch screen, rather than on Home's first request.
     */
    private suspend fun checkSession(): SessionCheck.Result {
        val api = ApiClient.getPodcastApi() ?: return SessionCheck.Result.Rejected
        val tokenBefore = ApiClient.getToken()
        sessionChecks++
        StartupTimeline.app.mark("session check started")
        val result = SessionCheck { withContext(Dispatchers.IO) { api.me() } }.run()
        val refreshed = ApiClient.getToken() != tokenBefore
        // The first check's verdict, then, if it took retries, when one got through.
        StartupTimeline.app.mark("session check done", "$result, token refreshed=$refreshed")
        if (result == SessionCheck.Result.Ready && sessionChecks > 1) {
            StartupTimeline.app.mark("session ready", "check $sessionChecks, token refreshed=$refreshed")
        }
        return result
    }

    private var sessionChecks = 0

    /**
     * Only once the screen is in front: a check that ends after Home was
     * pressed would have its start blocked as a background launch, and the
     * finish() after it would close the app.
     */
    private fun goHome() {
        lifecycleScope.launch {
            lifecycle.withStarted {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // The icon on the dark background from the first frame (#191); it
        // gives way to the launch screen as soon as that draws.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        StartupTimeline.app.mark("login started")

        loginHandler = LoginHandler(this)

        val sharedPreferencesHandler = SharedPreferencesHandler(this)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()
        val destination = StartDestination.of(userPreferences.host, userPreferences.token)
        // A stored session is checked before Home; none goes to the form, as before.
        val check = if (destination == StartDestination.Main) ::checkSession else null

        setContent {
            FahrenheitTheme {
                Surface(
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxSize()
                        // Surfaces testTag as resource-id for UiAutomator/Maestro
                        .semantics { testTagsAsResourceId = true },
                    shape = RectangleShape
                ) {
                    LaunchGate(
                        check = check,
                        onReady = ::goHome
                    ) {
                        LoginScreen(
                            loginHandler::handleLogin,
                            loginHandler::handleApiKeyLogin,
                            error = loginHandler.error.value,
                            onDismissError = loginHandler::clearError,
                            findServers = ::findServers,
                            // Welcome back over the covers this device already has (#161).
                            loadCovers = { host -> RecentCovers.loadCached(this@LoginActivity, host) }
                        )
                    }
                }
            }
        }
    }
}
