package com.paulohenriquesg.fahrenheit.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    private lateinit var mainHandler: MainHandler

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainHandler = MainHandler(this)

        // Use SharedPreferencesHandler to retrieve user preferences
        val sharedPreferencesHandler = SharedPreferencesHandler(this)
        val userPreferences = sharedPreferencesHandler.getUserPreferences()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            FahrenheitTheme(themeViewModel = themeViewModel) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainScreen(mainHandler::fetchLibraryItems)
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
            { _, _ -> }
        )
    }
}