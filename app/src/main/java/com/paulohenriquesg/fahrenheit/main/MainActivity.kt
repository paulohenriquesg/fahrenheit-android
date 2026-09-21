package com.paulohenriquesg.fahrenheit.main

import android.os.Bundle
import com.paulohenriquesg.fahrenheit.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
            Toast.makeText(this, getString(R.string.update_did_not_install), Toast.LENGTH_LONG).show()
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
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        // Surfaces testTag as resource-id for UiAutomator/Maestro
                        .semantics { testTagsAsResourceId = true },
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
            { emptyList() },
            { emptyList() }
        )
    }
}