package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The show's Downloads panel (#182, mock frame 3). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class DownloadsPanelTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val changes = mutableListOf<DownloadChange>()
    private var closed = false

    private fun render(
        settings: DownloadSettings = DownloadSettings(enabled = true, schedule = "0 0 * * *", keep = 5, perCheck = 3),
        failed: Boolean = false
    ) {
        compose.setContent {
            FahrenheitTheme {
                DownloadsPanel(settings = settings, failed = failed, onChange = { changes += it }, onClose = { closed = true })
            }
        }
        compose.waitForIdle()
    }

    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test
    fun `the current settings are the ones chosen, and focus starts on the first`() {
        render()

        compose.onNodeWithTag("downloads_enabled").assertIsFocused().assertIsOn()
        compose.onNodeWithTag("downloads_schedule_Daily").assertIsSelected()
        compose.onNodeWithTag("downloads_schedule_Hourly").assertIsNotSelected()
        compose.onNodeWithTag("downloads_keep_5").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("downloads_per_check_3").performScrollTo().assertIsSelected()
    }

    @Test
    fun `each choice asks for its change`() {
        render()

        press("downloads_enabled")
        press("downloads_schedule_Weekly")
        press("downloads_keep_0")
        press("downloads_per_check_10")

        assertEquals(
            listOf(
                DownloadChange.Enabled(false),
                DownloadChange.Schedule(ScheduleChoice.Weekly),
                DownloadChange.Keep(0),
                DownloadChange.PerCheck(10)
            ),
            changes
        )
    }

    // The schedule the web app set stays until a choice is picked.
    @Test
    fun `a schedule of the web app's own shows as Custom, which cannot be picked`() {
        render(DownloadSettings(enabled = true, schedule = "*/30 * * * *", keep = 0, perCheck = 3))

        compose.onNodeWithTag("downloads_schedule_Custom")
            .assertIsDisplayed()
            .assertIsSelected()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose.onNodeWithTag("downloads_schedule_Daily").assertIsNotSelected()
    }

    @Test
    fun `without a custom schedule there is no Custom`() {
        render()

        compose.onNodeWithTag("downloads_schedule_Custom").assertDoesNotExist()
    }

    @Test
    fun `a keep the server holds that is not one of ours is still shown, chosen`() {
        render(DownloadSettings(enabled = true, schedule = "0 0 * * *", keep = 7, perCheck = 3))

        compose.onNodeWithText("Latest 7").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("downloads_keep_7").assertIsSelected()
    }

    @Test
    fun `a failed save says so`() {
        render(failed = true)

        compose.onNodeWithText("Couldn't save that. Check the connection and try again.").assertIsDisplayed()
    }

    @Test
    fun `Back closes it`() {
        render()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertTrue(closed)
    }
}
