package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The playback service queues the next episode after each move by itself,
 * with no screen open (#160): before, only the player screen did, so with it
 * closed playback stopped one episode later than it should.
 */
@RunWith(AndroidJUnit4::class)
class NextEpisodeQueueTest {
    private val player: ExoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
        .setMediaSourceFactory(hourLongFiles())
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val hour = TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/e")))
    private val resolve: (String) -> String? = { "https://abs.test$it" }
    private fun episode(id: String) = NowPlaying("p1", id, hour, null, null, id, true, null)
    private fun itemsOf(id: String): List<MediaItem> = PlaybackQueue.itemsOf(episode(id), resolve)!!

    /** e1 -> e2 -> e3 -> e4, each newer than the last. */
    private val after = mapOf("e1" to "e2", "e2" to "e3", "e3" to "e4")
    private val asked = mutableListOf<String>()
    private var enabled = true

    @After
    fun tearDown() {
        scope.cancel()
        player.release()
    }

    private fun listen() {
        player.addListener(
            NextEpisodeQueue(player, scope, enabled = { enabled }) { file ->
                asked += file.episodeId!!
                after[file.episodeId]?.let(::itemsOf)
            }
        )
    }

    /** The screen queued e1 and e2, and closed; e1 is nearly over. */
    private fun playingNearTheEndOf(vararg ids: String) {
        player.setMediaItems(ids.flatMap(::itemsOf), 0, 3_595_000L)
        player.prepare()
        player.play()
    }

    private fun moveOn(to: Int) {
        run(player).untilPositionAtLeast(to, 1)
        run(player).untilPendingCommandsAreFullyHandled()
    }

    private fun queued() = (0 until player.mediaItemCount).map { QueuedFile.of(player.getMediaItemAt(it))!!.episodeId }

    @Test
    fun `two moves in a row each queue the following episode`() {
        listen()
        playingNearTheEndOf("e1", "e2")

        moveOn(to = 1)
        assertEquals(listOf("e1", "e2", "e3"), queued())

        player.seekTo(1, 3_595_000L)
        moveOn(to = 2)
        assertEquals(listOf("e1", "e2", "e3", "e4"), queued())
        assertEquals(listOf("e2", "e3"), asked)
    }

    @Test
    fun `setting off, nothing more is queued`() {
        enabled = false
        listen()
        playingNearTheEndOf("e1", "e2")

        moveOn(to = 1)

        assertEquals(listOf("e1", "e2"), queued())
        assertEquals(emptyList<String>(), asked)
    }

    // Queued while the setting was on, turned off since.
    @Test
    fun `setting off drops a next episode queued before`() {
        enabled = false
        listen()
        playingNearTheEndOf("e1", "e2", "e3")

        moveOn(to = 1)

        assertEquals(listOf("e1", "e2"), queued())
    }

    // The player screen, open, queues it too.
    @Test
    fun `a next episode queued already is not queued twice`() {
        listen()
        playingNearTheEndOf("e1", "e2", "e3")

        moveOn(to = 1)

        assertEquals(listOf("e1", "e2", "e3"), queued())
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `a book's next file asks for nothing`() {
        listen()
        val book = NowPlaying(
            "b1", "A Book",
            TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/p1"), TimelineTrack(2, 3600.0, 3600.0, "/p2"))),
            null, null, null, false, null
        )
        player.setMediaItems(PlaybackQueue.itemsOf(book, resolve)!!, 0, 3_595_000L)
        player.prepare()
        player.play()

        moveOn(to = 1)

        assertEquals(2, player.mediaItemCount)
        assertEquals(emptyList<String>(), asked)
    }
}
