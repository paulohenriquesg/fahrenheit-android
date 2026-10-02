package com.paulohenriquesg.fahrenheit.utils

import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The latest-episodes list showed no dates at all, so there was no way to tell
 * a brand new episode from one published in March - which is what made it look
 * like nothing new was arriving.
 */
class EpisodeDateTest {

    private val originalZone = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()
    private val now = 1_790_000_000_000L // a fixed "now"
    private val day = 24 * 60 * 60 * 1000L

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
    fun `something published a few hours ago is today`() {
        assertEquals("Today", EpisodeDate.of(now - 3 * 60 * 60 * 1000L, now))
    }

    @Test
    fun `yesterday says so`() {
        assertEquals("Yesterday", EpisodeDate.of(now - day - 60_000L, now))
    }

    @Test
    fun `this week counts the days`() {
        assertEquals("4 days ago", EpisodeDate.of(now - 4 * day, now))
    }

    @Test
    fun `older than a week gets a date`() {
        assertEquals("13/09/2026", EpisodeDate.of(now - 8 * day, now))
    }

    @Test
    fun `an episode with no date says nothing rather than lying`() {
        assertEquals("", EpisodeDate.of(null, now))
        assertEquals("", EpisodeDate.of(0, now))
    }

    @Test
    fun `a date in the future is not reported as days ago`() {
        // Feeds do publish ahead; "in 2 days ago" would be nonsense.
        assertEquals("Today", EpisodeDate.of(now + 2 * day, now))
    }
}
