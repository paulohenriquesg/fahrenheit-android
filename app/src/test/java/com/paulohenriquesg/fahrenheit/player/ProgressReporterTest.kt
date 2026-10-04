package com.paulohenriquesg.fahrenheit.player

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Sending the listening position while playing, for books and episodes alike.
 * It was written twice, once per player, and the book player ran it twice at
 * once whenever playback started from the remote's media button.
 */
class ProgressReporterTest {

    private fun reporter(
        positions: List<Double>,
        sent: MutableList<ListeningReport>,
        failOn: Int = -1
    ): Pair<ProgressReporter, () -> Boolean> {
        val queue = ArrayDeque(positions)
        var calls = 0
        val reporter = ProgressReporter(
            send = { request ->
                if (calls++ == failOn) throw IOException("offline")
                sent += request
            },
            position = { queue.first() },
            total = { 1000.0 },
            pause = { queue.removeFirst() }
        )
        // Plays for as long as there are positions left to report.
        return reporter to { queue.size > 1 }
    }

    @Test
    fun `while playing, the position is sent`() = runBlocking {
        val sent = mutableListOf<ListeningReport>()
        val (reporter, playing) = reporter(listOf(0.0, 10.0, 20.0, 30.0), sent)

        reporter.run(playing)

        assertEquals(listOf(10.0, 20.0, 30.0), sent.map { it.currentTime })
    }

    @Test
    fun `a position already sent is not sent again`() = runBlocking {
        val sent = mutableListOf<ListeningReport>()
        val (reporter, playing) = reporter(listOf(0.0, 10.0, 10.0, 20.0), sent)

        reporter.run(playing)

        assertEquals(listOf(10.0, 20.0), sent.map { it.currentTime })
    }

    @Test
    fun `a failed send does not end reporting`() = runBlocking {
        val sent = mutableListOf<ListeningReport>()
        val (reporter, playing) = reporter(listOf(0.0, 10.0, 20.0, 30.0), sent, failOn = 0)

        reporter.run(playing)

        assertEquals(listOf(20.0, 30.0), sent.map { it.currentTime })
    }

    @Test
    fun `nothing is sent when not playing`() = runBlocking {
        val sent = mutableListOf<ListeningReport>()
        val (reporter, _) = reporter(listOf(0.0, 10.0), sent)

        reporter.run { false }

        assertEquals(emptyList<Double>(), sent.map { it.currentTime })
    }

    private class Listening {
        val sent = mutableListOf<Double>()
        var at = 0.0
        var rounds = 0
        val reporter = ProgressReporter(
            send = { sent += it.currentTime!! },
            position = { at },
            total = { 1000.0 },
            pause = { at += 10.0 }
        )
        suspend fun play(rounds: Int) {
            this.rounds = rounds
            reporter.run { this.rounds-- > 0 }
        }
    }

    @Test
    fun `stopping sends where it stopped, not where the last round left it`() = runBlocking {
        val listening = Listening()
        listening.play(rounds = 1)   // sends 10
        listening.at = 13.0

        listening.reporter.finish()

        assertEquals(listOf(10.0, 13.0), listening.sent)
    }

    @Test
    fun `stopping where the last report left off sends nothing more`() = runBlocking {
        val listening = Listening()
        listening.play(rounds = 1)

        listening.reporter.finish()

        assertEquals(listOf(10.0), listening.sent)
    }

    // Review Focus 1: opened, never played, Back. A write here would overwrite
    // the resume point with itself and bump the book in Continue Listening.
    @Test
    fun `stopping before anything played sends nothing`() = runBlocking {
        val listening = Listening()
        listening.at = 900.0

        listening.reporter.finish()

        assertEquals(emptyList<Double>(), listening.sent)
    }

    @Test
    fun `a stop shorter than one round is still saved`() = runBlocking {
        val sent = mutableListOf<Double>()
        var at = 900.0
        var playing = true
        val reporter = ProgressReporter(
            send = { sent += it.currentTime!! },
            position = { at },
            total = { 1000.0 },
            // Paused after three seconds, before the first round's report.
            pause = { at = 903.0; playing = false; throw kotlinx.coroutines.CancellationException() }
        )
        runCatching { reporter.run { playing } }

        reporter.finish()

        assertEquals(listOf(903.0), sent)
    }

    private class Session {
        val synced = mutableListOf<ListeningReport>()
        val closed = mutableListOf<ListeningReport?>()
        val delivered = mutableListOf<Double>()
        var at = 0.0
        var heard = 0.0
        var failNext = false
        var rounds = 0
        val reporter = ProgressReporter(
            send = { if (failNext) { failNext = false; error("offline") }; synced += it },
            position = { at },
            total = { 1000.0 },
            pause = { at += 10.0; heard += 10.0 },
            listened = { heard },
            delivered = { delivered += it; heard -= it },
            close = { closed += it }
        )
        suspend fun play(rounds: Int) { this.rounds = rounds; reporter.run { this.rounds-- > 0 } }
    }

    @Test
    fun `listening time goes with each report, and is spent once delivered`() = runBlocking {
        val s = Session()
        s.play(rounds = 2)
        assertEquals(listOf(10.0, 10.0), s.synced.map { it.timeListened })
        assertEquals(listOf(10.0, 10.0), s.delivered)
    }

    @Test
    fun `a failed report keeps its listening time for the next`() = runBlocking {
        val s = Session()
        s.failNext = true
        s.play(rounds = 2)
        assertEquals(listOf(20.0), s.synced.map { it.timeListened })
    }

    @Test
    fun `stopping closes with the last report`() = runBlocking {
        val s = Session()
        s.play(rounds = 1)
        s.at = 13.0; s.heard = 3.0
        s.reporter.finish()
        assertEquals(13.0, s.closed.single()!!.currentTime, 1e-9)
        assertEquals(3.0, s.closed.single()!!.timeListened, 1e-9)
    }

    @Test
    fun `stopping with nothing new still closes`() = runBlocking {
        val s = Session()
        s.play(rounds = 1)
        s.reporter.finish()
        assertEquals(listOf<ListeningReport?>(null), s.closed)
    }

    // Review Focus 1: the guard and the player both announce a stop.
    @Test
    fun `stopping twice closes once`() = runBlocking {
        val s = Session()
        s.play(rounds = 1)
        s.reporter.finish(); s.reporter.finish()
        assertEquals(1, s.closed.size)
    }

    @Test
    fun `playing again after a stop closes again at the next stop`() = runBlocking {
        val s = Session()
        s.play(rounds = 1); s.reporter.finish()
        s.play(rounds = 1); s.reporter.finish()
        assertEquals(2, s.closed.size)
    }

    @Test
    fun `stopping before anything played closes nothing`() = runBlocking {
        val s = Session()
        s.reporter.finish()
        assertEquals(emptyList<ListeningReport?>(), s.closed)
    }

    // #145: what reached the server is known by the position it wrote.
    @Test
    fun `each report the server took is handed on, and only those`() = runBlocking {
        val reached = mutableListOf<Double>()
        val queue = ArrayDeque(listOf(0.0, 10.0, 20.0, 30.0))
        var calls = 0
        val reporter = ProgressReporter(
            send = { if (calls++ == 1) throw IOException("offline") },
            position = { queue.first() },
            total = { 1000.0 },
            pause = { queue.removeFirst() },
            reached = { reached += it.currentTime }
        )

        reporter.run { queue.size > 1 }
        reporter.finish()

        // The send of 20 failed; 10 and 30 reached the server.
        assertEquals(listOf(10.0, 30.0), reached)
    }

    @Test
    fun `a close with nothing new hands nothing on`() = runBlocking {
        val reached = mutableListOf<Double>()
        val queue = ArrayDeque(listOf(0.0, 10.0, 10.0))
        val reporter = ProgressReporter(
            send = {},
            position = { queue.first() },
            total = { 1000.0 },
            pause = { queue.removeFirst() },
            close = {},
            reached = { reached += it.currentTime }
        )

        reporter.run { queue.size > 1 }
        reporter.finish()

        assertEquals(listOf(10.0), reached)
    }
}
