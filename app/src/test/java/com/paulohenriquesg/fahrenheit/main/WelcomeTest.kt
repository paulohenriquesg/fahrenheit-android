package com.paulohenriquesg.fahrenheit.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeTest {

    @Test
    fun `the greeting uses the name`() {
        (0..50).forEach { seed ->
            assertTrue(Welcome.pick("admin", seed).contains("admin"))
        }
    }

    @Test
    fun `the same seed gives the same greeting, so it does not flicker`() {
        assertEquals(Welcome.pick("admin", 7), Welcome.pick("admin", 7))
    }

    @Test
    fun `it is not the same line every time`() {
        val seen = (0..50).map { Welcome.pick("admin", it) }.toSet()

        assertTrue("only saw $seen", seen.size > 1)
    }

    @Test
    fun `a server with no username does not get greeted with a comma`() {
        (0..50).forEach { seed ->
            val greeting = Welcome.pick("", seed)
            assertFalse("$greeting", greeting.contains(", !"))
            assertFalse("$greeting", greeting.trim().endsWith(","))
            assertTrue("$greeting", greeting.isNotBlank())
        }
    }
}
