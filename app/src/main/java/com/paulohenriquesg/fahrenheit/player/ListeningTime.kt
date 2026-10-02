package com.paulohenriquesg.fahrenheit.player

/**
 * The seconds actually listened, by the clock while playing, since the last
 * report that was delivered. Not the distance the position moved: a seek is
 * not listening, and stats count listening (#92). Seconds from a report that
 * failed stay here and go with the next one.
 *
 * @param now a monotonic clock in milliseconds.
 */
class ListeningTime(private val now: () -> Long) {
    private var playingSince: Long? = null
    private var bankedMs = 0L

    fun playing(isPlaying: Boolean) {
        val t = now()
        if (isPlaying) {
            if (playingSince == null) playingSince = t
        } else {
            playingSince?.let { bankedMs += t - it }
            playingSince = null
        }
    }

    fun pending(): Double = (bankedMs + (playingSince?.let { now() - it } ?: 0L)) / 1000.0

    /** A report carrying [seconds] of listening was delivered. */
    fun delivered(seconds: Double) {
        val t = now()
        playingSince?.let {
            bankedMs += t - it
            playingSince = t
        }
        bankedMs = (bankedMs - (seconds * 1000).toLong()).coerceAtLeast(0L)
    }
}
