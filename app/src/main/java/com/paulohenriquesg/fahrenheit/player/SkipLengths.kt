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
 * read the setting at each press, so a change applies at once.
 *
 * A skip moves in whole-book time, as the player's buttons do: across the
 * files of a book stored in several, never past its start or end. A
 * controller already connected keeps the increments it was told on
 * connecting; the presses themselves always use the setting.
 */
@OptIn(UnstableApi::class) // ForwardingPlayer: the documented way to intercept session commands.
class SkipLengths(
    player: Player,
    private val backSeconds: () -> Int,
    private val forwardSeconds: () -> Int
) : ForwardingPlayer(player) {

    override fun getSeekBackIncrement(): Long = backSeconds() * 1000L

    override fun getSeekForwardIncrement(): Long = forwardSeconds() * 1000L

    override fun seekBack() = skipBy(-seekBackIncrement)

    override fun seekForward() = skipBy(seekForwardIncrement)

    private fun skipBy(ms: Long) {
        val file = QueuedFile.of(currentMediaItem) ?: return skipInFile(ms)
        val target = (file.bookTime(currentPosition / 1000.0) + ms / 1000.0).coerceIn(0.0, file.bookTotal)
        // The file of this book that holds the target: the last one starting at or before it.
        val index = (0 until mediaItemCount).lastOrNull { i ->
            QueuedFile.of(getMediaItemAt(i))?.let { it.isFor(file.itemId, file.episodeId) && it.startOffset <= target } == true
        } ?: return skipInFile(ms)
        val start = QueuedFile.of(getMediaItemAt(index))!!.startOffset
        seekTo(index, ((target - start) * 1000).toLong())
    }

    /** Something not ours: as ExoPlayer does, within the item playing. */
    private fun skipInFile(ms: Long) {
        val target = (currentPosition + ms).coerceAtLeast(0)
        val length = duration
        seekTo(if (length != C.TIME_UNSET) minOf(target, length) else target)
    }
}
