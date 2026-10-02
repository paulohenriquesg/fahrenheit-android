package com.paulohenriquesg.fahrenheit.utils

import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The server is configured with a date format and sends it at login. Dates older
 * than a week were written dd/MM/yyyy regardless, which is wrong for a server
 * set to MM/dd/yyyy and reads as a date in September rather than one in May.
 */
class EpisodeDateFormatTest {

    private val originalZone = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()
    private val now = 1_790_000_000_000L
    private val eightDaysAgo = now - 8 * 24 * 60 * 60 * 1000L

    @Before
    fun pinTheClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.UK)
    }

    @After
    fun restore() {
        TimeZone.setDefault(originalZone)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `a server set to day-first writes the day first`() {
        assertEquals("13/09/2026", EpisodeDate.of(eightDaysAgo, now, "dd/MM/yyyy"))
    }

    @Test
    fun `a server set to month-first writes the month first`() {
        assertEquals("09/13/2026", EpisodeDate.of(eightDaysAgo, now, "MM/dd/yyyy"))
    }

    @Test
    fun `a server that spells the month gets the month spelled`() {
        assertEquals("13 Sept 2026", EpisodeDate.of(eightDaysAgo, now, "dd MMM yyyy"))
    }

    @Test
    fun `an ordinal day from the server does not print stray letters`() {
        assertEquals("Sept 13, 2026", EpisodeDate.of(eightDaysAgo, now, "MMM do, yyyy"))
    }

    @Test
    fun `no format from the server falls back rather than failing`() {
        assertEquals("13/09/2026", EpisodeDate.of(eightDaysAgo, now, null))
    }

    @Test
    fun `a format Java cannot use falls back too`() {
        assertEquals("13/09/2026", EpisodeDate.of(eightDaysAgo, now, "yyyy QQQQ 'o''clock"))
    }

    @Test
    fun `the words for recent days are not affected by the format`() {
        assertEquals("Today", EpisodeDate.of(now - 3_600_000L, now, "MM/dd/yyyy"))
        assertEquals("4 days ago", EpisodeDate.of(now - 4 * 24 * 60 * 60 * 1000L, now, "MM/dd/yyyy"))
    }
}
