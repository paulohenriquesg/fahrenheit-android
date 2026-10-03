package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The colour behind the player, taken from its cover (#107). */
class CoverWashTest {
    private val onSurface = Color(0xFFE6E1E5)
    private val onSurfaceVariant = Color(0xFFCAC4D0)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance() + 0.05, b.luminance() + 0.05).sortedDescending()
        return (hi / lo).toDouble()
    }

    @Test fun `the first colourful candidate wins`() {
        val green = Color(0xFF5B6B3A).toArgb()
        val wash = CoverWash.pick(listOf(null, Color(0xFF777777).toArgb(), green))!!
        assertTrue("greenish: $wash", wash.green > wash.red && wash.green > wash.blue)
    }

    // Review Focus 2.
    @Test fun `a colourless cover gives no wash`() =
        assertNull(CoverWash.pick(listOf(Color.Black.toArgb(), Color(0xFF808080).toArgb(), Color.White.toArgb())))

    @Test fun `no candidates gives no wash`() = assertNull(CoverWash.pick(listOf(null, null)))

    // Review Focus 1.
    @Test fun `a white cover still gives a dark wash`() {
        val wash = CoverWash.tone(Color(0xFFFFE8E8).toArgb())
        assertTrue(contrast(onSurface, wash) >= CoverWash.MIN_CONTRAST)
        assertTrue(contrast(onSurfaceVariant, wash) >= CoverWash.MIN_CONTRAST)
    }

    @Test fun `a dark colour is kept as it is`() {
        val dark = Color(0xFF24301A)
        assertEquals(dark.toArgb(), CoverWash.tone(dark.toArgb()).toArgb())
    }

    @Test fun `toning keeps the hue`() {
        val wash = CoverWash.tone(Color(0xFF3060E0).toArgb())
        assertTrue("still blue: $wash", wash.blue > wash.red && wash.blue > wash.green)
    }
}
