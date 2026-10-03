package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * When playback moves on to the next episode by itself (#108), it starts
 * where that episode was left ([QueuedFile.startAt]), not at its beginning -
 * in the service, so with no screen open too.
 */
class ResumeOnArrival(private val player: Player) : Player.Listener {
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
        val file = QueuedFile.of(mediaItem) ?: return
        // Only the first file of what was arrived at; the start is in its time.
        val inFile = file.startAt - file.startOffset
        if (file.startAt <= 0.0 || inFile < 0) return
        player.seekTo(player.currentMediaItemIndex, (inFile * 1000).toLong())
    }
}
