package com.paulohenriquesg.fahrenheit.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FahrenheitTheme() {
                SettingsScreen()
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferencesHandler = SharedPreferencesHandler(context)
    var isDarkTheme by remember { mutableStateOf(sharedPreferencesHandler.getUserPreferences().darkTheme) }
    var backgroundColor by remember { mutableStateOf(if (isDarkTheme) Color.Black else Color.White) }

    LaunchedEffect(isDarkTheme) {
        backgroundColor = if (isDarkTheme) Color.Black else Color.White
    }

    Log.i("SettingsScreen", "isDarkTheme: $isDarkTheme")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Apply background color from theme
            .padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Dark Theme")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isDarkTheme,
                onCheckedChange = {
                    isDarkTheme = !isDarkTheme
                    sharedPreferencesHandler.saveUserPreferences(
                        sharedPreferencesHandler.getUserPreferences().copy(darkTheme = isDarkTheme)
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "isDarkTheme: $isDarkTheme")
        Text(
            text = "SharedPreferences dark_theme: ${
                sharedPreferencesHandler.getUserPreferences().darkTheme
            }"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Background color: $backgroundColor")
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(backgroundColor)
                .border(2.dp, Color.Red) // Add red border
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FahrenheitTheme {
        SettingsScreen()
    }
}
