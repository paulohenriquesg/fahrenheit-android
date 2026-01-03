package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Version info
                Text(
                    text = "Current version: ${updateInfo.currentVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "New version: ${updateInfo.availableVersion}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Changelog
                if (!updateInfo.changelog.isNullOrBlank()) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = updateInfo.changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Skip button
                    TextButton(onClick = {
                        onSkip()
                        onDismiss()
                    }) {
                        Text("Skip This Version")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Later button
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Download button
                    Button(onClick = {
                        openUrl(context, updateInfo.releaseUrl)
                        onDismiss()
                    }) {
                        Text("Download")
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
