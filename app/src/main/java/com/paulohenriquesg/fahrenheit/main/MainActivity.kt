package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.ControllerSlot
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import android.os.Bundle
import com.paulohenriquesg.fahrenheit.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.paulohenriquesg.fahrenheit.update.AppUpdates
import com.paulohenriquesg.fahrenheit.update.PendingInstall
import kotlinx.coroutines.launch
import com.paulohenriquesg.fahrenheit.update.UpdateActivity

class MainActivity : ComponentActivity() {
    private lateinit var mainHandler: MainHandler

    /**
     * The playback service's player while this screen is visible, for the
     * rail's Now playing entry (#107) - the same connection the player uses,
     * so there is one source of truth.
     */
    private var playback by mutableStateOf<MediaController?>(null)
    private val connection by lazy {
        ControllerSlot(
            connect = { Playback.connect(this) },
            release = { it.release() },
            executor = ContextCompat.getMainExecutor(this),
            onChange = { playback = it }
        )
    }

    override fun onStart() {
        super.onStart()
        connection.open()
    }

    override fun onStop() {
        // Leaves playback alone: Stop on the rail's entry ends it (#155).
        connection.close()
        playback = null
        super.onStop()
    }

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainHandler = MainHandler(this)

        // Use SharedPreferencesHandler to retrieve user preferences
        val sharedPreferencesHandler = SharedPreferencesHandler(this)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()

        // An update handed to the system installer reports nothing back, so the
        // previous dispatch is judged here, once.
        if (AppUpdates.takePendingInstallOutcome(this) == PendingInstall.Outcome.Failed) {
            Toast.makeText(this@MainActivity, getString(R.string.update_did_not_install), Toast.LENGTH_LONG).show()
        }

        // Only from here: a player screen must never be interrupted by this.
        if (AppUpdates.isEnabled(this)) {
            lifecycleScope.launch {
                AppUpdates.checker(this@MainActivity).check()?.let { update ->
                    startActivity(UpdateActivity.createIntent(this@MainActivity, update))
                }
            }
        }

        setContent {
            FahrenheitTheme() {
                Surface(
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxSize()
                        // Surfaces testTag as resource-id for UiAutomator/Maestro
                        .semantics { testTagsAsResourceId = true },
                    shape = RectangleShape
                ) {
                    MainScreen(
                        mainHandler::fetchLibraryItems,
                        mainHandler::fetchPersonalizedView,
                        mainHandler::fetchProgress,
                        playback = playback,
                        favourites = mainHandler::favourites
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FahrenheitTheme {
        MainScreen(
            { _, _ -> emptyList() },
            { emptyList() }
        )
    }
}