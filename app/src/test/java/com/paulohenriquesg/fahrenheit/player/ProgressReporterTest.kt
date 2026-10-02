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
}
