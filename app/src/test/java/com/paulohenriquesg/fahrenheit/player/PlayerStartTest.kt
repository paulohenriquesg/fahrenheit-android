package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** What the player screen does with the service it finds: reattach, or load. */
@RunWith(AndroidJUnit4::class)
class PlayerStartTest {

    private lateinit var player: ExoPlayer

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )

    private fun book(id: String, timeline: TrackTimeline? = twoParts) =
        NowPlaying(id, id, timeline, null, null, null, false, null) { "" }

    private fun episode(id: String) =
        NowPlaying("p1", id, TrackTimeline(listOf(TimelineTrack(1, 0.0, 1800.0, "/$id"))), null, null, id, true, null) { "" }

    private fun savedAt(seconds: Double) = MediaProgressResponse(currentTime = seconds)

    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
    }

    @After
    fun tearDown() = player.release()

    @Test
    fun `a book not yet queued starts at its saved position, waiting for play`() {
        assertTrue(PlayerStart.begin(player, book("b1"), savedAt(4500.0), autoPlay = false, resolve))

        assertEquals(2, player.mediaItemCount)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `opened to play, it plays`() {
        PlayerStart.begin(player, book("b1"), null, autoPlay = true, resolve)

        assertTrue(player.playWhenReady)
    }

    // Review Focus 3.
    @Test
    fun `the book already playing is left where it is`() {
        PlayerStart.begin(player, book("b1"), savedAt(4500.0), autoPlay = true, resolve)

        // Back to the screen: the server's copy is older than what is playing.
        assertTrue(PlayerStart.begin(player, book("b1"), savedAt(100.0), autoPlay = false, resolve))

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `another book replaces what was queued`() {
        PlayerStart.begin(player, book("b1"), savedAt(4500.0), autoPlay = false, resolve)

        PlayerStart.begin(player, book("b2"), null, autoPlay = false, resolve)

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("b2", null))
        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    fun `another episode of the same podcast replaces the one queued`() {
        PlayerStart.begin(player, episode("e1"), null, autoPlay = false, resolve)

        PlayerStart.begin(player, episode("e2"), null, autoPlay = false, resolve)

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("p1", "e2"))
    }

    @Test
    fun `nothing to play leaves the queue alone`() {
        PlayerStart.begin(player, book("b1"), null, autoPlay = false, resolve)

        assertFalse(PlayerStart.begin(player, book("b2", timeline = null), null, autoPlay = false, resolve))

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("b1", null))
    }
}
