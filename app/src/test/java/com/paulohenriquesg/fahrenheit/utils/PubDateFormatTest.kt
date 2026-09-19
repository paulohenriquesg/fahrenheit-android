package com.paulohenriquesg.fahrenheit.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class PubDateFormatTest {
    private lateinit var locale: Locale
    private lateinit var zone: TimeZone

    @Before fun save() { locale = Locale.getDefault(); zone = TimeZone.getDefault() }
    @After fun restore() { Locale.setDefault(locale); TimeZone.setDefault(zone) }

    @Test
    fun `an RFC 822 date shows as day, month, year`() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("19/09/2026", formatPubDate("Fri, 19 Sep 2026 12:00:00 GMT"))
    }

    // Feeds publish English day and month names whatever the TV's language.
    @Test
    fun `English dates still parse on a Portuguese device`() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("19/09/2026", formatPubDate("Fri, 19 Sep 2026 12:00:00 GMT"))
    }

    @Test
    fun `a missing date shows nothing`() = assertEquals("", formatPubDate(null))

    @Test
    fun `an unparseable date is shown as sent`() = assertEquals("sometime", formatPubDate("sometime"))
}
