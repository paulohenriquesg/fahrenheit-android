package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture

/**
 * The connected controller, as [PlayerViewModel] sees it (#221): the boundary
 * between the screen's state and playback, which stays in [PlaybackService].
 *
 * [player] is Media3's [Player], not the controller: the helpers that queue,
 * seek and read the position ([PlayerStart], [BookPlayback], [QueueEndWatch],
 * the transport) take one and stay as they are. The rest is what only the
 * controller has. Tests pass a test player and record [send].
 */
interface PlayerLink {
    /** The service's player: its queue, position, playing, media-item transitions. */
    val player: Player

    /** What the service reports beyond the player: the sleep timer. */
    val sessionExtras: Bundle

    /** A command to the service: Sleep, Mark finished. */
    fun send(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult>
}

/** The Activity's own: a [MediaController] it connected, and releases. */
class ControllerLink(private val controller: MediaController) : PlayerLink {
    override val player: Player get() = controller
    override val sessionExtras: Bundle get() = controller.sessionExtras
    override fun send(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> =
        controller.sendCustomCommand(command, args)
}
