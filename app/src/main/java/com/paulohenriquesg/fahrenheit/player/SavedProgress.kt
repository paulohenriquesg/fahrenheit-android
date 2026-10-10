package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.CancellationException
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
         * A cancelled read is cancelled, not unreadable (#217): the player's
         * read restarts when playback connects or starts, and the cancelled one
         * said the position could not be read although nothing had failed.
         *
         * @param episodeId the episode asked about, or null for a book.
         * @param whyUnreadable why a read was unreadable: what was thrown, or the reply's code.
         * @param fetch the request; throwing means the server was not reached.
         */
        suspend fun read(
            episodeId: String?,
            whyUnreadable: (String) -> Unit = {},
            fetch: suspend () -> Response<MediaProgressResponse>
        ): SavedProgress {
            val response = try {
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                whyUnreadable(e::class.java.name)
                return Unreadable
            }
            if (response.code() == 404) return NeverStarted
            if (!response.isSuccessful) return Unreadable.also { whyUnreadable("HTTP ${response.code()}") }
            val body = response.body() ?: return Unreadable.also { whyUnreadable("HTTP ${response.code()} with no body") }
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
