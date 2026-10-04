package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * When to ask which position to continue from, and what each answer does,
 * for one item on the player screen (#90). Apart from the screen, so the
 * races can be tested without it.
 *
 * @param check the question to ask now, if any ([ResumeCheck], bound to the
 *   item and the player's position).
 * @param playWhenReady whether the player is set to play: what it is doing
 *   then is newer than anything the server holds.
 * @param timeoutMs how long Play waits for the server before it plays anyway.
 */
class ResumePrompt(
    private val scope: CoroutineScope,
    private val check: suspend () -> ResumeOffer?,
    private val playWhenReady: () -> Boolean,
    private val play: () -> Unit,
    private val seek: (Double) -> Unit,
    private val answered: (ResumeOffer) -> Unit,
    private val timeoutMs: Long = 1_500
) {
    /** The question showing, if any. */
    var offer: ResumeOffer? by mutableStateOf(null)
        private set

    private var playAfterAnswer = false
    /** One check at a time: a newer reason to ask replaces an older one. */
    private var checking: Job? = null
    /** Whether the remote's last Play down was taken here, so its up is too. */
    private var tookDown = false

    /**
     * The screen came back to this item, queued already.
     *
     * @param playAfter opened by a Play from outside the player (#144): the
     *   press is kept, and plays once answered, or at once with nothing to ask.
     */
    fun onReattach(playAfter: Boolean = false) {
        if (playWhenReady() || offer != null) return
        ask(playAfter)
    }

    /** Play from a pause. */
    fun onPlay() {
        if (offer != null) return
        ask(playAfter = true)
    }

    /**
     * The remote's Play key, before the media session sees it.
     *
     * @return whether it was taken here.
     */
    fun onKey(down: Boolean): Boolean {
        if (!down) return tookDown.also { tookDown = false }
        tookDown = when {
            // The question is up: nothing plays behind it.
            offer != null -> true
            playWhenReady() -> false
            else -> true.also { onPlay() }
        }
        return tookDown
    }

    fun answer(moveThere: Boolean) {
        val asked = offer ?: return
        if (moveThere) seek(asked.there)
        // Staying sends nothing: reports start only when playback moves.
        answered(asked)
        offer = null
        if (playAfterAnswer) play()
    }

    private fun ask(playAfter: Boolean) {
        checking?.cancel()
        checking = scope.launch {
            // A server slow to answer must not leave Play doing nothing.
            val found = withTimeoutOrNull(timeoutMs) { check() }
            // Started meanwhile, by the listener: theirs is the newer choice.
            if (!playAfter && playWhenReady()) return@launch
            if (found == null) {
                if (playAfter) play()
            } else {
                playAfterAnswer = playAfter
                offer = found
            }
        }
    }
}
