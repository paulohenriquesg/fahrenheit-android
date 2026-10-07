package com.paulohenriquesg.fahrenheit.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** The app's way to the [PlaybackService]. */
object Playback {
    /**
     * Carried by the app's own controllers. The remote's media keys and the
     * system's controls reach the session through Media3's notification
     * controller, which has the app's package too; this tells them apart (#144).
     */
    const val APP_SCREEN_HINT = "com.paulohenriquesg.fahrenheit.APP_SCREEN"

    fun connectionHints(): Bundle = Bundle().apply { putBoolean(APP_SCREEN_HINT, true) }

    /** @param listener hears what the service reports beyond the player, such as the sleep timer. */
    fun connect(context: Context, listener: MediaController.Listener? = null): ListenableFuture<MediaController> =
        MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setConnectionHints(connectionHints())
            .apply { listener?.let(::setListener) }
            .buildAsync()

    /**
     * Ends the listening session: stops, and empties the queue. The session's
     * player sends the closing progress report first (see [LeavingGuard]).
     *
     * Leaving the player screen - Back or Home - does not end it (#155); Stop on
     * the rail's Now playing entry does, as do a switch to another book and sign-out.
     */
    fun end(controller: Player) {
        controller.stop()
        controller.clearMediaItems()
    }

    /**
     * Marks a book or an episode finished or not from outside the player - its details
     * screen (#105) - through the service, which knows whether that book is
     * playing and must first be paused (see [FinishMarker]).
     *
     * @param episodeId the episode, for a podcast's (#181); the one playing goes
     *   through the same pause and closing report as a book.
     * @param keepAt where an un-finished book was, to keep it there (see [FinishMarker.mark]).
     */
    fun markFinished(
        context: Context,
        itemId: String,
        finished: Boolean,
        episodeId: String? = null,
        keepAt: Double? = null,
        onDone: (Boolean) -> Unit
    ) {
        val future = connect(context.applicationContext)
        val main = ContextCompat.getMainExecutor(context)
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener onDone(false)
            val answer = controller.sendCustomCommand(FinishCommand.COMMAND, FinishCommand.args(finished, itemId, episodeId, keepAt))
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
            end(controller)
            controller.release()
        }, ContextCompat.getMainExecutor(context))
    }
}
