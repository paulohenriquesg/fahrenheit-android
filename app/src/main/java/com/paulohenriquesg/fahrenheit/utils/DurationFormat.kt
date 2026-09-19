package com.paulohenriquesg.fahrenheit.utils

import java.util.Locale

/**
 * A duration as "1h 5m", or "5m" under an hour.
 *
 * Locale.ROOT, not the default: the suffixes are English, so the digits must be
 * Latin too. With the default locale a Persian device would show "۱h ۵m".
 */
fun formatDuration(seconds: Double): String {
    val hours = (seconds / 3600).toInt()
    val minutes = ((seconds % 3600) / 60).toInt()
    return if (hours > 0) {
        String.format(Locale.ROOT, "%dh %dm", hours, minutes)
    } else {
        String.format(Locale.ROOT, "%dm", minutes)
    }
}
