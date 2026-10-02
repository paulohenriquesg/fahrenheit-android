package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
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
        sent: MutableList<MediaProgressRequest>,
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
        val sent = mutableListOf<MediaProgressRequest>()
        val (reporter, playing) = reporter(listOf(0.0, 10.0, 20.0, 30.0), sent)

        reporter.run(playing)

        assertEquals(listOf(10.0, 20.0, 30.0), sent.map { it.currentTime })
    }

    @Test
    fun `a position already sent is not sent again`() = runBlocking {
        val sent = mutableListOf<MediaProgressRequest>()
        val (reporter, playing) = reporter(listOf(0.0, 10.0, 10.0, 20.0), sent)

        reporter.run(playing)

        assertEquals(listOf(10.0, 20.0), sent.map { it.currentTime })
    }

    @Test
    fun `a failed send does not end reporting`() = runBlocking {
        val sent = mutableListOf<MediaProgressRequest>()
        val (reporter, playing) = reporter(listOf(0.0, 10.0, 20.0, 30.0), sent, failOn = 0)

        reporter.run(playing)

        assertEquals(listOf(20.0, 30.0), sent.map { it.currentTime })
    }

    @Test
    fun `nothing is sent when not playing`() = runBlocking {
        val sent = mutableListOf<MediaProgressRequest>()
        val (reporter, _) = reporter(listOf(0.0, 10.0), sent)

        reporter.run { false }

        assertEquals(emptyList<Double>(), sent.map { it.currentTime })
    }
}
