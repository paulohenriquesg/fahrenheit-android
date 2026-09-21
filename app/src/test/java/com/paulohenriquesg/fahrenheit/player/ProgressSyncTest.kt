package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which position updates are worth sending to the server while playing.
 *
 * Both players looped every five seconds and sent whatever the media player
 * reported, including positions from before it was ready.
 */
class ProgressSyncTest {

    @Test
    fun `a position that has moved on is reported`() {
        val request = ProgressSync.next(position = 930.0, total = 3600.0, lastSent = 900.0)!!

        assertEquals(930.0, request.currentTime!!, 1e-9)
        assertEquals(3600.0, request.duration!!, 1e-9)
    }

    @Test
    fun `the first real position is reported`() =
        assertEquals(900.0, ProgressSync.next(position = 900.0, total = 3600.0, lastSent = null)!!.currentTime!!, 1e-9)

    // A media player that is not prepared reports 0, or -1. Sending that would
    // overwrite the saved resume point with the start of the book.
    @Test
    fun `a position from before playback started is not reported`() {
        assertNull(ProgressSync.next(position = 0.0, total = 3600.0, lastSent = null))
        assertNull(ProgressSync.next(position = -1.0, total = 3600.0, lastSent = null))
    }

    // Once it has reported real positions, a return to the start is a seek the
    // listener made, and has to be saved.
    @Test
    fun `seeking back to the start after playing is reported`() =
        assertEquals(0.0, ProgressSync.next(position = 0.0, total = 3600.0, lastSent = 900.0)!!.currentTime!!, 1e-9)

    @Test
    fun `an unknown length is not reported`() {
        assertNull(ProgressSync.next(position = 900.0, total = 0.0, lastSent = 800.0))
        assertNull(ProgressSync.next(position = 900.0, total = -1.0, lastSent = 800.0))
    }

    // Paused or stalled, the position does not move; there is nothing to say.
    @Test
    fun `a position that has barely moved is not resent`() =
        assertNull(ProgressSync.next(position = 900.2, total = 3600.0, lastSent = 900.0))

    @Test
    fun `a position past the end is reported as the end`() =
        assertEquals(3600.0, ProgressSync.next(position = 3700.0, total = 3600.0, lastSent = 3000.0)!!.currentTime!!, 1e-9)
}
