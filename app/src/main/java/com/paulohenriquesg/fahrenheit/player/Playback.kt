package com.paulohenriquesg.fahrenheit.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** The app's way to the [PlaybackService]. */
object Playback {

    /** @param listener hears what the service reports beyond the player, such as the sleep timer. */
    fun connect(context: Context, listener: MediaController.Listener? = null): ListenableFuture<MediaController> =
        MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .apply { listener?.let(::setListener) }
            .buildAsync()

    /**
     * The player screen is going away. Back ([finishing]) stops playback - the
     * rail's Now playing entry leads back to the player, but is not a control
     * (#107); Home leaves it playing. Either way the controller is released by the caller afterwards.
     */
    fun leave(controller: Player, finishing: Boolean) {
        if (!finishing) return
        controller.stop()
        controller.clearMediaItems()
    }

    /**
     * Marks a book finished or not from outside the player - its details
     * screen (#105) - through the service, which knows whether that book is
     * playing and must first be paused (see [FinishMarker]).
     */
    /** @param keepAt where an un-finished book was, to keep it there (see [FinishMarker.mark]). */
    fun markFinished(context: Context, itemId: String, finished: Boolean, keepAt: Double? = null, onDone: (Boolean) -> Unit) {
        val future = connect(context.applicationContext)
        val main = ContextCompat.getMainExecutor(context)
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener onDone(false)
            val answer = controller.sendCustomCommand(FinishCommand.COMMAND, FinishCommand.args(finished, itemId, keepAt = keepAt))
            answer.addListener({
                onDone(runCatching { answer.get().resultCode == SessionResult.RESULT_SUCCESS }.getOrDefault(false))
                controller.release()
            }, main)
        }, main)
    }

    /** Stops whatever is playing, from anywhere - sign-out uses it. */
    fun stop(context: Context) {
        val future = connect(context.applicationContext)
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            leave(controller, finishing = true)
            controller.release()
        }, ContextCompat.getMainExecutor(context))
    }
}
