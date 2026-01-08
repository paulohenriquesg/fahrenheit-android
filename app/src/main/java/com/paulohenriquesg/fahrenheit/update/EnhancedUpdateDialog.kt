package com.paulohenriquesg.fahrenheit.update

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EnhancedUpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val downloadState by UpdateService.downloadState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-launch Package Installer when download completes
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Complete) {
            val apkPath = (downloadState as DownloadState.Complete).apkPath

            // Check permission first
            if (!InstallationHelper.canInstallPackages(context)) {
                Toast.makeText(
                    context,
                    "Please grant permission to install apps",
                    Toast.LENGTH_LONG
                ).show()
                InstallationHelper.openInstallPermissionSettings(context)
            } else {
                // Launch Package Installer
                val result = InstallationHelper.installApk(context, apkPath)
                if (result.isFailure) {
                    Toast.makeText(
                        context,
                        "Installation failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Success - Package Installer launched
                    UpdateService.resetState()
                    onDismiss()
                }
            }
        }
    }

    // Prevent dismissal during download
    val canDismiss = downloadState !is DownloadState.Downloading

    Dialog(
        onDismissRequest = {
            if (canDismiss) {
                UpdateService.resetState()
                onDismiss()
            }
        }
    ) {
        Card(
            onClick = { /* Non-interactive dialog card */ },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(16.dp),
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = downloadState) {
                    is DownloadState.Idle -> {
                        UpdateAvailableContent(
                            updateInfo = updateInfo,
                            onSkip = onSkip,
                            onLater = onDismiss,
                            onDownload = {
                                coroutineScope.launch {
                                    UpdateService.downloadApk(context, updateInfo.downloadUrl)
                                }
                            }
                        )
                    }

                    is DownloadState.Downloading -> {
                        DownloadingContent(
                            progress = state.progress,
                            onCancel = {
                                UpdateService.cancelDownload(context)
                                UpdateService.resetState()
                                onDismiss()
                            }
                        )
                    }

                    is DownloadState.Complete -> {
                        // Show brief completion message while Package Installer launches
                        DownloadCompleteContent()
                    }

                    is DownloadState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onClose = {
                                UpdateService.resetState()
                                onDismiss()
                            },
                            onRetry = {
                                coroutineScope.launch {
                                    UpdateService.downloadApk(context, updateInfo.downloadUrl)
                                }
                            }
                        )
                    }

                    is DownloadState.Cancelled -> {
                        UpdateService.resetState()
                        onDismiss()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpdateAvailableContent(
    updateInfo: UpdateInfo,
    onSkip: () -> Unit,
    onLater: () -> Unit,
    onDownload: () -> Unit
) {
    // Title
    Text(
        text = "Update Available",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Version info
    Text(
        text = "New version ${updateInfo.availableVersion} is available",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Current version: ${updateInfo.currentVersion}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Changelog
    if (!updateInfo.changelog.isNullOrBlank()) {
        Text(
            text = "What's New:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = updateInfo.changelog,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Skip button
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Skip", maxLines = 1)
        }

        // Later button
        Button(
            onClick = onLater,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Later", maxLines = 1)
        }

        // Download & Install button (primary)
        Button(
            onClick = onDownload,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .weight(1.5f)
                .height(48.dp)
        ) {
            Text("Download & Install", maxLines = 1)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DownloadingContent(
    progress: Int,
    onCancel: () -> Unit
) {
    // Title
    Text(
        text = "Downloading Update",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Progress bar
    LinearProgressIndicator(
        progress = progress / 100f,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Progress percentage
    Text(
        text = "$progress%",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Cancel button
    Button(
        onClick = onCancel,
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.size(width = 140.dp, height = 48.dp)
    ) {
        Text("Cancel")
    }
}

@Composable
private fun DownloadCompleteContent() {
    // Simple completion message shown briefly while Package Installer launches
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✓ Download Complete",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Launching installer...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorContent(
    message: String,
    onClose: () -> Unit,
    onRetry: () -> Unit
) {
    // Title
    Text(
        text = "Download Failed",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Error message
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        // Close button
        Button(
            onClick = onClose,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(width = 140.dp, height = 48.dp)
        ) {
            Text("Close")
        }

        // Retry button (primary)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.size(width = 140.dp, height = 48.dp)
        ) {
            Text("Retry")
        }
    }
}
