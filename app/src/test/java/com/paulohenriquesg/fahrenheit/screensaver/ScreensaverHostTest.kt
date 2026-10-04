package com.paulohenriquesg.fahrenheit.screensaver

import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The screensaver on a real screen (#156): it takes over after the delay while
 * something plays, the first key only wakes the screen, and the screen is kept
 * on only while it should be.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ScreensaverHostTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var now = 0L
    private var playing by mutableStateOf(true)
    private var delayMinutes: Int? = 2
    private var keysSeen = 0

    private val line = NowPlayingLine(itemId = "b1", title = "An Invented Book", detail = "Chapter 3 · 12 min left in chapter")

    private fun install() {
        compose.setContent {
            FahrenheitTheme {
                val focus = remember { FocusRequester() }
                Box(
                    Modifier
                        .fillMaxSize()
                        .focusRequester(focus)
                        .focusable()
                        .onPreviewKeyEvent { keysSeen++; false }
                )
                LaunchedEffect(Unit) { focus.requestFocus() }
            }
        }
        compose.runOnUiThread {
            Screensaver.install(
                compose.activity,
                listening = { Listening(playing = playing, line = line, covers = emptyList(), wash = null) },
                settings = { ScreensaverSettings(delayMinutes, ScreensaverStyle.Bouncing) },
                clock = { now }
            )
        }
        compose.waitForIdle()
    }

    private fun tick() {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
    }

    private fun showing() =
        compose.onAllNodesWithTag(ScreensaverTags.SCREEN).fetchSemanticsNodes().isNotEmpty()

    private fun press(): Boolean {
        var eaten = false
        compose.runOnUiThread {
            val down = compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            val up = compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
            eaten = down && up && keysSeen == 0
        }
        compose.waitForIdle()
        return eaten
    }

    private fun keptOn(): Boolean {
        var on = false
        compose.runOnUiThread { on = compose.activity.window.decorView.findViewWithTag<View>(ScreensaverTags.OVERLAY)?.keepScreenOn == true }
        return on
    }

    @Test
    fun `after the delay while playing it shows, with what is playing`() {
        install()
        assertFalse(showing())

        now = 2 * 60_000L
        tick()

        assertTrue(showing())
        compose.onNodeWithText("An Invented Book", substring = true).assertExists()
    }

    @Test
    fun `the first key only wakes the screen and is not acted on`() {
        install()
        now = 2 * 60_000L
        tick()

        assertTrue("eaten, not passed to the screen", press())
        assertFalse(showing())
        assertEquals(0, keysSeen)

        press()
        assertTrue("the next key reaches the screen", keysSeen > 0)
    }

    @Test
    fun `a key before the delay starts the wait again`() {
        install()
        now = 90_000L
        tick()
        press()
        assertTrue("it went through", keysSeen > 0)

        now = 2 * 60_000L
        tick()
        assertFalse("only 30 s since the key", showing())
    }

    @Test
    fun `the screen is kept on while playing, and not when Off`() {
        install()
        assertTrue(keptOn())

        delayMinutes = null
        tick()
        assertFalse(keptOn())
    }

    @Test
    fun `nothing playing, nothing kept on, nothing shown`() {
        playing = false
        install()
        now = 60 * 60_000L
        tick()

        assertFalse(keptOn())
        assertFalse(showing())
    }
}
