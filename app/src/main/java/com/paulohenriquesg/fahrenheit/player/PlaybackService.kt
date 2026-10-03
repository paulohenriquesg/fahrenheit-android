package com.paulohenriquesg.fahrenheit.player

import androidx.annotation.OptIn
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.paulohenriquesg.fahrenheit.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private var sleep: SleepWatch? = null
    private var sleeping: Job? = null

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
        // Read for each file rather than once, so a rename in Settings names
        // the next session without restarting the service.
        val reporting = PlaybackReporting(exo, scope, open = {
            ListeningSession(it, ApiClient::getApiService, PlaybackDevice.info(this))
        })
        exo.addListener(reporting)
        val watch = SleepWatch(exo, now = { SystemClock.elapsedRealtime() }, publish = { session?.setSessionExtras(it) })
        exo.addListener(watch)
        sleep = watch
        exo.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                // Nothing queued: nothing to keep the service for.
                if (timeline.isEmpty) stopSelf()
            }
        })
        val guarded = LeavingGuard(exo) {
            reporting.beforeLeaving()
            watch.beforeLeaving()
        }
        val marker = FinishMarker(exo, reporting::closeAndWait) { itemId, episodeId, mark ->
            val api = ApiClient.getLibraryApi() ?: error("signed out")
            if (episodeId != null) api.markFinished(itemId, episodeId, mark) else api.markFinished(itemId, mark)
        }
        session = MediaSession.Builder(this, guarded)
            .setCallback(PlaybackSessionCallback(onFinish = { finished -> markFinished(marker, finished) }, onSleep = ::setSleep))
            .build()
    }

    /** Mark finished or unfinished; the answer comes once the server has it. */
    private fun markFinished(marker: FinishMarker, finished: Boolean): ListenableFuture<SessionResult> {
        val answer = SettableFuture.create<SessionResult>()
        scope.launch {
            val result = marker.mark(finished)
            answer.set(SessionResult(if (result.isSuccess) SessionResult.RESULT_SUCCESS else SessionError.ERROR_UNKNOWN))
        }
        return answer
    }

    /** Sets or clears the sleep timer, and ticks it while it runs. */
    private fun setSleep(args: Bundle) {
        val watch = sleep ?: return
        watch.set(args)
        if (sleeping?.isActive == true) return
        sleeping = scope.launch {
            while (watch.running) {
                delay(watch.nextCheckMs())
                watch.check()
            }
        }
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
 *
 * Trusted controllers - the app's own screen - may also set the sleep
 * timer ([SleepCommand]), handed to [onSleep], and mark what is queued
 * finished ([FinishCommand]), handed to [onFinish].
 */
internal class PlaybackSessionCallback(
    private val onFinish: (Boolean) -> ListenableFuture<SessionResult> = {
        Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
    },
    private val onSleep: (Bundle) -> Unit
) : MediaSession.Callback {
    // Media3's own default, trusted or not, plus the sleep timer and Mark
    // finished for the trusted. onConnectAsync, not onConnect: Media3's
    // default onConnectAsync never calls onConnect, so an override there is
    // reached only by the session's internal fallback and not by callers of
    // onConnectAsync.
    @OptIn(UnstableApi::class) // the trust-aware default builder and its commands
    override fun onConnectAsync(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.ConnectionResult> {
        val accepted = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
        if (controller.isTrusted) {
            accepted.setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SleepCommand.COMMAND)
                    .add(FinishCommand.COMMAND)
                    .build()
            )
        }
        return Futures.immediateFuture(accepted.build())
    }

    @OptIn(UnstableApi::class) // ControllerInfo.isTrusted
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        val ours = customCommand.customAction == SleepCommand.COMMAND.customAction ||
            customCommand.customAction == FinishCommand.COMMAND.customAction
        if (!ours) return super.onCustomCommand(session, controller, customCommand, args)
        // Media3 already refuses a command it did not offer; this says so here too.
        if (!controller.isTrusted) return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
        if (customCommand.customAction == FinishCommand.COMMAND.customAction) return onFinish(FinishCommand.finishedOf(args))
        onSleep(args)
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

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
