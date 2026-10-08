package com.paulohenriquesg.fahrenheit.settings

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.player.PlayerSettings
import com.paulohenriquesg.fahrenheit.screensaver.ListeningSource
import com.paulohenriquesg.fahrenheit.screensaver.NowPlayingLine
import com.paulohenriquesg.fahrenheit.screensaver.Queued
import com.paulohenriquesg.fahrenheit.screensaver.ScreensaverStyle
import com.paulohenriquesg.fahrenheit.screensaver.ScreensaverTags
import com.paulohenriquesg.fahrenheit.screensaver.WallArt
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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Settings → Playback: "Screensaver while listening", its style (#156), and trying it (#190). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
@OptIn(ExperimentalTestApi::class)
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

    // --- Try it (#190) ---

    private var queued by mutableStateOf<Queued?>(Queued(itemId = "b1", playing = true))

    private val source = object : ListeningSource {
        @Composable
        override fun queued(): Queued? = queued

        @Composable
        override fun line(): NowPlayingLine =
            NowPlayingLine(itemId = "b1", title = "An Invented Book", detail = "Chapter 3 · 12 min left in chapter")

        // One cover: the wall draws nothing without any.
        override suspend fun art(itemId: String): WallArt = WallArt(covers = listOf(ImageBitmap(8, 8)), wash = null)
    }

    /** Settings as MainScreen holds it: the style chosen is the style shown. */
    private fun renderTrying(style: ScreensaverStyle = ScreensaverStyle.Wall) {
        var chosen by mutableStateOf(style)
        compose.setContent {
            FahrenheitTheme {
                SettingsView(
                    theme = ThemePreference.System, onTheme = {}, rowLayout = true, onLayout = {},
                    version = "v0.0.10", update = UpdateCheck.Idle, onCheckUpdates = {}, onInstall = {},
                    username = "a listener", server = "http://books.example:13378", onSignOut = {},
                    deviceName = "Living room", onDeviceName = {},
                    screensaverStyle = chosen, onScreensaverStyle = { chosen = it; styles += it },
                    screensaver = source
                )
            }
        }
        compose.waitForIdle()
    }

    private val tryIt get() = compose.onNodeWithTag("screensaver_try_it")

    /** Focused, as the remote would leave it, then pressed. */
    private fun tryIt() {
        tryIt.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        // The screensaver moves while it shows; its time is moved by hand.
        compose.mainClock.autoAdvance = false
        tryIt.performSemanticsAction(SemanticsActions.OnClick)
        // The popup attaches, then its art arrives a frame later.
        repeat(2) { settle() }
    }

    /** Both the test rule's clock and the main looper's move: the trial's popup lays out on the looper. */
    private fun settle(ms: Long = 50) {
        compose.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)
        compose.waitForIdle()
    }

    @Test
    fun `with something queued, Try it shows the wall at once`() {
        renderTrying(ScreensaverStyle.Wall)
        tryIt.performScrollTo().assertExists()
        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertDoesNotExist()

        tryIt()

        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertExists()
        compose.onNodeWithTag(ScreensaverTags.WALL, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(ScreensaverTags.BOUNCING_COVER, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `a different style chosen shows that one when tried again`() {
        renderTrying(ScreensaverStyle.Wall)
        tryIt()
        tryIt.performKeyInput { pressKey(Key.DirectionDown) }
        settle()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("screensaver_style_Bouncing").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        tryIt()

        compose.onNodeWithTag(ScreensaverTags.BOUNCING_COVER, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(ScreensaverTags.WALL, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `any key closes it, focus is back on Try it, and the key moves nothing behind it`() {
        renderTrying(ScreensaverStyle.Bouncing)
        tryIt()
        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertExists()

        // Center, the key that pressed Try it: one let through would open it again.
        tryIt.performKeyInput { pressKey(Key.DirectionCenter) }
        settle()

        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertDoesNotExist()
        tryIt.assertIsFocused()
        // Closed, nothing moves: the clock runs again and the screen goes idle.
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        // And the key that closed it was not also a press behind it.
        assertEquals(emptyList<ScreensaverStyle>(), styles)
    }

    @Test
    fun `Back closes it and stays in Settings`() {
        renderTrying(ScreensaverStyle.Wall)
        // As MainScreen's Back handler would: it must never hear this Back.
        var backs = 0
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backs++
                }
            })
        }
        tryIt()

        // Through the activity, as the remote's Back arrives.
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        }
        settle()

        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertDoesNotExist()
        tryIt.assertIsFocused()
        assertEquals(0, backs)
    }

    @Test
    fun `the queue emptying leaves focus on Try it, which then says why it does nothing`() {
        renderTrying()
        tryIt.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        queued = null
        compose.waitForIdle()

        tryIt.assertIsFocused().assertIsNotEnabled()
        compose.onNodeWithText("Play something to try it").assertExists()
        tryIt.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertDoesNotExist()
    }

    @Test
    fun `the queue emptying under a trial closes it, focus still on Try it`() {
        renderTrying(ScreensaverStyle.Bouncing)
        tryIt()
        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertExists()

        queued = null
        settle()

        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertDoesNotExist()
        tryIt.assertIsFocused()
    }

    @Test
    fun `something else queued under a trial closes it rather than mix two items`() {
        renderTrying(ScreensaverStyle.Wall)
        tryIt()

        queued = Queued(itemId = "b2", playing = true)
        settle()

        compose.onNodeWithTag(ScreensaverTags.SCREEN).assertDoesNotExist()
        tryIt.assertIsFocused()
    }

    @Test
    fun `with nothing queued, Try it is disabled and says why`() {
        queued = null
        renderTrying()

        tryIt.performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Play something to try it").assertExists()
        compose.onNodeWithText("Try it").assertDoesNotExist()
    }
}
