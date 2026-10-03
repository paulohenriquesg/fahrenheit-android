package com.paulohenriquesg.fahrenheit.player

import java.math.BigDecimal

/** The speeds on offer in the Speed panel, and what a speed does to time left (#107). */
object ListeningSpeed {
    const val NORMAL = 1f

    val STEPS = listOf(0.75f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f)

    /** "1.25×", "1×": as frame C writes it. */
    fun label(speed: Float): String =
        BigDecimal(speed.toString()).stripTrailingZeros().toPlainString() + "×"

    /** How long the rest of the book takes to hear at [speed]. */
    fun left(position: Double, total: Double, speed: Float): Double =
        PlaybackPosition.left(position, total) / speed
}
