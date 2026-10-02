package com.paulohenriquesg.fahrenheit.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** The app's way to the [PlaybackService]. */
object Playback {

    fun connect(context: Context): ListenableFuture<MediaController> =
        MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .buildAsync()

    /**
     * The player screen is going away. Back ([finishing]) stops playback - with
     * no mini-player yet there would be no other way to stop it; Home leaves it
     * playing. Either way the controller is released by the caller afterwards.
     */
    fun leave(controller: Player, finishing: Boolean) {
        if (!finishing) return
        controller.stop()
        controller.clearMediaItems()
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
