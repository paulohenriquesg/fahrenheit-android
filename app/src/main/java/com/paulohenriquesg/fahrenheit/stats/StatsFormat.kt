package com.paulohenriquesg.fahrenheit.stats

import java.util.Locale

/**
 * A duration short enough for a tile: "181 h", "2 h 11 min", "24 min".
 *
 * Locale.ROOT, not the default: the units are English, so the digits must be
 * Latin too. With the default locale a Persian device would read "۲ h ۱۱ min".
 */
fun shortDuration(seconds: Double): String {
    val wholeMinutes = (seconds / 60).toInt()
    val hours = wholeMinutes / 60
    val minutes = wholeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> String.format(Locale.ROOT, "%d h %d min", hours, minutes)
        hours > 0 -> String.format(Locale.ROOT, "%d h", hours)
        minutes > 0 -> String.format(Locale.ROOT, "%d min", minutes)
        // Say what happened rather than round a real listen down to nothing.
        seconds > 0 -> "under a minute"
        else -> "0 min"
    }
}
