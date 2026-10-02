package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
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
 */
class ProgressReporter(
    private val send: suspend (MediaProgressRequest) -> Unit,
    private val position: () -> Double,
    private val total: () -> Double,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) }
) {
    private var lastSent: Double? = null

    /** Whether anything has played; a reporter that never played has nothing to say. */
    private var played = false

    suspend fun run(isPlaying: () -> Boolean) {
        while (isPlaying()) {
            played = true
            pause()
            val request = ProgressSync.next(position(), total(), lastSent) ?: continue
            // Only a delivered position counts as sent; a failed one is retried.
            if (runCatching { send(request) }.isSuccess) lastSent = request.currentTime
        }
    }

    /**
     * The closing update, when playback stops or the queue is about to change.
     *
     * Without it up to one round of listening is lost on every pause. The
     * position is read before anything suspends, so a caller that starts this
     * undispatched captures the position as it is now, before the queue
     * changes under it. Not retried: there is no next round to retry in.
     */
    suspend fun finish() {
        if (!played) return
        val request = ProgressSync.next(position(), total(), lastSent) ?: return
        lastSent = request.currentTime
        runCatching { send(request) }
    }
}
