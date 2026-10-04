package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
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

    /** Set to hold the answer back, as a slow server would. */
    private var answer: CompletableDeferred<Unit>? = null

    private lateinit var queue: NextEpisodeQueue

    private fun listen() {
        queue = NextEpisodeQueue(player, scope, enabled = { enabled }) { file ->
            asked += file.episodeId!!
            answer?.await()
            after[file.episodeId]?.let(::itemsOf)
        }
        player.addListener(queue)
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

    // Review: the setting turned off while an episode plays drops the next at
    // once; before, it played anyway, and only the one after was dropped.
    @Test
    fun `turning the setting off drops the queued next at once`() {
        listen()
        playingNearTheEndOf("e1", "e2")
        run(player).untilPendingCommandsAreFullyHandled()

        enabled = false
        queue.settingChanged()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(listOf("e1"), queued())
    }

    @Test
    fun `turning the setting on queues the next at once`() {
        enabled = false
        listen()
        playingNearTheEndOf("e1")
        run(player).untilPendingCommandsAreFullyHandled()

        enabled = true
        queue.settingChanged()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(listOf("e1", "e2"), queued())
    }

    @Test
    fun `turning it on with a book playing queues nothing`() {
        enabled = false
        listen()
        player.setMediaItems(PlaybackQueue.itemsOf(NowPlaying("b1", "A Book", hour, null, null, null, false, null), resolve)!!)
        player.prepare()
        run(player).untilPendingCommandsAreFullyHandled()

        enabled = true
        queue.settingChanged()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(1, player.mediaItemCount)
        assertEquals(emptyList<String>(), asked)
    }

    // Review: the player screen, open, queues the next too, through its own
    // controller's view of the queue, which can lag: the second copy goes.
    @Test
    fun `a next episode queued twice is kept once`() {
        listen()
        playingNearTheEndOf("e1", "e2")
        run(player).untilPendingCommandsAreFullyHandled()

        player.addMediaItems(itemsOf("e2"))
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(listOf("e1", "e2"), queued())
    }

    // Review: what the server says arrives later; by then the queue may be another.
    @Test
    fun `a queue replaced while asking gets nothing added`() {
        answer = CompletableDeferred()
        listen()
        playingNearTheEndOf("e1", "e2")
        moveOn(to = 1)
        assertEquals(listOf("e2"), asked)

        player.setMediaItems(itemsOf("e4"))
        run(player).untilPendingCommandsAreFullyHandled()
        answer!!.complete(Unit)
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(listOf("e4"), queued())
    }
}
