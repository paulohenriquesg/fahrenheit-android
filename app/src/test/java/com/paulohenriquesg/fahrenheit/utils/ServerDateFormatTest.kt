package com.paulohenriquesg.fahrenheit.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Audiobookshelf lets the server choose how dates are written, and sends it at
 * login. Its patterns are date-fns ones, which mostly coincide with Java's -
 * except the ordinal day, which Java cannot express at all.
 */
class ServerDateFormatTest {

    @Test
    fun `the plain patterns are already Java patterns`() {
        listOf("MM/dd/yyyy", "dd/MM/yyyy", "dd.MM.yyyy", "yyyy-MM-dd", "dd MMM yyyy", "dd MMMM yyyy")
            .forEach { assertEquals(it, ServerDateFormat.pattern(it)) }
    }

    @Test
    fun `an ordinal day becomes a plain one rather than printing a literal d`() {
        // date-fns "do" is 1st, 2nd, 3rd. Java has no equivalent, and left in
        // the pattern the letters would render as garbage.
        assertEquals("MMM d, yyyy", ServerDateFormat.pattern("MMM do, yyyy"))
        assertEquals("MMMM d, yyyy", ServerDateFormat.pattern("MMMM do, yyyy"))
    }

    @Test
    fun `a format the server never sent is nobody's business`() {
        assertNull(ServerDateFormat.pattern(null))
        assertNull(ServerDateFormat.pattern(""))
        assertNull(ServerDateFormat.pattern("   "))
    }

    @Test
    fun `a pattern Java cannot parse is refused rather than crashing a screen`() {
        assertNull(ServerDateFormat.pattern("yyyy QQQQ 'o''clock"))
    }

    @Test
    fun `an unfamiliar but valid pattern is still honoured`() {
        // The server's list may grow; anything Java accepts should pass through.
        assertEquals("EEE, dd MMM yyyy", ServerDateFormat.pattern("EEE, dd MMM yyyy"))
    }
}
