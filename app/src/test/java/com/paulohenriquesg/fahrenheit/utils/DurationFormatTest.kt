package com.paulohenriquesg.fahrenheit.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * "1h 5m" style durations. The suffixes are English, so the digits must be Latin
 * whatever the device locale: formatting with the default locale produced
 * mixed output such as "۱h ۵m" on Persian devices (lint: DefaultLocale).
 */
class DurationFormatTest {
    private lateinit var saved: Locale

    @Before fun remember() { saved = Locale.getDefault() }
    @After fun restore() { Locale.setDefault(saved) }

    @Test fun `under an hour shows minutes only`() = assertEquals("59m", formatDuration(3599.9))
    @Test fun `zero is zero minutes`() = assertEquals("0m", formatDuration(0.0))
    @Test fun `exactly an hour`() = assertEquals("1h 0m", formatDuration(3600.0))
    @Test fun `hours and minutes`() = assertEquals("1h 1m", formatDuration(3661.0))
    @Test fun `long audiobooks keep counting hours`() = assertEquals("25h 1m", formatDuration(90061.0))

    @Test
    fun `digits stay Latin on a locale with its own numerals`() {
        Locale.setDefault(Locale("fa"))

        assertEquals("1h 1m", formatDuration(3661.0))
    }
}
