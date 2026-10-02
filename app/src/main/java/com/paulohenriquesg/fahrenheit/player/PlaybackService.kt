package com.paulohenriquesg.fahrenheit.player

import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import retrofit2.awaitResponse

/**
 * Where playback lives, so it outlives the player screen (#16).
 *
 * The screen drives it through a MediaController (see [Playback]); so do the
 * remote's media keys and the system's controls. It knows nothing about books:
 * each queued file carries its own [QueuedFile] facts, and progress is
 * reported from those.
 */
class PlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaSession? = null

    /** The player the session drives; for tests. */
    internal val sessionPlayer: Player? get() = session?.player

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(AudioHttp.dataSourceFactory { ApiClient.audioHttpClient() })
            )
            // Speech, and audio focus handled: another app taking the audio pauses this one.
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            // Keeps streaming when a TV's screensaver starts.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        val reporting = PlaybackReporting(exo, scope, send = ::sendProgress)
        exo.addListener(reporting)
        exo.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                // Nothing queued: nothing to keep the service for.
                if (timeline.isEmpty) stopSelf()
            }
        })
        session = MediaSession.Builder(this, LeavingGuard(exo, reporting::beforeLeaving))
            .setCallback(PlaybackSessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun sendProgress(file: QueuedFile, request: MediaProgressRequest) {
        val api = ApiClient.getApiService() ?: error("signed out")
        val call = file.episodeId?.let { api.userCreateOrUpdateMediaProgress(file.itemId, it, request) }
            ?: api.userCreateOrUpdateMediaProgress(file.itemId, request)
        val response = call.awaitResponse()
        // Logged, not shown: the next round retries.
        if (!response.isSuccessful) {
            Log.w(TAG, "Progress rejected: ${response.code()}")
            error("progress rejected: ${response.code()}")
        }
    }

    private companion object {
        const val TAG = "PlaybackService"
    }
}

/**
 * The session's answers to controllers.
 *
 * Connections keep Media3's default on purpose: only this app, the system and
 * apps holding the media-control permission may queue anything. Queued audio
 * is fetched with the listener's token, so an app that could queue a URL of
 * its own could collect that token. PlaybackServiceTest pins this.
 *
 * Items arriving from a controller in another process have lost their URI;
 * [PlayableItems] rebuilds it.
 */
internal object PlaybackSessionCallback : MediaSession.Callback {
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val playable = PlayableItems.resolve(mediaItems)
            ?: return Futures.immediateFailedFuture(UnsupportedOperationException("An item has no URI"))
        return Futures.immediateFuture(playable.toMutableList())
    }
}
