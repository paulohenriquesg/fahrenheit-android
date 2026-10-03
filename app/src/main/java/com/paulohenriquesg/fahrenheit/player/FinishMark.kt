package com.paulohenriquesg.fahrenheit.player

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
