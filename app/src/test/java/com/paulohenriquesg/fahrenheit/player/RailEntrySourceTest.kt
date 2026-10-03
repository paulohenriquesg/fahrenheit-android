package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The rail reads what is queued from the player it is given - the main screen's controller (#107). */
@RunWith(RobolectricTestRunner::class)
class RailEntrySourceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer
    private var entry: RailEntry? = null
    private val asked = mutableListOf<String>()

    @After
    fun tearDown() = player.release()

    private fun show() {
        player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        compose.setContent {
            entry = rememberRailEntry(player) { itemId ->
                asked += itemId
                listOf(Chapter(start = 0.0, end = 1800.0, title = "One"), Chapter(start = 1800.0, end = 3600.0, title = "Two"))
            }
        }
        compose.waitForIdle()
    }

    private fun queue(startAt: Double) {
        val book = NowPlaying("b1", "A Book", TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/b1"))), null, null, null, false, null)
        val queue = PlaybackQueue.of(book, startAt) { "https://abs.test$it" }!!
        compose.runOnUiThread { player.setMediaItems(queue.items, queue.index, queue.positionMs) }
        compose.waitForIdle()
    }

    // Review Focus 1.
    @Test fun `nothing queued, no entry`() {
        show()
        assertNull(entry)
    }

    @Test fun `a queued book gives its title, chapter and play state`() {
        show()
        queue(startAt = 2000.0)
        assertEquals("A Book", entry!!.title)
        assertEquals("Two", entry!!.chapter)
        assertEquals(false, entry!!.playing)
        assertEquals(listOf("b1"), asked)
    }

    // Review Focus 1: Back in the player clears the queue.
    @Test fun `the entry goes when the queue is cleared`() {
        show()
        queue(startAt = 0.0)
        compose.runOnUiThread { player.clearMediaItems() }
        compose.waitForIdle()
        assertNull(entry)
    }

    @Test fun `no player, no entry`() {
        player = TestExoPlayerBuilder(compose.activity).build()
        compose.setContent { entry = rememberRailEntry(null) { null } }
        compose.waitForIdle()
        assertNull(entry)
    }
}
