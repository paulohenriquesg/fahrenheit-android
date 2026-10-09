package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
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
    private val reports = mutableListOf<Pair<QueuedFile, ListeningReport>>()

    /** Stands in for the item's listening session, recording what reaches it. */
    private inner class Recorder(val file: QueuedFile) : ListeningDelivery {
        override suspend fun sync(report: ListeningReport) {
            reports += file to report
            sent += file to report.currentTime
        }

        override suspend fun close(report: ListeningReport?) {
            report?.let { sync(it) }
        }
    }

    private fun nowPlaying(itemId: String, timeline: TrackTimeline, episodeId: String? = null) = NowPlaying(
        itemId = itemId, title = itemId, timeline = timeline, mediaDuration = null, chapters = null,
        episodeId = episodeId, goToPodcast = episodeId != null, description = null
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
            open = { Recorder(it) },
            // Rounds never come round: only the closing reports are under test.
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() }
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

    // Review: on Back the service is destroyed within milliseconds of the
    // stop, cancelling its scope - and with it the closing report, mid-send.
    @Test
    fun `the closing report survives the service going away`() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val delivered = mutableListOf<Double>()
        val network = CompletableDeferred<Unit>()
        val reporting = PlaybackReporting(
            player,
            scope,
            open = { _ ->
                object : ListeningDelivery {
                    override suspend fun sync(report: ListeningReport) = Unit
                    override suspend fun close(report: ListeningReport?) {
                        network.await()
                        report?.let { delivered += it.currentTime }
                    }
                }
            },
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() }
        )
        player.addListener(reporting)
        val guard = LeavingGuard(player, reporting::beforeLeaving)
        val queue = PlaybackQueue.of(nowPlaying("b1", twoParts), 3602.0) { "https://abs.test$it" }!!
        guard.setMediaItems(queue.items, queue.index, queue.positionMs)
        guard.prepare()
        guard.play()
        run(player).untilPositionAtLeast(5_000)

        guard.stop()
        scope.cancel()
        network.complete(Unit)

        assertEquals(3605.0, delivered.single(), 0.5)
    }

    // Review Focus 3.
    @Test
    fun `listening time is what was played, not how far it moved`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)
        guarded.seekTo(1, 600_000)
        run(player).untilPositionAtLeast(602_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val heard = reports.single().second.timeListened
        assertTrue("heard $heard s", heard in 4.0..6.5)
    }

    @Test
    fun `pausing closes, and playing on reports again`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)
        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        guarded.play()
        run(player).untilPositionAtLeast(8_000)
        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(2, reports.size)
    }

    // Review: buffering is not a stop, but a pause during it is. The player
    // already reads as not playing, so only the pause itself can say so.
    @Test
    fun `pausing while buffering still closes`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)
        guarded.seekTo(1, 600_000)
        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(1, reports.size)
    }

    // Review (#108): Media3 moves on to the next episode without isPlaying
    // changing, so the last one was never closed and the next never reported.
    @Test
    fun `moving on by itself closes the last episode at its end, and reports the next`() {
        val hour = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/e")))
        val e1 = nowPlaying("p1", hour, episodeId = "e1")
        val e2 = nowPlaying("p1", hour, episodeId = "e2")
        val queue = PlaybackQueue.of(e1, 3595.0, { "https://abs.test$it" }, next = e2)!!
        guarded.setMediaItems(queue.items, queue.index, queue.positionMs)
        guarded.prepare()
        guarded.play()
        run(player).untilPositionAtLeast(1, 5_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val first = sent.filter { it.first.episodeId == "e1" }
        val second = sent.filter { it.first.episodeId == "e2" }
        assertEquals("e1 closed at its end: $sent", 3600.0, first.single().second, 1.0)
        assertTrue("e2 reported: $sent", second.isNotEmpty() && second.last().second >= 5.0)
    }

    // #207: the store reads the item back from the server once its stop is there,
    // since whether the end finished it is the server's rule.
    @Test
    fun `a stretch's closing report done, the service is told which item`() {
        val closed = mutableListOf<QueuedFile>()
        val network = CompletableDeferred<Unit>()
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            open = { _ ->
                object : ListeningDelivery {
                    override suspend fun sync(report: ListeningReport) = Unit
                    override suspend fun close(report: ListeningReport?) = network.await()
                }
            },
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() },
            closed = { closed += it }
        )
        player.addListener(reporting)
        val guard = LeavingGuard(player, reporting::beforeLeaving)
        val queue = PlaybackQueue.of(nowPlaying("p1", oneFile, episodeId = "e1"), 0.0) { "https://abs.test$it" }!!
        guard.setMediaItems(queue.items, queue.index, queue.positionMs)
        guard.prepare()
        guard.play()
        run(player).untilPositionAtLeast(3_000)

        guard.pause()
        run(player).untilPendingCommandsAreFullyHandled()
        assertEquals("not while the report is out", emptyList<QueuedFile>(), closed)

        network.complete(Unit)
        assertEquals(listOf("p1" to "e1"), closed.map { it.itemId to it.episodeId })
    }

    // Review: at the end of the queue the service stops itself and its scope
    // is cancelled while the closing report is still out.
    @Test
    fun `told of the close even when the service goes while the report is out`() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val closed = mutableListOf<QueuedFile>()
        val network = CompletableDeferred<Unit>()
        val reporting = PlaybackReporting(
            player,
            scope,
            open = { _ ->
                object : ListeningDelivery {
                    override suspend fun sync(report: ListeningReport) = Unit
                    override suspend fun close(report: ListeningReport?) = network.await()
                }
            },
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() },
            closed = { closed += it }
        )
        player.addListener(reporting)
        val guard = LeavingGuard(player, reporting::beforeLeaving)
        val queue = PlaybackQueue.of(nowPlaying("p1", oneFile, episodeId = "e1"), 0.0) { "https://abs.test$it" }!!
        guard.setMediaItems(queue.items, queue.index, queue.positionMs)
        guard.prepare()
        guard.play()
        run(player).untilPositionAtLeast(3_000)

        guard.stop()
        scope.cancel()
        network.complete(Unit)

        assertEquals(listOf("e1"), closed.map { it.episodeId })
    }
}
