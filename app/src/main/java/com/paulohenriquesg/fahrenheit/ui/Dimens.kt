package com.paulohenriquesg.fahrenheit.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The spacing scale. See docs/ui-style-guide.md.
 *
 * A whole screen is 540dp tall on these sticks, so these are deliberately small
 * numbers: at density 2.0 each one is twice as many pixels as it reads.
 */
object Space {
    /** Inside a box. */
    val inset = 14.dp

    /** Between boxes. */
    val gap = 16.dp

    /** A screen's horizontal safe area. */
    val screenH = 24.dp

    /** A full-width block of text, which wants more room than a box does. */
    val readingH = 48.dp

    /** Clear of the top bar. */
    val belowTopBar = 48.dp
}

object Radius {
    val panel = RoundedCornerShape(10.dp)
    val inner = RoundedCornerShape(6.dp)
    val bar = RoundedCornerShape(3.dp)
}

object Border {
    /** A focusable box at rest. */
    val rest = 1.dp

    /** The same box with focus: thick enough to see across a room. */
    val focus = 3.dp
}
