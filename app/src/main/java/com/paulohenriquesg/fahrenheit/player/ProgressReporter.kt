package com.paulohenriquesg.fahrenheit.player

import kotlinx.coroutines.delay

/**
 * Sends the listening position while something plays, by [ProgressSync]'s rules.
 *
 * One class for books and episodes: it was written once per player, and the
 * book player ran two copies at once whenever playback started from the
 * remote's media button. Kept out of the Activity, so the playback service owns
 * it unchanged.
 *
 * @param send the request for this item or episode; throwing is a failed send.
 * @param reached each report the server took, as it was sent (#145).
 */
class ProgressReporter(
    private val send: suspend (ListeningReport) -> Unit,
    private val position: () -> Double,
    private val total: () -> Double,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) },
    private val listened: () -> Double = { 0.0 },
    private val delivered: (Double) -> Unit = {},
    private val close: suspend (ListeningReport?) -> Unit = { it?.let { r -> send(r) } },
    private val reached: (ListeningReport) -> Unit = {}
) {
    private var lastSent: Double? = null

    /** Whether anything has played; a reporter that never played has nothing to say. */
    private var played = false

    /** Whether this stretch of listening is closed already: a stop arrives twice. */
    private var closed = false

    suspend fun run(isPlaying: () -> Boolean) {
        while (isPlaying()) {
            played = true
            closed = false
            pause()
            val report = ProgressSync.next(position(), total(), lastSent, listened()) ?: continue
            // Only a delivered report counts as sent; a failed one is retried,
            // and its listening time goes with the next.
            if (runCatching { send(report) }.isSuccess) {
                lastSent = report.currentTime
                delivered(report.timeListened)
                reached(report)
            }
        }
    }

    /**
     * Closes this stretch of listening: once, with whatever is new since the
     * last report, when playback stops or the queue is about to change.
     *
     * Without it up to one round of listening is lost on every pause. The
     * position and time are read before anything suspends, so a caller that
     * starts this undispatched captures them as they are now, before the
     * queue changes under it. Not retried: there is no next round to retry in.
     *
     * @return whether this closed the stretch; false when there was none open.
     */
    suspend fun finish(): Boolean {
        if (!played || closed) return false
        closed = true
        val report = ProgressSync.next(position(), total(), lastSent, listened())
        report?.let { lastSent = it.currentTime }
        if (runCatching { close(report) }.isSuccess) report?.let {
            delivered(it.timeListened)
            reached(it)
        }
        return true
    }
}
