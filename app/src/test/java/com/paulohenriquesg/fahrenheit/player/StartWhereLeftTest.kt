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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An episode moved on to by itself starts where it was left (#108), from its
 * first rendered moment: no second of its beginning first (#171).
 */
@RunWith(AndroidJUnit4::class)
class StartWhereLeftTest {
    private val player: ExoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
        .setMediaSourceFactory(StartWhereLeft(hourLongFiles()))
        .build()
    private val hour = TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/e")))
    private val twoHours = TrackTimeline(
        listOf(TimelineTrack(1, 0.0, 3600.0, "/e2a"), TimelineTrack(2, 3600.0, 3600.0, "/e2b"))
    )
    private val e1 = NowPlaying("p1", "e1", hour, null, null, "e1", true, null)
    private val resolve: (String) -> String? = { "https://abs.test$it" }

    /** Where the player stood at each automatic move, and every seek after the first. */
    private val arrivals = mutableListOf<Pair<Int, Long>>()
    private val seeks = mutableListOf<Long>()

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    arrivals += player.currentMediaItemIndex to player.currentPosition
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) seeks += newPosition.positionMs
            }
        })
    }

    @After
    fun tearDown() = player.release()

    private fun queueWithNext(next: TrackTimeline = hour, startAt: Double) {
        val e2 = NowPlaying("p1", "e2", next, null, null, "e2", true, null)
        val items = PlaybackQueue.itemsOf(e1, resolve)!! + PlaybackQueue.itemsOf(e2, resolve, startAt = startAt)!!
        player.setMediaItems(items, 0, 3_595_000L)
        player.prepare()
        player.play()
    }

    @Test
    fun `moving on, the next episode starts at its saved place with no seek after`() {
        queueWithNext(startAt = 600.0)
        run(player).untilPositionAtLeast(1, 601_000)

        val (index, at) = arrivals.single()
        assertEquals(1, index)
        assertTrue("arrived at $at", at >= 600_000L)
        assertTrue("seeks $seeks", seeks.isEmpty())
    }

    @Test
    fun `an episode never started starts at the beginning`() {
        queueWithNext(startAt = 0.0)
        run(player).untilPositionAtLeast(1, 1)

        assertTrue("arrived at ${arrivals.single().second}", arrivals.single().second < 1_000L)
    }

    @Test
    fun `an episode resumed in its second file starts there`() {
        queueWithNext(next = twoHours, startAt = 4200.0)
        run(player).untilPositionAtLeast(2, 601_000)

        assertEquals("arrivals $arrivals", 2, player.currentMediaItemIndex)
        val (index, at) = arrivals.last()
        assertEquals(2, index)
        assertTrue("arrived at $at", at in 600_000L..601_000L)
    }

    @Test
    fun `seeking back before the saved place still reaches it`() {
        queueWithNext(startAt = 600.0)
        run(player).untilPositionAtLeast(1, 601_000)

        player.seekTo(1, 10_000)
        run(player).untilPendingCommandsAreFullyHandled()

        assertTrue("at ${player.currentPosition}", player.currentPosition in 10_000L..11_000L)
    }

    @Test
    fun `reports close the finished episode at its end and the next from its saved place`() {
        val reports = mutableListOf<Pair<QueuedFile, ListeningReport>>()
        player.addListener(
            PlaybackReporting(
                player,
                CoroutineScope(Dispatchers.Unconfined),
                open = { file ->
                    object : ListeningDelivery {
                        override suspend fun sync(report: ListeningReport) {
                            reports += file to report
                        }

                        override suspend fun close(report: ListeningReport?) {
                            report?.let { sync(it) }
                        }
                    }
                },
                pause = { awaitCancellation() },
                now = { player.clock.elapsedRealtime() }
            )
        )
        queueWithNext(startAt = 600.0)
        run(player).untilPositionAtLeast(1, 605_000)

        player.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val first = reports.filter { it.first.episodeId == "e1" }.map { it.second }
        val second = reports.filter { it.first.episodeId == "e2" }.map { it.second }
        assertEquals("e1: $first", 3600.0, first.single().currentTime, 1.0)
        assertEquals("e2: $second", 605.0, second.single().currentTime, 1.0)
        val heard = second.single().timeListened
        assertTrue("heard $heard s", heard in 4.0..6.5)
    }
}
