package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import org.commonmark.node.*

class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val update = AvailableUpdate(
            versionCode = intent.getIntExtra(EXTRA_VERSION_CODE, 0),
            versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: "",
            changelog = intent.getStringArrayListExtra(EXTRA_CHANGELOG).orEmpty(),
            apkUrl = intent.getStringExtra(EXTRA_APK_URL) ?: "",
            sha256 = intent.getStringExtra(EXTRA_SHA256) ?: "",
            sizeBytes = intent.getLongExtra(EXTRA_SIZE_BYTES, 0L)
        )

        setContent {
            FahrenheitTheme {
                UpdateScreen(
                    update = update,
                    onDismiss = { finish() },
                    onLater = {
                        AppUpdates.checker(this).snooze(update)
                        Toast.makeText(this, getString(R.string.update_snoozed), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_VERSION_CODE = "version_code"
        private const val EXTRA_VERSION_NAME = "version_name"
        private const val EXTRA_CHANGELOG = "changelog"
        private const val EXTRA_APK_URL = "apk_url"
        private const val EXTRA_SHA256 = "sha256"
        private const val EXTRA_SIZE_BYTES = "size_bytes"

        fun createIntent(context: Context, update: AvailableUpdate): Intent {
            return Intent(context, UpdateActivity::class.java).apply {
                putExtra(EXTRA_VERSION_CODE, update.versionCode)
                putExtra(EXTRA_VERSION_NAME, update.versionName)
                putStringArrayListExtra(EXTRA_CHANGELOG, ArrayList(update.changelog))
                putExtra(EXTRA_APK_URL, update.apkUrl)
                putExtra(EXTRA_SHA256, update.sha256)
                putExtra(EXTRA_SIZE_BYTES, update.sizeBytes)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UpdateScreen(
    update: AvailableUpdate,
    onDismiss: () -> Unit,
    onLater: () -> Unit
) {
    val context = LocalContext.current
    val downloadState by UpdateService.downloadState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-launch Package Installer when download completes
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Complete) {
            val apkPath = (downloadState as DownloadState.Complete).apkPath

            if (!InstallationHelper.canInstallPackages(context)) {
                Toast.makeText(
                    context,
                    context.getString(R.string.install_permission_needed),
                    Toast.LENGTH_LONG
                ).show()
                InstallationHelper.openInstallPermissionSettings(context)
            } else {
                val result = InstallationHelper.installApk(context, apkPath)
                if (result.isFailure) {
                    Toast.makeText(
                        context,
                        "Installation failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    UpdateService.resetState()
                    onDismiss()
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        when (val state = downloadState) {
            is DownloadState.Idle -> {
                UpdateAvailableScreen(
                    update = update,
                    onLater = onLater,
                    onDownload = {
                        coroutineScope.launch {
                            UpdateService.downloadApk(context, update.apkUrl, update.sha256)
                        }
                    }
                )
            }

            is DownloadState.Downloading -> {
                DownloadingScreen(
                    progress = state.progress,
                    version = update.versionName,
                    onCancel = {
                        UpdateService.cancelDownload(context)
                        UpdateService.resetState()
                        onDismiss()
                    }
                )
            }

            is DownloadState.Complete -> {
                DownloadCompleteScreen()
            }

            is DownloadState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onClose = {
                        UpdateService.resetState()
                        onDismiss()
                    },
                    onRetry = {
                        coroutineScope.launch {
                            UpdateService.downloadApk(context, update.apkUrl, update.sha256)
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpdateAvailableScreen(
    update: AvailableUpdate,
    onLater: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 80.dp, vertical = 60.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.update_available),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Version info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.current_version),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "→",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Column {
                Text(
                    text = stringResource(R.string.new_version),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = update.versionName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Changelog
        if (update.changelog.isNotEmpty()) {
            Text(
                text = stringResource(R.string.what_s_new),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MarkdownText(
                    markdown = update.changelog.joinToString("\n") { "- $it" },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onLater,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.later),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onDownload,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.download_install),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DownloadingScreen(
    progress: Int,
    version: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 80.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.downloading_update),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Version $version",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "$progress%",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
        ) {
            Text(
                text = stringResource(R.string.cancel),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun DownloadCompleteScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.download_complete),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.launching_installer),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorScreen(
    message: String,
    onClose: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.download_failed),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val parser = remember { Parser.builder().build() }
    val document = remember(markdown) { parser.parse(markdown) }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val annotatedString = remember(document, surfaceVariant) {
        buildAnnotatedString {
            processNode(document, surfaceVariant)
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.processNode(
    node: Node,
    surfaceVariant: androidx.compose.ui.graphics.Color
) {
    when (node) {
        is Heading -> {
            val style = when (node.level) {
                1 -> SpanStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                2 -> SpanStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                else -> SpanStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            withStyle(style) {
                node.firstChild?.let { child -> processNode(child, surfaceVariant) }
                Unit
            }
            append("\n\n")
        }
        is Paragraph -> {
            node.firstChild?.let { processNode(it, surfaceVariant) }
            append("\n\n")
        }
        is Text -> {
            append(node.literal)
        }
        is StrongEmphasis -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.firstChild?.let { child -> processNode(child, surfaceVariant) }
                Unit
            }
        }
        is Emphasis -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.firstChild?.let { child -> processNode(child, surfaceVariant) }
                Unit
            }
        }
        is BulletList -> {
            node.firstChild?.let { processNode(it, surfaceVariant) }
            append("\n")
        }
        is ListItem -> {
            append("• ")
            node.firstChild?.let { processNode(it, surfaceVariant) }
            append("\n")
        }
        is Code -> {
            withStyle(
                SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = surfaceVariant
                )
            ) {
                append(node.literal)
            }
        }
    }

    // Process next sibling
    node.next?.let { processNode(it, surfaceVariant) }
}
