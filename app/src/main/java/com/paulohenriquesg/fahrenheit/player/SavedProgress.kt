package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import retrofit2.Response

/**
 * What the server says about where the listener left off.
 *
 * "Never started" and "could not tell" were once the same null, so a failed
 * read quietly started from 0:00. Starting over is still what happens, but
 * the player now says so: playing on saves the new position over the old one.
 */
sealed interface SavedProgress {
    data class Found(val progress: MediaProgressResponse) : SavedProgress
    data object NeverStarted : SavedProgress
    data object Unreadable : SavedProgress

    companion object {

        /**
         * @param episodeId the episode asked about, or null for a book.
         * @param fetch the request; throwing means the server was not reached.
         */
        suspend fun read(episodeId: String?, fetch: suspend () -> Response<MediaProgressResponse>): SavedProgress {
            val response = runCatching { fetch() }.getOrElse { return Unreadable }
            if (response.code() == 404) return NeverStarted
            val body = response.body().takeIf { response.isSuccessful } ?: return Unreadable
            // The server answers for the item when it has nothing for the episode.
            if (episodeId != null && body.episodeId != episodeId) return NeverStarted
            return Found(body)
        }
    }
}

/** The position to start from: none means the beginning, and [onUnreadable] says why when it is not by choice. */
fun SavedProgress.progressOr(onUnreadable: () -> Unit): MediaProgressResponse? = when (this) {
    is SavedProgress.Found -> progress
    SavedProgress.NeverStarted -> null
    SavedProgress.Unreadable -> null.also { onUnreadable() }
}
