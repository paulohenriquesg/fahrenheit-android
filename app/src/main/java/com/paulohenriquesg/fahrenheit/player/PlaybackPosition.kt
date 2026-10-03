package com.paulohenriquesg.fahrenheit.player

import java.util.Locale

/**
 * The arithmetic behind the player's scrubber.
 *
 * It lived inline in the player composable, the progress fraction repeated at
 * five call sites, each able to drift from the others.
 */
object PlaybackPosition {

    /** How much of the book is behind us, as 0..1. */
    fun fraction(current: Double, total: Double): Float {
        if (total <= 0) return 0f
        return (current / total).coerceIn(0.0, 1.0).toFloat()
    }

    /** Where a skip button lands, never outside the book. */
    fun skip(current: Double, by: Double, total: Double): Double =
        (current + by).coerceIn(0.0, total.coerceAtLeast(0.0))


    /**
     * A length as frame 4 writes it: "5 h 28 min" over an hour, "12 min 30 s"
     * under one, "45 s" under a minute, so a short episode visibly moves.
     * Latin digits whatever the device language, as the units are Latin.
     */
    fun spoken(seconds: Double): String {
        val whole = seconds.coerceAtLeast(0.0).toLong()
        val h = whole / 3600
        val m = (whole % 3600) / 60
        val s = whole % 60
        return when {
            h > 0 -> String.format(Locale.ROOT, "%d h %d min", h, m)
            m > 0 -> String.format(Locale.ROOT, "%d min %d s", m, s)
            else -> String.format(Locale.ROOT, "%d s", s)
        }
    }

    /** How much of the book is still ahead, never below nothing. */
    fun left(current: Double, total: Double): Double = (total - current).coerceAtLeast(0.0)
}
