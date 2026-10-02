package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reports the listening position of whatever [player] is playing.
 *
 * One [ProgressReporter] per item: its ids come from the queued file, so a
 * report can never go to a book other than the one it measured. Positions are
 * whole-book time ([QueuedFile.bookTime]), never time within one file.
 *
 * Install as a listener on the player, and pass [beforeLeaving] to the
 * [LeavingGuard] in front of it.
 *
 * @param send delivers one report for that file's item or episode; throwing is a failed send.
 */
class PlaybackReporting(
    private val player: Player,
    private val scope: CoroutineScope,
    private val send: suspend (QueuedFile, MediaProgressRequest) -> Unit,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) }
) : Player.Listener {

    private var reportingFor: QueuedFile? = null
    private var reporter: ProgressReporter? = null
    private var rounds: Job? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) start() else finish()
    }

    /** The queue is about to be stopped, replaced or cleared. */
    fun beforeLeaving() = finish()

    private fun start() {
        val current = QueuedFile.of(player.currentMediaItem) ?: return
        val owner = reportingFor
        if (reporter == null || owner == null || !owner.isFor(current.itemId, current.episodeId)) {
            reportingFor = current
            reporter = ProgressReporter(
                send = { send(current, it) },
                position = { positionIn(current) },
                total = { current.bookTotal },
                pause = pause
            )
        }
        if (rounds?.isActive == true) return
        val active = reporter ?: return
        rounds = scope.launch { active.run { player.isPlaying } }
    }

    private fun finish() {
        rounds?.cancel()
        rounds = null
        val active = reporter ?: return
        // Undispatched, so the position is read now, before the queue changes.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { active.finish() }
    }

    /** Whole-book time, or -1 (never sent) once the player has moved on to something else. */
    private fun positionIn(owner: QueuedFile): Double {
        val now = QueuedFile.of(player.currentMediaItem) ?: return -1.0
        if (!now.isFor(owner.itemId, owner.episodeId)) return -1.0
        return now.bookTime(player.currentPosition / 1000.0)
    }
}
