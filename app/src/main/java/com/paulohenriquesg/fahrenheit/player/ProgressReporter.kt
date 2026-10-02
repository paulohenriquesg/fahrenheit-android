package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import kotlinx.coroutines.delay

/**
 * Sends the listening position while something plays, by [ProgressSync]'s rules.
 *
 * One class for books and episodes: it was written once per player, and the
 * book player ran two copies at once whenever playback started from the
 * remote's media button. Kept out of the Activity, so a player that outlives a
 * screen (#16) can own it unchanged.
 *
 * @param send the request for this item or episode; throwing is a failed send.
 */
class ProgressReporter(
    private val send: suspend (MediaProgressRequest) -> Unit,
    private val position: () -> Double,
    private val total: () -> Double,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) }
) {
    suspend fun run(isPlaying: () -> Boolean) {
        var lastSent: Double? = null
        while (isPlaying()) {
            pause()
            val request = ProgressSync.next(position(), total(), lastSent) ?: continue
            // Only a delivered position counts as sent; a failed one is retried.
            if (runCatching { send(request) }.isSuccess) lastSent = request.currentTime
        }
    }
}
