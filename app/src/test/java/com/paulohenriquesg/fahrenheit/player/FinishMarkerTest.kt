package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mark finished and unfinished, as the playback service does them (#107),
 * against a server that keeps the real rules:
 * - any update that moves currentTime un-finishes a finished item, even the
 *   one that finishes it;
 * - un-finishing puts currentTime back to 0.
 */
@RunWith(AndroidJUnit4::class)
class FinishMarkerTest {

    /** The server's progress for one item, by its rules. */
    private class Server(var currentTime: Double = 0.0, var isFinished: Boolean = false, val duration: Double = 5400.0) {
        val marks = mutableListOf<ProgressMark>()

        fun apply(mark: ProgressMark) {
            marks += mark
            var time = mark.currentTime
            var moved = false
            if (mark.isFinished == false && isFinished) {
                currentTime = 0.0
                moved = true
                time = null
            }
            mark.isFinished?.let { isFinished = it }
            if (time != null && time != currentTime) {
                currentTime = time
                moved = true
            }
            val nearTheEnd = duration - currentTime < 10
            if (!isFinished && nearTheEnd) isFinished = true
            else if (isFinished && moved && !nearTheEnd) isFinished = false
        }
    }

    private val server = Server()
    private val closing = CompletableDeferred<Unit>()
    /** This test's closing reports in flight; the app keeps one set for the process. */
    private val closings = Closings()
    private lateinit var player: ExoPlayer
    private lateinit var reporting: PlaybackReporting

    /** The item's listening session: reports move the server's position; a close waits for [closing]. */
    private inner class Session : ListeningDelivery {
        override suspend fun sync(report: ListeningReport) = server.apply(ProgressMark(currentTime = report.currentTime))
        override suspend fun close(report: ListeningReport?) {
            closing.await()
            report?.let { server.apply(ProgressMark(currentTime = it.currentTime)) }
        }
    }

    private val book = NowPlaying(
        "b1", "b1",
        TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 5400.0, contentUrl = "/b1"))),
        null, null, null, false, null
    )

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext()).setMediaSourceFactory(hourLongFiles()).build()
        reporting = PlaybackReporting(
            player, CoroutineScope(Dispatchers.Unconfined), open = { Session() },
            pause = { awaitCancellation() }, now = { player.clock.elapsedRealtime() }, closings = closings
        )
        player.addListener(reporting)
        val queue = PlaybackQueue.of(book, 300.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
    }

    @After
    fun tearDown() = player.release()

    /** Which item each mark went to. */
    private val markedFor = mutableListOf<String>()

    private fun marker() = FinishMarker(player, reporting::closeAndWait, pending = closings::settled) { itemId, _, mark ->
        markedFor += itemId
        server.apply(mark)
    }

    // Review: {isFinished, currentTime} un-finished itself, and a late close undid it again.
    @Test
    fun `marked finished while playing stays finished, though the closing report lands late`() = runBlocking {
        player.play()
        run(player).untilPositionAtLeast(305_000)

        val marking = async(Dispatchers.Unconfined) { marker().mark(finished = true) }
        assertFalse("nothing is marked before the close is delivered", server.isFinished)
        closing.complete(Unit)

        assertTrue(marking.await().isSuccess)
        assertFalse(player.playWhenReady)
        assertTrue(server.isFinished)
        assertEquals(ProgressMark(isFinished = true), server.marks.last())
    }

    // Review: un-finishing put the server's position to 0, and nothing set it back.
    @Test
    fun `unfinished keeps the listener's place`() = runBlocking {
        closing.complete(Unit)
        server.isFinished = true
        server.currentTime = 1000.0

        assertTrue(marker().mark(finished = false).isSuccess)

        assertFalse(server.isFinished)
        assertEquals(300.0, server.currentTime, 0.001)
    }

    // Review (#145): un-finishing writes 0, then the place; the check must know
    // both, or a failed second write reads as someone else's 0:00.
    @Test
    fun `unfinishing knows the positions it wrote`() = runBlocking {
        closing.complete(Unit)
        server.isFinished = true
        val written = mutableListOf<Double>()
        val m = FinishMarker(player, reporting::closeAndWait, pending = closings::settled, wrote = { _, _, at -> written += at }) { _, _, mark ->
            server.apply(mark)
        }

        assertTrue(m.mark(finished = false).isSuccess)

        assertEquals(listOf(0.0, 300.0), written)
    }

    @Test
    fun `a failed second write leaves the 0 known`() = runBlocking {
        closing.complete(Unit)
        val written = mutableListOf<Double>()
        var sends = 0
        val m = FinishMarker(player, reporting::closeAndWait, pending = closings::settled, wrote = { _, _, at -> written += at }) { _, _, _ ->
            if (sends++ == 1) error("offline")
        }

        assertFalse(m.mark(finished = false).isSuccess)

        assertEquals(listOf(0.0), written)
    }

    @Test
    fun `nothing queued, nothing to mark`() = runBlocking {
        player.clearMediaItems()
        assertTrue(marker().mark(finished = true).isFailure)
        assertEquals(emptyList<ProgressMark>(), server.marks)
    }

    @Test
    fun `a failed request is a failure`() = runBlocking {
        closing.complete(Unit)
        val failing = FinishMarker(player, reporting::closeAndWait) { _, _, _ -> error("offline") }
        assertTrue(failing.mark(finished = true).isFailure)
    }

    // #105, Review Focus 2: Mark finished on another book's details screen.
    @Test
    fun `another item is marked directly, and what plays keeps playing`() = runBlocking {
        player.play()
        run(player).untilPositionAtLeast(301_000)

        // Bounded: a marker that waited for this book's closing report would wait for ever.
        val result = withTimeoutOrNull(5_000) { marker().mark(finished = true, itemId = "b9", episodeId = null) }

        assertTrue("marked without waiting on what plays", result?.isSuccess == true)
        assertTrue(player.playWhenReady)
        assertEquals(listOf("b9"), markedFor)
        assertEquals(listOf(ProgressMark(isFinished = true)), server.marks)
    }

    @Test
    fun `naming the item that is queued marks it as from the player`() = runBlocking {
        closing.complete(Unit)
        player.play()
        run(player).untilPositionAtLeast(301_000)

        assertTrue(marker().mark(finished = true, itemId = "b1", episodeId = null).isSuccess)

        assertFalse(player.playWhenReady)
        assertEquals(listOf("b1"), markedFor)
    }

    @Test
    fun `nothing queued, a named item is still marked`() = runBlocking {
        player.clearMediaItems()
        assertTrue(marker().mark(finished = false, itemId = "b9", episodeId = null).isSuccess)
        assertEquals(listOf(ProgressMark(isFinished = false)), server.marks)
    }

    @Test
    fun `the command names its item`() {
        val args = FinishCommand.args(true, itemId = "b9")
        assertEquals(true, FinishCommand.finishedOf(args))
        assertEquals("b9", FinishCommand.itemOf(args))
        assertEquals(null, FinishCommand.itemOf(FinishCommand.args(false)))
    }

    // Review: Back stopped b1 and its close was still on its way when the
    // details screen marked it; the close then un-finished it.
    @Test
    fun `a book just stopped is marked only once its closing report is in`() = runBlocking {
        player.play()
        run(player).untilPositionAtLeast(305_000)
        reporting.beforeLeaving()
        player.clearMediaItems()

        val marking = async(Dispatchers.Unconfined) { marker().mark(finished = true, itemId = "b1", episodeId = null) }
        assertFalse("nothing is marked before the close is delivered", server.isFinished)
        closing.complete(Unit)

        assertTrue(withTimeoutOrNull(5_000) { marking.await() }?.isSuccess == true)
        assertTrue(server.isFinished)
    }

    // Review: un-finishing a book not playing sent it back to 0; the details
    // screen knows where it was.
    @Test
    fun `unfinished from elsewhere puts the book back where it was`() = runBlocking {
        server.isFinished = true
        server.currentTime = 1000.0

        assertTrue(marker().mark(finished = false, itemId = "b9", episodeId = null, keepAt = 1000.0).isSuccess)

        assertFalse(server.isFinished)
        assertEquals(1000.0, server.currentTime, 0.001)
    }

    @Test
    fun `the command carries where to keep the book`() {
        assertEquals(1000.0, FinishCommand.keepAtOf(FinishCommand.args(false, itemId = "b9", keepAt = 1000.0))!!, 0.0)
        assertEquals(null, FinishCommand.keepAtOf(FinishCommand.args(false, itemId = "b9")))
    }
}
