package com.paulohenriquesg.fahrenheit.player

/**
 * Which position updates are worth sending while something is playing.
 *
 * Both players used to send whatever the media player reported, every five
 * seconds, whether or not it had moved or was even ready to be asked.
 */
object ProgressSync {

    /** How often to look at the position. */
    const val INTERVAL_MS = 5_000L

    /** Below this, the position has not really moved. */
    private const val MIN_MOVEMENT_SECONDS = 1.0

    /**
     * The update to send, or null to skip this round.
     *
     * [lastSent] is the position last reported in this session, and separates
     * "the player has not started yet" from "the listener went back to the
     * beginning": the first must not overwrite a saved resume point with 0,
     * the second must be saved.
     *
     * [listened] is the seconds played since the last delivered report; a
     * round that did not move is still sent when it has that to deliver.
     */
    fun next(position: Double, total: Double, lastSent: Double?, listened: Double = 0.0): ListeningReport? {
        if (total <= 0) return null
        if (position < 0) return null
        if (lastSent == null && position <= 0) return null

        val reported = position.coerceAtMost(total)
        // A round that barely moved is still worth sending when it carries
        // listening time: stats need the time, e.g. after buffering (#92).
        val moved = lastSent == null || kotlin.math.abs(reported - lastSent) >= MIN_MOVEMENT_SECONDS
        if (!moved && listened < MIN_MOVEMENT_SECONDS) return null

        return ListeningReport(currentTime = reported, duration = total, timeListened = listened.coerceAtLeast(0.0))
    }
}
