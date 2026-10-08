package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.After
import org.junit.Before
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

    // The chapters are kept for the process; each test starts without them.
    @Before
    fun forget() = RailChapters.known.clear()

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
        // The test player tells its listeners on its own clock: let it.
        run(player).untilPendingCommandsAreFullyHandled()
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

    // Stop on the entry clears the queue (#155).
    @Test fun `the entry goes when the queue is cleared`() {
        show()
        queue(startAt = 0.0)
        compose.runOnUiThread { player.clearMediaItems() }
        run(player).untilPendingCommandsAreFullyHandled()
        compose.waitForIdle()
        assertNull(entry)
    }

    // #179: the player keeps wanting to play after the end; the entry says it stopped.
    @Test fun `played to its end, the entry is not playing`() {
        show()
        queue(startAt = 3595.0)
        compose.runOnUiThread {
            player.prepare()
            player.play()
        }
        run(player).untilState(Player.STATE_ENDED)
        compose.waitForIdle()
        assertEquals(false, entry!!.playing)
    }

    @Test fun `no player, no entry`() {
        player = TestExoPlayerBuilder(compose.activity).build()
        compose.setContent { entry = rememberRailEntry(null) { null } }
        compose.waitForIdle()
        assertNull(entry)
    }

    // Review (#107): each return to the main screen brings a new controller,
    // and a new composition of the entry.
    @Test fun `a book's chapters are asked for once, across returns to the screen`() {
        player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        var visit by mutableStateOf(0)
        compose.setContent {
            key(visit) {
                entry = rememberRailEntry(player) { itemId ->
                    asked += itemId
                    listOf(Chapter(start = 0.0, end = 1800.0, title = "One"), Chapter(start = 1800.0, end = 3600.0, title = "Two"))
                }
            }
        }
        queue(startAt = 2000.0)

        visit = 1
        compose.waitForIdle()

        assertEquals(listOf("b1"), asked)
        assertEquals("Two", entry!!.chapter)
    }
}
