package com.paulohenriquesg.fahrenheit.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Play that did not come through the player screen - the remote's Play on
 * Home or any other screen, or the system's - checked as the screen checks
 * its own (#144).
 *
 * @param check the question the queued item would raise, if any.
 * @param openPlayer opens the player screen on the queued item, which asks as
 *   it comes back to it (#142); false when it cannot - an activity cannot be
 *   started with the app in the background.
 * @param timeoutMs how long a press waits for the server before it plays anyway.
 */
class OutsidePlay(
    private val scope: CoroutineScope,
    private val check: suspend () -> ResumeOffer?,
    private val play: () -> Unit,
    private val openPlayer: () -> Boolean,
    private val timeoutMs: Long = 1_500
) {
    fun request() {
        scope.launch {
            val asked = withTimeoutOrNull(timeoutMs) { check() }
            // Asked where it can be; otherwise Play does what it always did.
            if (asked == null || !openPlayer()) play()
        }
    }
}
