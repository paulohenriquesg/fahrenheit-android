package com.paulohenriquesg.fahrenheit.utils

import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import java.util.Locale
import kotlin.math.roundToLong

private const val TEN_MINUTES = 600.0

/**
 * A length or time left on a list row or a cover ("29 min", "4 h 12 min",
 * "1 h"). Under ten minutes it keeps its seconds as the player writes them
 * ("9 min 59 s"); from there the seconds are noise and it rounds to the
 * nearest minute, with no "0 min" on a whole hour.
 *
 * Not for the player's running counter: that keeps PlaybackPosition.spoken,
 * which must tick by the second.
 */
fun listeningLength(seconds: Double): String {
    if (seconds < TEN_MINUTES) return PlaybackPosition.spoken(seconds)
    val minutes = (seconds / 60).roundToLong()
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> String.format(Locale.ROOT, "%d min", m)
        m == 0L -> String.format(Locale.ROOT, "%d h", h)
        else -> String.format(Locale.ROOT, "%d h %d min", h, m)
    }
}

/**
 * Time left where it is glanced at and must not tick, the screensaver's line
 * (#172): whole minutes, the part minute dropped as the main screen's rail
 * drops it ("18 min", "1 h 5 min", "2 h"), and "under a minute" rather than
 * "0 min" or seconds.
 */
fun minutesLeft(seconds: Double): String {
    if (seconds < 60) return "under a minute"
    val minutes = (seconds / 60).toLong()
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> String.format(Locale.ROOT, "%d min", m)
        m == 0L -> String.format(Locale.ROOT, "%d h", h)
        else -> String.format(Locale.ROOT, "%d h %d min", h, m)
    }
}
