package com.paulohenriquesg.fahrenheit.main

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Now playing shows the state, not the button's action (#170): bars that move
 * while playing, a still pause badge while paused. The player's own button and
 * the remote show the action, so a ▶ while playing read as the opposite.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NowPlayingStateTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val book = RailEntry("b1", null, "A Long Drift", playing = true, progress = 0.4f, chapter = "Chapter 12", chapterNumber = 12, leftSeconds = 1210.0)
    private var entry by mutableStateOf(book)

    private fun show(first: RailEntry, byHand: Boolean = false) {
        entry = first
        if (byHand) compose.mainClock.autoAdvance = false
        compose.setContent { FahrenheitTheme { CoverWithRing(entry) } }
        if (byHand) compose.mainClock.advanceTimeByFrame() else compose.waitForIdle()
    }

    @Test fun `playing, the bars and no pause badge`() {
        show(book)
        compose.onNodeWithTag(NOW_PLAYING_EQUALISER_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Playing").assertExists()
        compose.onNodeWithTag(NOW_PLAYING_PAUSED_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun `paused, a pause badge and no bars`() {
        show(book.copy(playing = false))
        compose.onNodeWithTag(NOW_PLAYING_PAUSED_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Paused").assertExists()
        compose.onNodeWithTag(NOW_PLAYING_EQUALISER_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun `while playing the bars move`() {
        show(book, byHand = true)
        compose.mainClock.advanceTimeBy(300)
        val early = bars()
        compose.mainClock.advanceTimeBy(700)
        assertNotEquals(early, bars())
    }

    // Nothing runs forever: an animation that never ends keeps the app from
    // ever being idle, and spends the stick's frames on a screen nobody watches.
    @Test fun `the bars come to rest when nothing new arrives`() {
        show(book, byHand = true)
        compose.mainClock.advanceTimeBy(10_000)
        val resting = bars()
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(resting, bars())
    }

    // The slot hands over a new entry with each poll while playing: each one
    // starts the bars again.
    @Test fun `a new entry while playing moves them again`() {
        show(book, byHand = true)
        compose.mainClock.advanceTimeBy(10_000)
        val resting = bars()
        entry = book.copy(leftSeconds = 1205.0)
        // With the clock moved by hand, the write has to be announced to be seen.
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(400)
        assertNotEquals(resting, bars())
    }

    @Test fun `a still pause badge is idle`() {
        show(book.copy(playing = false))
        // waitForIdle returning at all is the point; and the badge stays put.
        compose.waitForIdle()
        compose.onNodeWithTag(NOW_PLAYING_PAUSED_TAG, useUnmergedTree = true).assertExists()
    }

    // The window drawn into a bitmap (captureToImage waits for a frame callback
    // Robolectric never sends), cut to the bars' own bounds.
    private fun bars(): List<Int> {
        val node = compose.onNodeWithTag(NOW_PLAYING_EQUALISER_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val at = node.positionInWindow
        var pixels: List<Int> = emptyList()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val map = bitmap.asImageBitmap().toPixelMap()
            pixels = (0 until node.size.height).flatMap { y ->
                (0 until node.size.width).map { x -> map[at.x.toInt() + x, at.y.toInt() + y].hashCode() }
            }
        }
        return pixels
    }
}
