package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Playing to the end of the queue ends the listening session, as Stop does
 * (#179): put together as the service puts it, reporting and guard included,
 * so the closing report is seen to go once, at the end.
 */
@RunWith(AndroidJUnit4::class)
class QueueEndTest {

    private lateinit var player: ExoPlayer
    private lateinit var guarded: Player
    private val closings = mutableListOf<Pair<QueuedFile, Double?>>()

    /** Stands in for the item's listening session, recording how it closes. */
    private inner class Recorder(val file: QueuedFile) : ListeningDelivery {
        override suspend fun sync(report: ListeningReport) = Unit

        override suspend fun close(report: ListeningReport?) {
            closings += file to report?.currentTime
        }
    }

    private fun episode(id: String) = NowPlaying(
        itemId = "p1", title = id,
        timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/$id"))),
        mediaDuration = null, chapters = null, episodeId = id, goToPodcast = true, description = null
    )

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            open = { Recorder(it) },
            // Rounds never come round: only the closing reports are under test.
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() },
            closings = Closings()
        )
        player.addListener(reporting)
        guarded = LeavingGuard(player, reporting::beforeLeaving)
        player.addListener(QueueEnd(guarded))
    }

    @After
    fun tearDown() = player.release()

    @Test
    fun `the last episode played to its end ends the session, reported once at its end`() {
        val queue = PlaybackQueue.of(episode("e1"), 3595.0) { "https://abs.test$it" }!!
        guarded.setMediaItems(queue.items, queue.index, queue.positionMs)
        guarded.prepare()
        guarded.play()

        run(player).untilState(Player.STATE_IDLE)
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(0, player.mediaItemCount)
        val (file, at) = closings.single()
        assertEquals("e1", file.episodeId)
        assertEquals(3600.0, at!!, 0.5)
    }

    // Auto-advance (#108): Media3 moves on by itself, and never reaches the end.
    @Test
    fun `an episode with the next queued moves on, and nothing ends`() {
        val queue = PlaybackQueue.of(episode("e1"), 3595.0) { "https://abs.test$it" }!!
        val next = PlaybackQueue.itemsOf(episode("e2"), { "https://abs.test$it" })!!
        guarded.setMediaItems(queue.items + next, queue.index, queue.positionMs)
        guarded.prepare()
        guarded.play()

        run(player).untilPositionAtLeast(1, 2_000)

        assertEquals(2, player.mediaItemCount)
        assertEquals("e2", QueuedFile.of(player.currentMediaItem)!!.episodeId)
        assertEquals(Player.STATE_READY, player.playbackState)
    }
}
