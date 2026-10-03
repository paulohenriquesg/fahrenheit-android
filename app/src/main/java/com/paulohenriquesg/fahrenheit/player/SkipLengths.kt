package com.paulohenriquesg.fahrenheit.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * The session's player, skipping by the lengths set in Settings (#107).
 *
 * The remote's rewind and fast-forward keys reach the session as seekBack and
 * seekForward. ExoPlayer's own increments are fixed when it is built; these
 * read the setting at each press, so a change applies at once. Like
 * ExoPlayer's, a skip stays within the file playing.
 */
@OptIn(UnstableApi::class) // ForwardingPlayer: the documented way to intercept session commands.
class SkipLengths(
    player: Player,
    private val backSeconds: () -> Int,
    private val forwardSeconds: () -> Int
) : ForwardingPlayer(player) {

    override fun getSeekBackIncrement(): Long = backSeconds() * 1000L

    override fun getSeekForwardIncrement(): Long = forwardSeconds() * 1000L

    override fun seekBack() {
        seekTo((currentPosition - seekBackIncrement).coerceAtLeast(0))
    }

    override fun seekForward() {
        val target = currentPosition + seekForwardIncrement
        val length = duration
        seekTo(if (length != C.TIME_UNSET) minOf(target, length) else target)
    }
}
