package com.paulohenriquesg.fahrenheit.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeManager
import com.paulohenriquesg.fahrenheit.update.UpdateChecker
import com.paulohenriquesg.fahrenheit.update.UpdateDialog
import com.paulohenriquesg.fahrenheit.update.UpdateInfo

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
    val isDarkTheme by ThemeManager.isDarkTheme

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    Log.i("SettingsScreen", "isDarkTheme: $isDarkTheme")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        // Dark Theme Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Dark Theme", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isDarkTheme,
                onCheckedChange = {
                    ThemeManager.toggleTheme(context)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Version
        Text(
            text = "Version: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Check for Updates Button
        Button(
            onClick = {
                isCheckingUpdate = true
                UpdateChecker.checkForUpdate(
                    currentVersion = BuildConfig.VERSION_NAME,
                    context = context
                ) { result ->
                    isCheckingUpdate = false
                    if (result != null) {
                        updateInfo = result
                    } else {
                        Toast.makeText(context, "You're up to date!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isCheckingUpdate,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCheckingUpdate) {
                CircularProgressIndicator()
            } else {
                Text("Check for Updates")
            }
        }
    }

    // Show update dialog
    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            onDismiss = { updateInfo = null },
            onSkip = {
                UpdateChecker.markVersionSkipped(context, info.availableVersion)
                Toast.makeText(context, "Update skipped", Toast.LENGTH_SHORT).show()
            }
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
