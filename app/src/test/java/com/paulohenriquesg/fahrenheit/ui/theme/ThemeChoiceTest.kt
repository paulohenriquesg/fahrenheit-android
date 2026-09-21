package com.paulohenriquesg.fahrenheit.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeChoiceTest {

    // A TV set to dark should not open a white app on first run.
    @Test
    fun `before anyone chooses, the device decides`() {
        assertEquals(true, ThemeChoice.resolve(chosen = null, systemIsDark = true))
        assertEquals(false, ThemeChoice.resolve(chosen = null, systemIsDark = false))
    }

    @Test
    fun `a choice in settings outranks the device`() {
        assertEquals(false, ThemeChoice.resolve(chosen = false, systemIsDark = true))
        assertEquals(true, ThemeChoice.resolve(chosen = true, systemIsDark = false))
    }
}
