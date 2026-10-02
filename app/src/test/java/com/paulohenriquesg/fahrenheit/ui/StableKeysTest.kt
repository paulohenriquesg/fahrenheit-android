package com.paulohenriquesg.fahrenheit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Keys for lazy lists have to be unique or Compose throws (#55).
 *
 * The server does repeat ids: a podcast library's "Continue Listening" shelf
 * lists one entry per episode in progress, so the same library item id
 * appeared six times in one row and the app crashed on launch.
 */
class StableKeysTest {

    @Test
    fun `ids that are already unique are used as they are`() =
        assertEquals(listOf("a", "b", "c"), StableKeys.of(listOf("a", "b", "c")) { it })

    @Test
    fun `a repeated id gets a distinct key per occurrence`() {
        val keys = StableKeys.of(listOf("pod", "pod", "other", "pod")) { it }

        assertEquals(4, keys.toSet().size)
        assertEquals("pod", keys.first())
        assertEquals("other", keys[2])
    }

    @Test
    fun `keys keep their position so a refresh does not reshuffle them`() {
        val first = StableKeys.of(listOf("a", "pod", "pod")) { it }
        val again = StableKeys.of(listOf("a", "pod", "pod")) { it }

        assertEquals(first, again)
    }

    @Test
    fun `an empty list has no keys`() =
        assertEquals(emptyList<String>(), StableKeys.of(emptyList<String>()) { it })

    // Nothing says a server id cannot be blank, and two blanks must still differ.
    @Test
    fun `blank ids are still distinguished`() {
        val keys = StableKeys.of(listOf("", "", "x")) { it }

        assertEquals(3, keys.toSet().size)
    }
}
