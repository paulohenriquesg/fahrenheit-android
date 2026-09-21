package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter
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
     * Where to draw a mark for each chapter boundary, as a percentage of the
     * track. The final chapter ends where the book does, so it gets no mark,
     * and an end beyond the reported duration is left out rather than drawn
     * off the end of the bar.
     */
    fun chapterMarks(chapters: List<Chapter>?, total: Double): List<Float> {
        if (total <= 0) return emptyList()
        return chapters.orEmpty()
            .mapNotNull { it.end }
            .filter { it > 0 && it < total }
            .map { (it / total * 100).toFloat() }
    }

    /** A position as HH:MM:SS, in Latin digits whatever the device language. */
    fun clock(seconds: Double): String {
        val whole = seconds.coerceAtLeast(0.0).toLong()
        return String.format(Locale.ROOT, "%02d:%02d:%02d", whole / 3600, (whole % 3600) / 60, whole % 60)
    }
}
