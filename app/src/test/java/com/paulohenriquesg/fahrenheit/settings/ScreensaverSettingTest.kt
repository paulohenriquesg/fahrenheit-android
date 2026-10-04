package com.paulohenriquesg.fahrenheit.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.player.PlayerSettings
import com.paulohenriquesg.fahrenheit.screensaver.ScreensaverStyle
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings → Playback: "Screensaver while listening" and its style (#156). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ScreensaverSettingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val settings get() = PlayerSettings(ApplicationProvider.getApplicationContext())

    // --- stored on the device ---

    @Test
    fun `five minutes and the wall of covers until changed`() {
        assertEquals(5, settings.screensaverMinutes)
        assertEquals(ScreensaverStyle.Wall, settings.screensaverStyle)
    }

    @Test
    fun `the choice is kept, Off included`() {
        settings.screensaverMinutes = null
        assertNull(settings.screensaverMinutes)
        settings.screensaverMinutes = 2
        assertEquals(2, settings.screensaverMinutes)
        settings.screensaverStyle = ScreensaverStyle.Bouncing
        assertEquals(ScreensaverStyle.Bouncing, settings.screensaverStyle)
    }

    // --- the rows ---

    private val minutes = mutableListOf<Int?>()
    private val styles = mutableListOf<ScreensaverStyle>()

    private fun render(delay: Int? = 5, style: ScreensaverStyle = ScreensaverStyle.Wall) {
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {}, onInstall = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    deviceName = "Living room", onDeviceName = {},
                    screensaverMinutes = delay, onScreensaverMinutes = { minutes += it },
                    screensaverStyle = style, onScreensaverStyle = { styles += it }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the rows offer Off, 2, 5 and 10 minutes, and both styles, the ones set ticked`() {
        render(delay = 10, style = ScreensaverStyle.Bouncing)
        listOf("off", "2", "5", "10").forEach { compose.onNodeWithTag("screensaver_$it").performScrollTo() }
        listOf("Wall", "Bouncing").forEach { compose.onNodeWithTag("screensaver_style_$it").performScrollTo() }

        compose.onNodeWithText("10 min ✓").assertExists()
        compose.onNodeWithText("Bouncing cover ✓").assertExists()
    }

    @Test
    fun `choosing reports it`() {
        render()
        compose.onNodeWithTag("screensaver_off").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("screensaver_2").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("screensaver_style_Bouncing").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(listOf(null, 2), minutes)
        assertEquals(listOf(ScreensaverStyle.Bouncing), styles)
    }
}
