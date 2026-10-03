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
import kotlinx.coroutines.runBlocking
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
        NowPlaying(id, id, timeline, null, null, null, false, null)

    private fun episode(id: String) =
        NowPlaying("p1", id, TrackTimeline(listOf(TimelineTrack(1, 0.0, 1800.0, "/$id"))), null, null, id, true, null)

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
        assertTrue(runBlocking { PlayerStart(autoPlay = false).begin(player, book("b1"), { savedAt(4500.0) }, resolve) })

        assertEquals(2, player.mediaItemCount)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `opened to play, it plays`() {
        runBlocking { PlayerStart(autoPlay = true).begin(player, book("b1"), { null }, resolve) }

        assertTrue(player.playWhenReady)
    }

    // Review Focus 3.
    @Test
    fun `the book already playing is left where it is`() {
        runBlocking { PlayerStart(autoPlay = true).begin(player, book("b1"), { savedAt(4500.0) }, resolve) }

        // Back to the screen: the server's copy is older than what is playing.
        assertTrue(runBlocking { PlayerStart(autoPlay = false).begin(player, book("b1"), { savedAt(100.0) }, resolve) })

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `another book replaces what was queued`() {
        runBlocking { PlayerStart(autoPlay = false).begin(player, book("b1"), { savedAt(4500.0) }, resolve) }

        runBlocking { PlayerStart(autoPlay = false).begin(player, book("b2"), { null }, resolve) }

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("b2", null))
        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    fun `another episode of the same podcast replaces the one queued`() {
        runBlocking { PlayerStart(autoPlay = false).begin(player, episode("e1"), { null }, resolve) }

        runBlocking { PlayerStart(autoPlay = false).begin(player, episode("e2"), { null }, resolve) }

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("p1", "e2"))
    }

    @Test
    fun `nothing to play leaves the queue alone`() {
        runBlocking { PlayerStart(autoPlay = false).begin(player, book("b1"), { null }, resolve) }

        assertFalse(runBlocking { PlayerStart(autoPlay = false).begin(player, book("b2", timeline = null), { null }, resolve) })

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("b1", null))
    }

    // Review: Back from another screen to a player whose book is no longer
    // queued re-queued it from the position read when the screen first
    // opened, and played it - writing that stale position over the real one.
    @Test
    fun `coming back to a book no longer queued re-reads its position and does not play`() = runBlocking {
        val start = PlayerStart(autoPlay = true)
        start.begin(player, book("b1"), { savedAt(600.0) }, resolve)
        assertTrue(player.playWhenReady)
        // Listened elsewhere meanwhile; the queue was stopped and emptied.
        player.stop()
        player.clearMediaItems()

        start.begin(player, book("b1"), { savedAt(2400.0) }, resolve)

        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(2_400_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `a book queued without play waits, even over one that was playing`() = runBlocking {
        PlayerStart(autoPlay = true).begin(player, book("b1"), { null }, resolve)

        PlayerStart(autoPlay = false).begin(player, book("b2"), { null }, resolve)

        assertFalse(player.playWhenReady)
    }

    @Test
    fun `the saved position is not asked for when reattaching`() = runBlocking {
        PlayerStart(autoPlay = false).begin(player, book("b1"), { savedAt(4500.0) }, resolve)
        var asked = false

        PlayerStart(autoPlay = false).begin(player, book("b1"), { asked = true; null }, resolve)

        assertFalse(asked)
    }

    // #105: the screen that opened the player asked for this place.
    @Test
    fun `a start position wins over the saved one`() {
        assertTrue(runBlocking { PlayerStart(autoPlay = true, startAt = 3900.0).begin(player, book("b1"), { savedAt(100.0) }, resolve) })

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(300_000L, player.currentPosition)
        assertTrue(player.playWhenReady)
    }

    // Review Focus 1: Home left it playing, then a chapter was chosen on its details screen.
    @Test
    fun `a book already queued moves to the start position, and plays`() {
        runBlocking { PlayerStart(autoPlay = false).begin(player, book("b1"), { savedAt(100.0) }, resolve) }

        runBlocking { PlayerStart(autoPlay = true, startAt = 3900.0).begin(player, book("b1"), { savedAt(100.0) }, resolve) }

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(300_000L, player.currentPosition)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `the start position is honoured once`() {
        val start = PlayerStart(autoPlay = true, startAt = 3900.0)
        runBlocking { start.begin(player, book("b1"), { savedAt(100.0) }, resolve) }
        player.seekTo(0, 5_000L)

        runBlocking { start.begin(player, book("b1"), { savedAt(100.0) }, resolve) }

        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(5_000L, player.currentPosition)
    }

    private fun episodeIn(id: String) =
        NowPlaying("p1", id, TrackTimeline(listOf(TimelineTrack(1, 0.0, 1800.0, "/$id"))), null, null, id, true, null)

    @Test
    fun `with a next episode, it is queued after the one asked for`() {
        runBlocking { PlayerStart(autoPlay = false).begin(player, episodeIn("e1"), { null }, resolve, next = episodeIn("e2")) }

        assertEquals(2, player.mediaItemCount)
        assertEquals(QueuedFile("p1", "e2", 0.0, 1800.0), QueuedFile.of(player.getMediaItemAt(1)))
    }

    // The screen followed a move to e2: it reattaches, and queues e3 behind it, once.
    @Test
    fun `reattaching queues the next episode behind, once`() {
        runBlocking { PlayerStart(autoPlay = false).begin(player, episodeIn("e2"), { null }, resolve) }
        player.seekTo(0, 5_000L)

        val start = PlayerStart(autoPlay = false)
        runBlocking { start.begin(player, episodeIn("e2"), { null }, resolve, next = episodeIn("e3")) }
        runBlocking { start.begin(player, episodeIn("e2"), { null }, resolve, next = episodeIn("e3")) }

        assertEquals(2, player.mediaItemCount)
        assertEquals(5_000L, player.currentPosition)
        assertEquals(QueuedFile("p1", "e3", 0.0, 1800.0), QueuedFile.of(player.getMediaItemAt(1)))
    }

    // #108: the screen followed a move from e1 to e2; e1 has played, and e2
    // should be first again so the screen's timeline maps onto the queue.
    @Test
    fun `reattaching after a move drops what has already played`() {
        val queue = PlaybackQueue.of(episodeIn("e1"), 0.0, next = episodeIn("e2"), resolveUrl = resolve)!!
        player.setMediaItems(queue.items, 1, 5_000L)

        runBlocking { PlayerStart(autoPlay = false).begin(player, episodeIn("e2"), { null }, resolve) }

        assertEquals(1, player.mediaItemCount)
        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(5_000L, player.currentPosition)
        assertEquals(QueuedFile("p1", "e2", 0.0, 1800.0), QueuedFile.of(player.currentMediaItem))
    }
}
