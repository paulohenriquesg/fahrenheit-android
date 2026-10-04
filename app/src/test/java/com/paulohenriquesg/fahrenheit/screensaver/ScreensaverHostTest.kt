package com.paulohenriquesg.fahrenheit.screensaver

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * The screensaver on a real screen (#156): it takes over after the delay while
 * something plays, the first key only wakes the screen, the screen is kept on
 * only while it should be, and nothing is asked of the player or the server
 * that the moment does not need.
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
    private var asked = 0
    private var artLoads = 0

    private val source = object : ListeningSource {
        @Composable
        override fun queued(): Queued? {
            LaunchedEffect(Unit) { asked++ }
            return Queued(itemId = "b1", playing = playing)
        }

        @Composable
        override fun line(): NowPlayingLine =
            NowPlayingLine(itemId = "b1", title = "An Invented Book", detail = "Chapter 3 · 12 min left in chapter")

        override suspend fun art(itemId: String): WallArt {
            artLoads++
            return WallArt(covers = emptyList(), wash = null)
        }
    }

    private fun install(times: Int = 1) {
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
        val lastKey = LastKey { now }
        compose.runOnUiThread {
            repeat(times) {
                Screensaver.install(
                    compose.activity,
                    source,
                    settings = { ScreensaverSettings(delayMinutes, ScreensaverStyle.Bouncing) },
                    lastKey = lastKey
                )
            }
        }
        // The overlay's effects start with its first frame.
        settle()
    }

    /** Both the test rule's clock and the main looper's move. */
    private fun settle(ms: Long = 50) {
        compose.mainClock.advanceTimeBy(ms)
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)
        compose.waitForIdle()
    }

    /** [ms] pass, on the screensaver's clock and on the ones its waits run on. */
    private fun pass(ms: Long) {
        now += ms
        settle(ms)
    }

    private fun showing() =
        compose.onAllNodesWithTag(ScreensaverTags.SCREEN).fetchSemanticsNodes().isNotEmpty()

    /** Through the window, as the remote's keys come: the decor view hands them to the window's callback. */
    private fun send(event: KeyEvent): Boolean {
        var eaten = false
        compose.runOnUiThread { eaten = compose.activity.window.decorView.dispatchKeyEvent(event) }
        return eaten
    }

    private fun press(): Boolean {
        val seenBefore = keysSeen
        val down = send(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        val up = send(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        settle()
        return down && up && keysSeen == seenBefore
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

    // A game controller's stick is as much a sign of someone there as a key.
    @Test
    fun `a controller's movement counts as someone there`() {
        install()
        pass(90_000L)
        compose.runOnUiThread {
            val stick = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0).apply { source = InputDevice.SOURCE_JOYSTICK }
            compose.activity.window.callback.dispatchGenericMotionEvent(stick)
            stick.recycle()
        }
        settle()

        pass(30_000L)
        assertFalse("only 30 s since the stick moved", showing())
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
    fun `when Off, the player is not even asked what plays`() {
        delayMinutes = null
        install()
        pass(10 * 60_000L)

        assertEquals(0, asked)
        assertFalse(keptOn())
    }

    // A screen opened onto something paused an hour ago is not a reason to
    // hold the screen on for another delay: only a pause seen here is.
    @Test
    fun `a screen opened onto something already paused keeps nothing on`() {
        playing = false
        install()

        assertFalse(keptOn())
        pass(60 * 60_000L)
        assertFalse(showing())
    }

    @Test
    fun `a pause seen here keeps the screen on for the same delay, then lets go`() {
        install()
        pass(60_000L)
        // As the player's listener does, from outside Compose: told at once.
        playing = false
        Snapshot.sendApplyNotifications()
        settle()
        assertTrue("still within the delay", keptOn())

        pass(2 * 60_000L)
        assertFalse("handed back to the system", keptOn())
        assertFalse(showing())
    }

    // Covers and colour are a fetch and a decode each: only for showing.
    @Test
    fun `the wall's covers and colour are fetched only when it is about to show`() {
        install()
        pass(60_000L)
        assertEquals(0, artLoads)

        pass(60_000L)
        assertTrue(showing())
        assertEquals(1, artLoads)
    }

    @Test
    fun `installed twice, a screen still has one screensaver`() {
        install(times = 2)

        var overlays = 0
        compose.runOnUiThread {
            val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
            overlays = (0 until content.childCount).count { content.getChildAt(it).tag == ScreensaverTags.OVERLAY }
        }
        assertEquals(1, overlays)
    }
}
