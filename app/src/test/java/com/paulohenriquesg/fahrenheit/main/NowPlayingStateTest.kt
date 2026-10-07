package com.paulohenriquesg.fahrenheit.main

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.ui.elements.CoverPlaceholderTone
import com.paulohenriquesg.fahrenheit.ui.elements.CoverTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
    private lateinit var scheme: ColorScheme

    private fun show(first: RailEntry, byHand: Boolean = false) {
        entry = first
        if (byHand) compose.mainClock.autoAdvance = false
        compose.setContent {
            FahrenheitTheme {
                scheme = MaterialTheme.colorScheme
                CoverWithRing(entry)
            }
        }
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

    // The ring is 44 dp with a 3 dp stroke, so 38 dp inside it (#177).
    @Test fun `the cover sits inside the ring`() {
        show(book.copy(playing = false))
        val cover = compose.onNodeWithTag(NOW_PLAYING_COVER_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val inside = with(compose.density) { 38.dp.toPx() }
        val side = with(compose.density) { 36.dp.toPx() }
        assertEquals(side, cover.size.width.toFloat(), 1f)
        assertEquals(side, cover.size.height.toFloat(), 1f)
        assertTrue(cover.size.width <= inside)
    }

    // A square's corners ran into the ring (#177). With no server the
    // placeholder is drawn, and its padding is plain tone at the top middle;
    // a round cover leaves the box's corner to what is behind it.
    @Test fun `the cover is round, placeholder and all`() {
        show(book.copy(playing = false))
        compose.onNodeWithTag(CoverTags.PLACEHOLDER, useUnmergedTree = true).assertExists()
        val node = compose.onNodeWithTag(NOW_PLAYING_COVER_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val at = node.positionInWindow
        val map = window()
        val left = at.x.toInt()
        val top = at.y.toInt()
        val middle = map[left + node.size.width / 2, top + 2]
        assertEquals(CoverPlaceholderTone.of(scheme), middle)
        assertNotEquals(middle, map[left + 1, top + 1])
    }

    // The window drawn into a bitmap (captureToImage waits for a frame callback
    // Robolectric never sends), cut to the bars' own bounds.
    private fun bars(): List<Int> {
        val node = compose.onNodeWithTag(NOW_PLAYING_EQUALISER_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val at = node.positionInWindow
        val map = window()
        return (0 until node.size.height).flatMap { y ->
            (0 until node.size.width).map { x -> map[at.x.toInt() + x, at.y.toInt() + y].hashCode() }
        }
    }

    private fun window(): PixelMap {
        lateinit var map: PixelMap
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            map = bitmap.asImageBitmap().toPixelMap()
        }
        return map
    }
}
