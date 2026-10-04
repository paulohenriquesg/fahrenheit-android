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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit
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
        // The screensaver moves while it shows; time here is moved by hand.
        compose.mainClock.autoAdvance = false
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
        // The overlay's effects start with its first frame.
        settle()
    }

    /**
     * The overlay is a view of its own, not the test rule's content: it draws
     * and waits on the main looper, so time moves for it there.
     */
    private fun settle(ms: Long = 50) {
        compose.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)
        compose.waitForIdle()
    }

    /** [ms] pass, on the screensaver's clock and on the one its wait runs on. */
    private fun pass(ms: Long) {
        now += ms
        settle(ms)
    }

    private fun showing() =
        compose.onAllNodesWithTag(ScreensaverTags.SCREEN).fetchSemanticsNodes().isNotEmpty()

    private fun press(): Boolean {
        var eaten = false
        // Through the window, as the remote's keys come: the decor view hands
        // them to the window's callback, where the screensaver sits.
        compose.runOnUiThread {
            val window = compose.activity.window.decorView
            val down = window.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            val up = window.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
            eaten = down && up && keysSeen == 0
        }
        // The overlay settles again on its next frame.
        settle()
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

        pass(2 * 60_000L)

        assertTrue(showing())
        // The bouncing style: the chapter and time left under the cover.
        compose.onNodeWithText("Chapter 3 · 12 min left in chapter", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the first key only wakes the screen and is not acted on`() {
        install()
        pass(2 * 60_000L)

        assertTrue("eaten, not passed to the screen", press())
        assertFalse(showing())
        assertEquals(0, keysSeen)

        press()
        assertTrue("the next key reaches the screen", keysSeen > 0)
    }

    @Test
    fun `a key before the delay starts the wait again`() {
        install()
        pass(90_000L)
        press()
        assertTrue("it went through", keysSeen > 0)

        // Two minutes from the start: the key was 30 s ago.
        pass(30_000L)
        assertFalse("only 30 s since the key", showing())
    }

    @Test
    fun `the screen is kept on while playing, and not when Off`() {
        install()
        assertTrue(keptOn())

        // The setting is read again at the next key.
        delayMinutes = null
        press()
        assertFalse(keptOn())
    }

    @Test
    fun `nothing playing, nothing kept on, nothing shown`() {
        playing = false
        install()
        pass(60 * 60_000L)

        assertFalse(keptOn())
        assertFalse(showing())
    }
}
