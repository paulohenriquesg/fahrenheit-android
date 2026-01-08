package com.paulohenriquesg.fahrenheit.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.update.UpdateChecker
import com.paulohenriquesg.fahrenheit.update.EnhancedUpdateDialog
import com.paulohenriquesg.fahrenheit.update.UpdateInfo

class MainActivity : ComponentActivity() {
    private lateinit var mainHandler: MainHandler
    private var startupUpdateInfo: UpdateInfo? = null

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainHandler = MainHandler(this)

        // Use SharedPreferencesHandler to retrieve user preferences
        val sharedPreferencesHandler = SharedPreferencesHandler(this)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()

        // Check for updates on startup (max once per 24h)
        if (UpdateChecker.shouldCheckOnStartup(this)) {
            UpdateChecker.checkForUpdate(
                currentVersion = BuildConfig.VERSION_NAME,
                context = this
            ) { updateInfo ->
                // Update available - will show dialog in MainScreen
                if (updateInfo != null) {
                    // Store for MainScreen to display
                    startupUpdateInfo = updateInfo
                }
            }
        }

        setContent {
            FahrenheitTheme() {
                val context = LocalContext.current
                var updateInfo by remember { mutableStateOf(startupUpdateInfo) }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainScreen(
                        mainHandler::fetchLibraryItems,
                        mainHandler::fetchPersonalizedView
                    )
                }

                // Show update dialog if available
                updateInfo?.let { info ->
                    EnhancedUpdateDialog(
                        updateInfo = info,
                        onDismiss = {
                            updateInfo = null
                            startupUpdateInfo = null
                        },
                        onSkip = {
                            UpdateChecker.markVersionSkipped(context, info.availableVersion)
                            updateInfo = null
                            startupUpdateInfo = null
                        }
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
            { _, _ -> },
            { _, _ -> }
        )
    }
}