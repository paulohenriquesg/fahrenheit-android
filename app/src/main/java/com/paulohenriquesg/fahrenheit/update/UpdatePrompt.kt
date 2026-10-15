package com.paulohenriquesg.fahrenheit.update

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.login.LoginButton
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/** The download behind the dialog: [UpdateService] in the app, a fake in tests. */
interface UpdateDownloads {
    val state: StateFlow<DownloadState>
    suspend fun download(update: AvailableUpdate)
    fun reset()

    /** The real one, with its SHA-256 check. */
    class Service(private val context: android.content.Context) : UpdateDownloads {
        override val state get() = UpdateService.downloadState
        override suspend fun download(update: AvailableUpdate) {
            UpdateService.downloadApk(context, update.apkUrl, update.sha256)
        }
        override fun reset() = UpdateService.resetState()
    }
}

/**
 * Hands [check]'s answer to [show] once Home is in front. The launch check
 * answers whenever the network does, and by then a player may have been
 * opened over Home: the question is never asked over it (#229).
 */
suspend fun offerOnHome(lifecycle: Lifecycle, check: suspend () -> AvailableUpdate?, show: (AvailableUpdate) -> Unit) {
    val update = check() ?: return
    lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.STARTED) }
    show(update)
}

/** What the dialog shows. */
private sealed interface Step {
    data object Question : Step
    data class Downloading(val bytesDone: Long, val bytesTotal: Long, val percent: Int) : Step
    data object Failed : Step
}

/**
 * The update question over Home, and the download it starts, in one dialog
 * (#229; docs/mocks/update-prompt.html).
 *
 * @param onIgnore stores [update] as ignored; [onDone] follows.
 * @param onInstall gets the checked APK, for Android's installer.
 * @param onDone the dialog is finished with: No, Ignore, Cancel, Close or the
 *   hand-off to the installer.
 */
@Composable
fun UpdateOffer(
    update: AvailableUpdate,
    downloads: UpdateDownloads,
    onIgnore: () -> Unit,
    onInstall: (String) -> Unit,
    onDone: () -> Unit
) {
    val state by downloads.state.collectAsState()
    val scope = rememberCoroutineScope()
    // What the service held before this dialog's Yes is not this dialog's.
    var started by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun start() {
        // Whatever an earlier download left in the service is not this one's.
        downloads.reset()
        started = true
        job = scope.launch { downloads.download(update) }
    }

    fun close() {
        job?.cancel()
        job = null
        downloads.reset()
        onDone()
    }

    val current = state
    LaunchedEffect(started, current) {
        if (started && current is DownloadState.Complete) {
            onInstall(current.apkPath)
            downloads.reset()
            onDone()
        }
    }

    val step = when {
        !started -> Step.Question
        current is DownloadState.Error -> Step.Failed
        current is DownloadState.Downloading -> {
            // The manifest's size stands in when the server sends no length.
            val total = current.bytesTotal.takeIf { it > 0 } ?: update.sizeBytes
            val percent = if (current.bytesTotal > 0) current.progress
            else if (total > 0) (current.bytesDone * 100 / total).toInt() else 0
            Step.Downloading(current.bytesDone, total, percent)
        }
        // Yes was pressed and the first bytes have not arrived.
        else -> Step.Downloading(0, update.sizeBytes, 0)
    }

    UpdatePrompt(
        versionName = update.versionName,
        step = step,
        onIgnore = {
            onIgnore()
            onDone()
        },
        onNo = onDone,
        onYes = ::start,
        onCancel = ::close,
        onRetry = ::start,
        onClose = ::close
    )
}

@Composable
private fun UpdatePrompt(
    versionName: String,
    step: Step,
    onIgnore: () -> Unit,
    onNo: () -> Unit,
    onYes: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    // Back is the safe answer at every step: No, Cancel, Close.
    val onBack = when (step) {
        Step.Question -> onNo
        is Step.Downloading -> onCancel
        Step.Failed -> onClose
    }
    // Its own window: Home's own focus requests behind it, as its shelves
    // arrive, cannot take focus from the question.
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(step::class) { focus.requestFocusWhenAttached() }
        Column(
            modifier = Modifier
                .width(500.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .onPreviewKeyEvent { event ->
                    if (event.key != Key.Back) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyUp) onBack()
                    true
                }
                .focusGroup()
                .padding(start = 30.dp, end = 30.dp, top = 28.dp, bottom = 22.dp)
                .testTag("update_prompt")
        ) {
            when (step) {
                Step.Question -> {
                    Title(stringResource(R.string.update_prompt_title))
                    Body(stringResource(R.string.update_prompt_question, versionName))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuietButton(stringResource(R.string.update_prompt_ignore), onIgnore, Modifier.testTag("update_ignore"))
                        Spacer(Modifier.weight(1f))
                        LoginButton(
                            text = stringResource(R.string.update_prompt_no),
                            onClick = onNo,
                            modifier = Modifier.focusRequester(focus).testTag("update_no")
                        )
                        LoginButton(
                            text = stringResource(R.string.update_prompt_yes),
                            onClick = onYes,
                            primary = true,
                            modifier = Modifier.testTag("update_yes")
                        )
                    }
                }
                is Step.Downloading -> {
                    Title(stringResource(R.string.update_prompt_downloading, versionName))
                    LinearProgressIndicator(
                        progress = { step.percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp, bottom = 9.dp)
                            .height(7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
                        Meta(
                            stringResource(R.string.update_prompt_bytes, megabytes(step.bytesDone), megabytes(step.bytesTotal)),
                            Modifier.weight(1f).testTag("update_bytes")
                        )
                        Meta(stringResource(R.string.update_prompt_percent, step.percent), Modifier.testTag("update_percent"))
                    }
                    Row {
                        Spacer(Modifier.weight(1f))
                        LoginButton(
                            text = stringResource(R.string.cancel),
                            onClick = onCancel,
                            modifier = Modifier.focusRequester(focus).testTag("update_cancel")
                        )
                    }
                }
                Step.Failed -> {
                    Title(stringResource(R.string.update_prompt_failed))
                    Body(stringResource(R.string.update_prompt_failed_detail, versionName))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(Modifier.weight(1f))
                        LoginButton(
                            text = stringResource(R.string.close),
                            onClick = onClose,
                            modifier = Modifier.testTag("update_close")
                        )
                        LoginButton(
                            text = stringResource(R.string.retry),
                            onClick = onRetry,
                            primary = true,
                            modifier = Modifier.focusRequester(focus).testTag("update_retry")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 11.dp)
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 25.dp)
    )
}

@Composable
private fun Meta(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Ignore: a text button in the mock, quieter than No and Yes. */
@Composable
private fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        // Focus does not move things (docs/ui-style-guide.md).
        scale = ButtonDefaults.scale(focusedScale = 1f)
    ) {
        Text(text)
    }
}

/** "11.4": megabytes as Settings counts them, one decimal. */
private fun megabytes(bytes: Long): String =
    String.format(Locale.ROOT, "%.1f", bytes / BYTES_PER_MB.toDouble())

private const val BYTES_PER_MB = 1024L * 1024L
