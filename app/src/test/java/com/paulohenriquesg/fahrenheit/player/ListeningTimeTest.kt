package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Seconds actually listened, by the clock: what stats count (#92). */
class ListeningTimeTest {
    private var ms = 0L
    private val time = ListeningTime { ms }

    @Test
    fun `time playing counts`() {
        time.playing(true); ms += 10_000
        assertEquals(10.0, time.pending(), 1e-9)
    }

    @Test
    fun `time paused does not`() {
        time.playing(true); ms += 10_000; time.playing(false); ms += 30_000
        assertEquals(10.0, time.pending(), 1e-9)
    }

    @Test
    fun `delivered seconds are spent, and the clock keeps counting`() {
        time.playing(true); ms += 10_000
        time.delivered(10.0); ms += 5_000
        assertEquals(5.0, time.pending(), 1e-9)
    }

    @Test
    fun `undelivered seconds roll into the next report`() {
        time.playing(true); ms += 10_000
        // A failed sync delivers nothing.
        ms += 5_000
        assertEquals(15.0, time.pending(), 1e-9)
    }

    @Test
    fun `playing twice in a row does not restart the count`() {
        time.playing(true); ms += 4_000; time.playing(true); ms += 1_000
        assertEquals(5.0, time.pending(), 1e-9)
    }
}
