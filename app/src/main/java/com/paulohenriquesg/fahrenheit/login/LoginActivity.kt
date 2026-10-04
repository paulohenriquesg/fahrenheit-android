package com.paulohenriquesg.fahrenheit.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.elements.RecentCovers
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class LoginActivity : ComponentActivity() {
    /** Frame 1 (#101): the Audiobookshelf servers answering on this network. */
    private suspend fun findServers(onFound: (FoundServer) -> Unit) {
        val own = withContext(Dispatchers.IO) { LocalNetwork.ownAddress() } ?: return
        ServerScan(probe = { url -> ApiClient.createProbeApi(url).status() }).scan(own, onFound)
    }

    private lateinit var loginHandler: LoginHandler

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginHandler = LoginHandler(this)

        val sharedPreferencesHandler = SharedPreferencesHandler(this)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()
        val destination = StartDestination.of(userPreferences.host, userPreferences.token)
        if (destination == StartDestination.Main) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

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