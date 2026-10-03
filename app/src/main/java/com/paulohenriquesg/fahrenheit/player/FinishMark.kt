package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.FinishedRequest
import com.paulohenriquesg.fahrenheit.api.LibraryApi

/**
 * Mark finished and Mark unfinished, from About (#107).
 *
 * The server un-finishes an item on any later update that moves its
 * currentTime, and a playing book keeps reporting. So [finished] pauses first
 * - which sends the closing report - and marks the item at that same paused
 * position: whichever reaches the server first, the other moves nothing.
 */
class FinishMark(private val api: LibraryApi) {

    suspend fun finished(player: Player, playback: BookPlayback, itemId: String, episodeId: String?): Result<Unit> {
        player.pause()
        return mark(itemId, episodeId, FinishedRequest(isFinished = true, currentTime = playback.bookPosition()))
    }

    /** The server also puts its saved position back to the start; the next report sets it again. */
    suspend fun unfinished(itemId: String, episodeId: String?): Result<Unit> =
        mark(itemId, episodeId, FinishedRequest(isFinished = false))

    private suspend fun mark(itemId: String, episodeId: String?, body: FinishedRequest): Result<Unit> = runCatching {
        if (episodeId != null) api.markFinished(itemId, episodeId, body) else api.markFinished(itemId, body)
    }
}

/**
 * Whether the book is finished, as About shows it; null for an episode.
 *
 * Playing a finished book moves its position, which the server takes as
 * un-finishing it, so starting to play does the same here.
 */
@Composable
fun rememberFinished(player: Player, initial: Boolean?): MutableState<Boolean?> {
    val finished = remember(player, initial) { mutableStateOf(initial) }
    DisposableEffect(player, finished) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && finished.value == true) finished.value = false
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return finished
}
