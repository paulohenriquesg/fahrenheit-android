package com.paulohenriquesg.fahrenheit.player

import androidx.annotation.OptIn
import android.os.Bundle
import android.util.Log
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import android.content.Intent
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
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
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
    private var stopWatchingSettings: () -> Unit = {}

    /** The player the session drives; for tests. */
    internal val sessionPlayer: Player? get() = session?.player

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            // The next episode starts where it was left, with or without a screen (#108, #171).
            .setMediaSourceFactory(
                StartWhereLeft(
                    DefaultMediaSourceFactory(this)
                        .setDataSourceFactory(AudioHttp.dataSourceFactory { ApiClient.audioHttpClient() })
                )
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
        val reporting = PlaybackReporting(
            exo,
            scope,
            open = { ListeningSession(it, ApiClient::getApiService, PlaybackDevice.info(this)) },
            // Known by the position written, not the time: the server stamps
            // its own clock, which the app never sees (#145).
            delivered = { file, position -> ServerKnowledge.process.wrote(file.itemId, file.episodeId, position) }
        )
        exo.addListener(reporting)
        val watch = SleepWatch(exo, now = { SystemClock.elapsedRealtime() }, publish = { session?.setSessionExtras(it) })
        exo.addListener(watch)
        // Where the next episode was left is for arriving once (#171).
        exo.addListener(SavedPlaceSpent(exo))
        val settings = PlayerSettings(this)
        // And the one after it is queued, with or without a screen (#160).
        val nextEpisode = NextEpisodeQueue(exo, scope, enabled = { settings.playNextEpisode }) { file ->
            val episodeId = file.episodeId ?: return@NextEpisodeQueue null
            val item = ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(file.itemId).getOrNull() }
            // Said in the log: with no screen, playback simply stops after this episode.
            if (item == null) Log.w(TAG, "Couldn't read ${file.itemId} for the next episode")
            item?.let { UpNext.after(it, episodeId, ResumeSources::saved, ApiClient::generateFullUrl) }
        }
        exo.addListener(nextEpisode)
        stopWatchingSettings = settings.onPlayNextEpisodeChanged(nextEpisode::settingChanged)
        sleep = watch
        exo.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                // Nothing queued: nothing to keep the service for.
                if (timeline.isEmpty) stopSelf()
            }
        })
        val skipping = SkipLengths(exo, { settings.skipBackSeconds }, { settings.skipForwardSeconds })
        val guarded = LeavingGuard(skipping) {
            reporting.beforeLeaving()
            watch.beforeLeaving()
        }
        // The end of the queue ends the session, as Stop does (#179).
        exo.addListener(QueueEnd(guarded))
        val marker = FinishMarker(
            exo,
            reporting::closeAndWait,
            pending = Closings.process::settled,
            wrote = ServerKnowledge.process::wrote
        ) { itemId, episodeId, mark ->
            val api = ApiClient.getLibraryApi() ?: error("signed out")
            if (episodeId != null) api.markFinished(itemId, episodeId, mark) else api.markFinished(itemId, mark)
        }
        session = MediaSession.Builder(this, guarded)
            .setCallback(
                PlaybackSessionCallback(
                    onFinish = { args -> markFinished(marker, args) },
                    onSleep = ::setSleep,
                    outsidePlay = { holdOutsidePlay(exo) }
                )
            )
            .build()
    }

    /** One at a time: presses during a check are the same press (#144). */
    private val outsidePlay by lazy {
        OutsidePlay(
            scope = scope,
            check = {
                val player = session?.player
                val file = QueuedFile.of(player?.currentMediaItem)
                if (player == null || file == null) null
                else outsideCheck.offer(file.itemId, file.episodeId, file.bookTime(player.currentPosition / 1000.0), playing = false)
            },
            play = { session?.player?.play() },
            openPlayer = openPlayer@{
                val file = QueuedFile.of(session?.player?.currentMediaItem) ?: return@openPlayer false
                if (!AppVisibility.process.visible) return@openPlayer false
                // It asks as it comes back to the item, and plays after the answer.
                startActivity(
                    PlayerActivity.createIntent(this, file.itemId, file.episodeId, askThenPlay = true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }
        )
    }

    private val outsideCheck by lazy {
        ResumeCheck(
            progress = ResumeSources::progress,
            latestSession = ResumeSources::latestSession,
            thisDevice = PlaybackDevice.info(this).deviceId.orEmpty()
        )
    }

    /**
     * Holds a Play from outside the app's screens for the check (#144); false
     * lets it play as it is: nothing of ours queued, or the player screen
     * showing, which asks for itself rather than open over itself.
     */
    private fun holdOutsidePlay(player: Player): Boolean {
        QueuedFile.of(player.currentMediaItem) ?: return false
        if (AppVisibility.process.playerVisible) return false
        outsidePlay.request()
        return true
    }

    /** Mark finished or unfinished; the answer comes once the server has it. */
    private fun markFinished(marker: FinishMarker, args: Bundle): ListenableFuture<SessionResult> {
        val answer = SettableFuture.create<SessionResult>()
        scope.launch {
            val result = marker.mark(
                FinishCommand.finishedOf(args), FinishCommand.itemOf(args), FinishCommand.episodeOf(args), FinishCommand.keepAtOf(args)
            )
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

    private companion object {
        const val TAG = "PlaybackService"
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        stopWatchingSettings()
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
 * timer ([SleepCommand]), handed to [onSleep], and mark an item finished -
 * the one named, or whatever is queued ([FinishCommand]) - handed to [onFinish].
 */
internal class PlaybackSessionCallback(
    private val onFinish: (Bundle) -> ListenableFuture<SessionResult> = {
        Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
    },
    private val outsidePlay: () -> Boolean = { false },
    // Last, so `PlaybackSessionCallback { ... }` still names the sleep timer.
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

    // Play from anything but the app's screens - the remote's keys and the
    // system's controls, which arrive through the notification controller
    // under this app's own package, launcher cards - is held and checked
    // first (#144); the player screen checks its own (#142). Deprecated in
    // 1.11 but still called; its successor needs a forwarding player.
    @Suppress("DEPRECATION")
    override fun onPlayerCommandRequest(session: MediaSession, controller: MediaSession.ControllerInfo, playerCommand: Int): Int {
        val play = playerCommand == Player.COMMAND_PLAY_PAUSE && !session.player.playWhenReady
        val appScreen = controller.connectionHints.getBoolean(Playback.APP_SCREEN_HINT)
        if (play && !appScreen && outsidePlay()) return SessionResult.RESULT_INFO_SKIPPED
        return super.onPlayerCommandRequest(session, controller, playerCommand)
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
        if (customCommand.customAction == FinishCommand.COMMAND.customAction) return onFinish(args)
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
