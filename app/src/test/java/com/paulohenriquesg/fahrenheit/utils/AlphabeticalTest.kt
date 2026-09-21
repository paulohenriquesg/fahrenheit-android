package com.paulohenriquesg.fahrenheit.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Sorting titles the way a reader expects, rather than by character code.
 */
class AlphabeticalTest {

    private lateinit var locale: Locale

    @Before fun save() { locale = Locale.getDefault(); Locale.setDefault(Locale.forLanguageTag("pt-BR")) }
    @After fun restore() { Locale.setDefault(locale) }

    private fun sorted(vararg names: String?) = names.toList().sortedWith(Alphabetical.byName { it })

    @Test
    fun `case does not decide the order`() =
        assertEquals(listOf("apple", "Banana", "cherry"), sorted("Banana", "cherry", "apple"))

    // Á is code point 193, past Z at 90: by character code an accented title
    // lands after every unaccented one.
    @Test
    fun `accented titles sit with their unaccented neighbours`() =
        assertEquals(listOf("Ártico", "Baleia", "Zebra"), sorted("Zebra", "Ártico", "Baleia"))

    @Test
    fun `titles with no name sort last rather than first`() =
        assertEquals(listOf("Baleia", "Zebra", null), sorted("Zebra", null, "Baleia"))

    @Test
    fun `an empty list is left alone`() =
        assertEquals(emptyList<String>(), emptyList<String>().sortedWith(Alphabetical.byName { it }))
}
