package com.paulohenriquesg.fahrenheit.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

/** #194: the series row showed four whole covers of seven, with no hint of the rest. */
class SeriesRowTest {

    private val gap = 12.dp
    private val smallest = 72.dp

    /** How much of the cover after the last whole one shows. */
    private fun peek(width: Dp, cover: Dp): Float {
        val whole = floor((width.value + gap.value) / (cover.value + gap.value))
        return (width.value - whole * (cover.value + gap.value)) / cover.value
    }

    @Test fun `the row's width leaves about half a cover showing at its right edge`() {
        // The book screen's and About's rows, and a wider one.
        listOf(380.dp, 308.dp, 520.dp).forEach { width ->
            val cover = SeriesRow.coverSize(width, gap, smallest)
            assertTrue("$width: cover $cover", cover >= smallest)
            val peek = peek(width, cover)
            assertTrue("$width: cover $cover shows $peek of the next", peek in 0.4f..0.6f)
        }
    }
}
