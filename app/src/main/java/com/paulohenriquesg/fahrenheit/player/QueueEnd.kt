package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player

/**
 * Playing to the end of the queue - the newest episode with nothing after it,
 * the end of a book - ends the listening session as Stop does (#179): the
 * queue empties, and Now playing goes with it. Left queued, the ended item
 * kept the rail's entry, and its Stop, until someone pressed it.
 *
 * Moving on to the next episode (#108) never reaches the end: Media3 moves to
 * the next item by itself.
 *
 * Install on the session's player, with [player] the [LeavingGuard] in front
 * of it. Acted on in [onEvents], after the other listeners have heard of the
 * end: the closing report has gone at the end by then, and the guard's
 * second close says nothing.
 */
class QueueEnd(private val player: Player) : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {
        if (!events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) return
        if (player.playbackState == Player.STATE_ENDED) Playback.end(this.player)
    }
}
