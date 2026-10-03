package com.paulohenriquesg.fahrenheit.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A length or time left on a list row or a cover: seconds under ten minutes,
 * whole minutes from there, no "0 min" on a whole hour. Not the player's
 * running counter, which keeps PlaybackPosition.spoken and ticks by the second.
 */
class ListeningLengthTest {

    @Test
    fun `under a minute is in seconds`() = assertEquals("45 s", listeningLength(45.0))

    @Test
    fun `under ten minutes keeps its seconds`() = assertEquals("9 min 59 s", listeningLength(599.0))

    @Test
    fun `from ten minutes it rounds to the nearest minute`() {
        assertEquals("10 min", listeningLength(600.0))
        assertEquals("29 min", listeningLength(28 * 60 + 49.0))
        assertEquals("30 min", listeningLength(30 * 60 + 3.0))
        assertEquals("48 min", listeningLength(48 * 60.0))
    }

    @Test
    fun `over an hour reads in hours and minutes`() =
        assertEquals("4 h 12 min", listeningLength(4 * 3600 + 12 * 60 + 10.0))

    @Test
    fun `a whole number of hours reads as hours alone`() {
        assertEquals("1 h", listeningLength(3600.0))
        assertEquals("2 h", listeningLength(2 * 3600 + 20.0))
        // Rounding up across the hour lands on a whole hour too.
        assertEquals("1 h", listeningLength(3599.6))
    }

    @Test
    fun `nothing left is never negative`() = assertEquals("0 s", listeningLength(-3.0))
}
