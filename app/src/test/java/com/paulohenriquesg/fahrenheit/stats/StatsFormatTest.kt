package com.paulohenriquesg.fahrenheit.stats

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsFormatTest {

    private val original = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `whole hours leave the minutes off`() {
        assertEquals("181 h", shortDuration(651_600.0))
    }

    @Test
    fun `hours carry their minutes`() {
        assertEquals("2 h 11 min", shortDuration(7_860.0))
    }

    @Test
    fun `under an hour is minutes alone`() {
        assertEquals("24 min", shortDuration(1_440.0))
    }

    @Test
    fun `a listen too short to round to a minute still says so`() {
        assertEquals("under a minute", shortDuration(42.0))
    }

    @Test
    fun `nothing listened is nothing, not a blank`() {
        assertEquals("0 min", shortDuration(0.0))
    }

    @Test
    fun `the digits stay Latin wherever the device is`() {
        Locale.setDefault(Locale.forLanguageTag("fa-IR"))

        assertEquals("2 h 11 min", shortDuration(7_860.0))
    }
}
