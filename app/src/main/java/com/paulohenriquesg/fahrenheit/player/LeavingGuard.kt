package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * The session's player, announcing a stop, a new queue or an empty one before
 * it happens.
 *
 * The closing progress report has to read the position of what was playing.
 * A listener hears about these changes only after the queue has moved on, so
 * the report would go out with the next book's position, or none.
 */
class LeavingGuard(player: Player, private val beforeLeaving: () -> Unit) : ForwardingPlayer(player) {

    override fun stop() { beforeLeaving(); super.stop() }

    override fun clearMediaItems() { beforeLeaving(); super.clearMediaItems() }

    override fun setMediaItem(mediaItem: MediaItem) { beforeLeaving(); super.setMediaItem(mediaItem) }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        beforeLeaving(); super.setMediaItem(mediaItem, startPositionMs)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        beforeLeaving(); super.setMediaItem(mediaItem, resetPosition)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) { beforeLeaving(); super.setMediaItems(mediaItems) }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        beforeLeaving(); super.setMediaItems(mediaItems, resetPosition)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        beforeLeaving(); super.setMediaItems(mediaItems, startIndex, startPositionMs)
    }
}
