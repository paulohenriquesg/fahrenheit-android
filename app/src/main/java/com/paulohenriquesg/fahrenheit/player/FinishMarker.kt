package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import android.os.Bundle
import androidx.media3.session.SessionCommand
import com.paulohenriquesg.fahrenheit.api.ProgressMark

/**
 * Mark finished and Mark unfinished, done by the playback service (#107).
 *
 * The server keeps two rules that shape this:
 * - any later update that moves an item's position un-finishes it - even the
 *   update that finishes it, so the mark carries no position;
 * - un-finishing puts the saved position back to 0.
 *
 * So finishing pauses, waits for the closing report to be delivered ([closed]),
 * and only then says finished. Un-finishing says so, then says where the
 * listener is, or the book would reopen at the start.
 *
 * @param send one progress update for an item or episode; throwing is a failure.
 */
class FinishMarker(
    private val player: Player,
    private val closed: suspend () -> Unit,
    private val send: suspend (itemId: String, episodeId: String?, mark: ProgressMark) -> Unit
) {
    suspend fun mark(finished: Boolean): Result<Unit> {
        val file = QueuedFile.of(player.currentMediaItem) ?: return Result.failure(IllegalStateException("nothing queued"))
        return try {
            if (finished) {
                player.pause()
                closed()
                send(file.itemId, file.episodeId, ProgressMark(isFinished = true))
            } else {
                val at = file.bookTime(player.currentPosition / 1000.0)
                send(file.itemId, file.episodeId, ProgressMark(isFinished = false))
                send(file.itemId, file.episodeId, ProgressMark(currentTime = at))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** The session command a screen sends to mark what is queued finished or not (#107). */
object FinishCommand {
    val COMMAND = SessionCommand("fahrenheit.FINISH", Bundle.EMPTY)
    private const val FINISHED = "fahrenheit.finish.finished"
    fun args(finished: Boolean): Bundle = Bundle().apply { putBoolean(FINISHED, finished) }
    fun finishedOf(args: Bundle): Boolean = args.getBoolean(FINISHED)
}

/**
 * Whether the book is finished, as About shows it; null for an episode.
 *
 * Playing a finished book moves its position, which the server takes as
 * un-finishing it, so starting to play does the same here.
 */
@Composable
fun rememberFinished(player: Player?, initial: Boolean?): MutableState<Boolean?> {
    // Kept across a new connection (Home and back): only new item facts reset it.
    val finished = remember(initial) { mutableStateOf(initial) }
    DisposableEffect(player, finished) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && finished.value == true) finished.value = false
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    return finished
}
