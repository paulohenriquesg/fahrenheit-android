package com.paulohenriquesg.fahrenheit.player

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reports the listening of whatever [player] is playing: where, and for how long.
 *
 * One [ProgressReporter] per item: its ids come from the queued file, so a
 * report can never go to a book other than the one it measured. Positions are
 * whole-book time ([QueuedFile.bookTime]), never time within one file.
 *
 * Install as a listener on the player, and pass [beforeLeaving] to the
 * [LeavingGuard] in front of it.
 *
 * Each item's reports go to its own [ListeningDelivery] - in the app, a
 * listening session - with the seconds actually played ([ListeningTime]).
 *
 * @param open the delivery for an item, made when it first plays.
 * @param now a monotonic clock in milliseconds, for listening time.
 */
class PlaybackReporting(
    private val player: Player,
    private val scope: CoroutineScope,
    private val open: (QueuedFile) -> ListeningDelivery,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) },
    private val now: () -> Long = { SystemClock.elapsedRealtime() },
    private val closings: Closings = Closings.process,
    private val delivered: (QueuedFile) -> Unit = {}
) : Player.Listener {

    private var reportingFor: QueuedFile? = null
    private var reporter: ProgressReporter? = null
    private var time: ListeningTime? = null
    private var rounds: Job? = null
    /** Where the item just left ended, for its closing report: the live position already belongs to the next. */
    private var endedAt: Double? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        when {
            isPlaying -> start()
            // Buffering - after a seek, on a slow network - is not a stop: the
            // listener still wants to play. Closing here would end the session
            // on every skip and open a new one at the next report.
            player.playWhenReady && player.playbackState == Player.STATE_BUFFERING -> time?.playing(false)
            else -> finish()
        }
    }

    // A pause during buffering: the player already reads as not playing, so
    // onIsPlayingChanged does not fire again, and only this says it stopped.
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady && !player.isPlaying) finish()
    }

    /** The queue is about to be stopped, replaced or cleared. */
    fun beforeLeaving() = finish()

    /**
     * Media3 moved on to another item by itself - the next episode (#108) -
     * or by a skip, without isPlaying changing: close what was playing, at
     * its end when it played through, and report what plays now. The next
     * file of the same book is not a move.
     */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val owner = reportingFor ?: return
        val arrived = QueuedFile.of(mediaItem)
        if (arrived != null && arrived.isFor(owner.itemId, owner.episodeId)) return
        endedAt = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) owner.bookTotal else null
        finish()
        endedAt = null
        reporter = null
        reportingFor = null
        if (player.isPlaying) start()
    }

    /**
     * Closes this stretch of listening now and waits until its report is
     * delivered (or has failed): Mark finished must reach the server after it,
     * or the report's new position would un-finish the item again.
     */
    suspend fun closeAndWait() {
        finish()
        closings.settled()
    }

    private fun start() {
        val current = QueuedFile.of(player.currentMediaItem) ?: return
        val owner = reportingFor
        if (reporter == null || owner == null || !owner.isFor(current.itemId, current.episodeId)) {
            reportingFor = current
            val delivery = open(current)
            val listening = ListeningTime(now).also { time = it }
            reporter = ProgressReporter(
                send = delivery::sync,
                position = { positionIn(current) },
                total = { current.bookTotal },
                pause = pause,
                listened = listening::pending,
                delivered = listening::delivered,
                close = delivery::close
            )
        }
        time?.playing(true)
        if (rounds?.isActive == true) return
        val active = reporter ?: return
        rounds = scope.launch { active.run { player.isPlaying } }
    }

    private fun finish() {
        rounds?.cancel()
        rounds = null
        time?.playing(false)
        val active = reporter ?: return
        // Undispatched, so the position is read now, before the queue changes.
        // Not cancellable: on Back the service, and its scope, are gone
        // within milliseconds, which would drop the report mid-send.
        val closing = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { active.finish() }
        }
        closings.track(closing)
    }

    /**
     * Whole-book time; once the player has moved on to something else, where
     * this item ended if it played through, else -1 (never sent).
     */
    private fun positionIn(owner: QueuedFile): Double {
        val now = QueuedFile.of(player.currentMediaItem) ?: return endedAt ?: -1.0
        if (!now.isFor(owner.itemId, owner.episodeId)) return endedAt ?: -1.0
        return now.bookTime(player.currentPosition / 1000.0)
    }
}

/**
 * Closing reports still on their way, for the whole process: Back stops
 * playback and the service goes, but its last report is still being sent, and
 * a Mark finished made meanwhile must wait for it (see [FinishMarker]).
 * Main thread only.
 */
class Closings {
    private val jobs = mutableSetOf<Job>()

    fun track(job: Job) {
        if (!job.isActive) return
        jobs += job
        job.invokeOnCompletion { jobs -= job }
    }

    /** Returns once every closing report tracked so far is delivered, or has failed. */
    suspend fun settled() = jobs.toList().joinAll()

    companion object {
        val process = Closings()
    }
}
