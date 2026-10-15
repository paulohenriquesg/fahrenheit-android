package com.paulohenriquesg.fahrenheit.update

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The question over Home (#229, docs/mocks/update-prompt.html): Ignore, No
 * and Yes, then the download in the same dialog.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp")
class UpdatePromptTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val mb = 1024L * 1024L
    private val update = AvailableUpdate(
        versionCode = 11,
        versionName = "v0.1.1",
        changelog = listOf("Fixes"),
        apkUrl = "https://example.invalid/app.apk",
        sha256 = "abc",
        sizeBytes = 272 * mb / 10
    )

    /** The service's part, without a network: a download runs until cancelled. */
    private class FakeDownloads : UpdateDownloads {
        override val state = MutableStateFlow<DownloadState>(DownloadState.Idle)
        var started = 0
        var cancelled = 0
        var progress: DownloadState = DownloadState.Downloading(42, 0, 0)
        /** As the service on its IO thread: nothing written yet after Yes. */
        var slowStart = false

        override suspend fun download(update: AvailableUpdate) {
            started++
            try {
                if (!slowStart) state.value = progress
                awaitCancellation()
            } catch (e: CancellationException) {
                cancelled++
                state.value = DownloadState.Cancelled
                throw e
            }
        }

        override fun reset() {
            state.value = DownloadState.Idle
        }
    }

    private val downloads = FakeDownloads()
    private var ignored = 0
    private var done = 0
    private val installed = mutableListOf<String>()

    private fun show() {
        compose.setContent {
            FahrenheitTheme {
                UpdateOffer(
                    update = update,
                    downloads = downloads,
                    onIgnore = { ignored++ },
                    onInstall = { installed += it },
                    onDone = { done++ }
                )
            }
        }
        compose.waitForIdle()
    }

    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun back(onTag: String) {
        compose.onNodeWithTag(onTag).performKeyInput { pressKey(Key.Back) }
        compose.waitForIdle()
    }

    @Test
    fun `asks about the new version, with focus on No`() {
        show()

        compose.onNodeWithText("New version!").assertExists()
        compose.onNodeWithText("Version v0.1.1 found. Update?").assertExists()
        compose.onNodeWithTag("update_ignore").assertExists()
        compose.onNodeWithTag("update_yes").assertExists()
        compose.onNodeWithTag("update_no").assertIsFocused()
    }

    @Test
    fun `No closes it and stores nothing`() {
        show()

        press("update_no")

        assertEquals(1, done)
        assertEquals(0, ignored)
        assertEquals(0, downloads.started)
    }

    @Test
    fun `Back is No`() {
        show()

        back("update_no")

        assertEquals(1, done)
        assertEquals(0, ignored)
        assertEquals(0, downloads.started)
    }

    @Test
    fun `Ignore stores the version and closes`() {
        show()

        press("update_ignore")

        assertEquals(1, ignored)
        assertEquals(1, done)
        assertEquals(0, downloads.started)
    }

    @Test
    fun `Yes downloads in the same dialog, in MB of the total`() {
        downloads.progress = DownloadState.Downloading(42, 114 * mb / 10, 272 * mb / 10)
        show()

        press("update_yes")

        assertEquals(1, downloads.started)
        compose.onNodeWithText("Downloading v0.1.1").assertExists()
        compose.onNodeWithText("11.4 MB of 27.2 MB").assertExists()
        compose.onNodeWithText("42%").assertExists()
        compose.onNodeWithTag("update_cancel").assertIsFocused()
        assertEquals(0, done)
    }

    // A server that does not send a length still has the manifest's size.
    @Test
    fun `with no length from the server, the total is the manifest's size`() {
        downloads.progress = DownloadState.Downloading(0, 114 * mb / 10, 0)
        show()

        press("update_yes")

        compose.onNodeWithText("11.4 MB of 27.2 MB").assertExists()
        compose.onNodeWithText("41%").assertExists()
    }

    @Test
    fun `Cancel stops the download and closes, storing nothing`() {
        show()
        press("update_yes")

        press("update_cancel")

        assertEquals(1, downloads.cancelled)
        assertEquals(1, done)
        assertEquals(0, ignored)
        // The cancelled download may say so after the reset: either is at rest.
        assertFalse(downloads.state.value is DownloadState.Downloading)
    }

    @Test
    fun `Back while downloading is Cancel`() {
        show()
        press("update_yes")

        back("update_cancel")

        assertEquals(1, downloads.cancelled)
        assertEquals(1, done)
    }

    @Test
    fun `a failed download says so, with Retry and Close`() {
        show()
        press("update_yes")

        downloads.state.value = DownloadState.Error("Download failed with HTTP 503")
        compose.waitForIdle()

        compose.onNodeWithText("Download failed").assertExists()
        compose.onNodeWithTag("update_close").assertExists()
        compose.onNodeWithTag("update_retry").assertIsFocused()
        assertEquals(0, done)
    }

    @Test
    fun `Retry starts the download again`() {
        show()
        press("update_yes")
        downloads.state.value = DownloadState.Error("Download failed with HTTP 503")
        compose.waitForIdle()

        press("update_retry")

        assertEquals(2, downloads.started)
        compose.onNodeWithTag("update_cancel").assertExists()
    }

    @Test
    fun `Close after a failure closes it`() {
        show()
        press("update_yes")
        downloads.state.value = DownloadState.Error("Download failed with HTTP 503")
        compose.waitForIdle()

        press("update_close")

        assertEquals(1, done)
        assertEquals(DownloadState.Idle, downloads.state.value)
    }

    @Test
    fun `a checked download goes to the installer and the dialog closes`() {
        show()
        press("update_yes")

        downloads.state.value = DownloadState.Complete("/cache/fahrenheit_update.apk")
        compose.waitForIdle()

        assertEquals(listOf("/cache/fahrenheit_update.apk"), installed)
        assertEquals(1, done)
        assertEquals(DownloadState.Idle, downloads.state.value)
    }

    @Test
    fun `nothing is installed or ignored while it only asks`() {
        show()

        assertTrue(installed.isEmpty())
        assertFalse(ignored > 0)
    }

    // The download service outlives a dialog. What an earlier one left there -
    // a finished or failed download - is not this dialog's (from review).
    @Test
    fun `Yes after an earlier dialog's finished download does not install it`() {
        downloads.state.value = DownloadState.Complete("/cache/earlier.apk")
        downloads.slowStart = true
        show()

        press("update_yes")

        assertTrue(installed.isEmpty())
        assertEquals(0, done)
        compose.onNodeWithTag("update_cancel").assertIsFocused()
    }

    @Test
    fun `Yes after an earlier dialog's failure shows the download, not the failure`() {
        downloads.state.value = DownloadState.Error("Download failed with HTTP 503")
        downloads.slowStart = true
        show()

        press("update_yes")

        compose.onNodeWithTag("update_retry").assertDoesNotExist()
        compose.onNodeWithTag("update_cancel").assertIsFocused()
    }
}
