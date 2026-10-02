package com.paulohenriquesg.fahrenheit.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ContrastTest {

    @Test
    fun `black on white is the widest there is`() {
        assertEquals(21f, Contrast.ratio(Color.Black, Color.White), 0.1f)
    }

    @Test
    fun `a colour on itself is no contrast at all`() {
        assertEquals(1f, Contrast.ratio(Color.Red, Color.Red), 0.001f)
    }

    @Test
    fun `the order of the pair does not matter`() {
        assertEquals(
            Contrast.ratio(Color.Black, Color.White),
            Contrast.ratio(Color.White, Color.Black),
            0.001f
        )
    }
}
