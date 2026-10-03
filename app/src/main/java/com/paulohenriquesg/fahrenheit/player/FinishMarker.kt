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
 * An item that is not the one queued - Mark finished on another book's details
 * screen (#105) - is marked directly: nothing reports on it, and what plays
 * keeps playing.
 *
 * @param send one progress update for an item or episode; throwing is a failure.
 */
class FinishMarker(
    private val player: Player,
    private val closed: suspend () -> Unit,
    private val pending: suspend () -> Unit = {},
    private val send: suspend (itemId: String, episodeId: String?, mark: ProgressMark) -> Unit
) {
    /** @param itemId the item to mark; null for whatever is queued. */
    /**
     * @param keepAt for an item not queued being un-finished: where it was,
     *   sent back after the server has put it to 0. Null leaves it at 0.
     */
    suspend fun mark(finished: Boolean, itemId: String? = null, episodeId: String? = null, keepAt: Double? = null): Result<Unit> {
        val queued = QueuedFile.of(player.currentMediaItem)
        if (itemId != null && queued?.isFor(itemId, episodeId) != true) return markElsewhere(finished, itemId, episodeId, keepAt)
        val file = queued ?: return Result.failure(IllegalStateException("nothing queued"))
        return attempt {
            if (finished) {
                player.pause()
                closed()
                send(file.itemId, file.episodeId, ProgressMark(isFinished = true))
            } else {
                val at = file.bookTime(player.currentPosition / 1000.0)
                send(file.itemId, file.episodeId, ProgressMark(isFinished = false))
                send(file.itemId, file.episodeId, ProgressMark(currentTime = at))
            }
        }
    }

    /**
     * An item not queued. It may have been playing a moment ago - Back, then
     * Mark finished on its details screen - with its closing report still on
     * the way, which would un-finish it: so that goes first ([pending]).
     */
    private suspend fun markElsewhere(finished: Boolean, itemId: String, episodeId: String?, keepAt: Double?) = attempt {
        pending()
        send(itemId, episodeId, ProgressMark(isFinished = finished))
        if (!finished && keepAt != null) send(itemId, episodeId, ProgressMark(currentTime = keepAt))
    }

    private suspend fun attempt(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** The session command a screen sends to mark an item finished or not - the one named, or whatever is queued (#107, #105). */
object FinishCommand {
    val COMMAND = SessionCommand("fahrenheit.FINISH", Bundle.EMPTY)
    private const val FINISHED = "fahrenheit.finish.finished"
    private const val ITEM = "fahrenheit.finish.itemId"
    private const val EPISODE = "fahrenheit.finish.episodeId"
    private const val KEEP_AT = "fahrenheit.finish.keepAt"

    /**
     * @param itemId the item to mark; null for whatever is queued.
     * @param keepAt see [FinishMarker.mark].
     */
    fun args(finished: Boolean, itemId: String? = null, episodeId: String? = null, keepAt: Double? = null): Bundle = Bundle().apply {
        keepAt?.let { putDouble(KEEP_AT, it) }
        putBoolean(FINISHED, finished)
        itemId?.let { putString(ITEM, it) }
        episodeId?.let { putString(EPISODE, it) }
    }

    fun itemOf(args: Bundle): String? = args.getString(ITEM)
    fun episodeOf(args: Bundle): String? = args.getString(EPISODE)
    fun keepAtOf(args: Bundle): Double? = if (args.containsKey(KEEP_AT)) args.getDouble(KEEP_AT) else null
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
