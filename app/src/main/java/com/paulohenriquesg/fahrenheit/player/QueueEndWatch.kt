package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player

/**
 * Tells the player screen the queue it shows has ended - played to its end,
 * when the service empties it ([QueueEnd]) - so the screen can close (#179).
 *
 * Only the end of what the screen shows: a queue that held [itemId] and
 * [episodeId] and is now empty. A queue replaced is not an end, and neither is
 * another book's or episode's going - the screen before a switch ends its own
 * as this one connects.
 */
class QueueEndWatch(
    private val player: Player,
    private val itemId: String,
    private val episodeId: String?,
    private val onEnded: () -> Unit
) : Player.Listener {
    private var held = holds()

    override fun onEvents(player: Player, events: Player.Events) {
        val ended = held && this.player.currentTimeline.isEmpty
        held = holds()
        if (ended) onEnded()
    }

    private fun holds() = QueuedFile.of(player.currentMediaItem)?.isFor(itemId, episodeId) == true
}
