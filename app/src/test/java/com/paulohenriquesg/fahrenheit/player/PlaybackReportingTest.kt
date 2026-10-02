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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The service reports whatever is playing, in whole-book time, to the item it
 * belongs to - and has a last word before a stop or a new queue.
 *
 * Fake media ([hourLongFiles]): book time comes from each file's startOffset,
 * so what the fake files contain does not matter.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackReportingTest {

    private lateinit var player: ExoPlayer
    private lateinit var guarded: Player
    private val sent = mutableListOf<Pair<QueuedFile, Double>>()

    private fun nowPlaying(itemId: String, timeline: TrackTimeline, episodeId: String? = null) = NowPlaying(
        itemId = itemId, title = itemId, timeline = timeline, mediaDuration = null, chapters = null,
        episodeId = episodeId, goToPodcast = episodeId != null, description = null, line = { "" }
    )

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )
    private val oneFile = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/ep")))

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            send = { file, request -> sent += file to request.currentTime!! },
            // Rounds never come round: only the closing reports are under test.
            pause = { awaitCancellation() }
        )
        player.addListener(reporting)
        guarded = LeavingGuard(player, reporting::beforeLeaving)
    }

    @After
    fun tearDown() = player.release()

    private fun queue(nowPlaying: NowPlaying, startAt: Double) {
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        guarded.setMediaItems(queue.items, queue.index, queue.positionMs)
        guarded.prepare()
    }

    private fun playUntil(positionMs: Long) {
        guarded.play()
        run(player).untilPositionAtLeast(positionMs)
    }

    @Test
    fun `pausing reports the whole-book position to the book`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val (file, time) = sent.single()
        assertEquals("b1", file.itemId)
        assertEquals(3605.0, time, 0.5)
    }

    @Test
    fun `stopping reports where it was, not where stopping leaves it`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)

        guarded.stop()
        guarded.clearMediaItems()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(3605.0, sent.single().second, 0.5)
    }

    // Review Focus 2.
    @Test
    fun `replacing one book with another reports the first to the first`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)

        queue(nowPlaying("b2", twoParts), startAt = 0.0)
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(listOf("b1"), sent.map { it.first.itemId })
    }

    // Review Focus 1.
    @Test
    fun `stopping something never played sends nothing`() {
        queue(nowPlaying("b1", twoParts), startAt = 900.0)

        guarded.stop()
        guarded.clearMediaItems()
        run(player).untilPendingCommandsAreFullyHandled()

        assertTrue(sent.isEmpty())
    }

    @Test
    fun `an episode reports to its episode`() {
        queue(nowPlaying("p1", oneFile, episodeId = "e1"), startAt = 0.0)
        playUntil(3_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val (file, time) = sent.single()
        assertEquals("e1", file.episodeId)
        assertEquals(3.0, time, 0.5)
    }
}
