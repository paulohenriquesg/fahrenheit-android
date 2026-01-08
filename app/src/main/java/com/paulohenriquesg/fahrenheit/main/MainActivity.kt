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
import com.paulohenriquesg.fahrenheit.update.UpdateActivity
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
                // Update available - launch UpdateActivity
                if (updateInfo != null) {
                    val intent = UpdateActivity.createIntent(this, updateInfo)
                    startActivity(intent)
                }
            }
        }

        setContent {
            FahrenheitTheme() {
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