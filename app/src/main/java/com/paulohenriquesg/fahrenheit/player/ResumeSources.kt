package com.paulohenriquesg.fahrenheit.player

import android.util.Log
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import retrofit2.awaitResponse

/**
 * What the continue-from-where check reads from the server, for the player
 * screen and the playback service alike (#90, #144). Quietly: a check is a
 * question, not a load, so a failed read just means no question.
 */
object ResumeSources {
    private const val TAG = "ResumeSources"

    /** The server's progress for the item; null when it cannot be read or was never started. */
    suspend fun progress(itemId: String, episodeId: String?): MediaProgressResponse? =
        (saved(itemId, episodeId) as? SavedProgress.Found)?.progress

    /** The same, telling "never started" from "could not tell"; signed out cannot tell. */
    suspend fun saved(itemId: String, episodeId: String?): SavedProgress {
        val api = ApiClient.getApiService() ?: return SavedProgress.Unreadable
        return SavedProgress.read(episodeId, whyUnreadable = { Log.d(TAG, "Progress unreadable: $it") }) {
            val call = if (episodeId != null) api.userGetMediaProgress(itemId, episodeId) else api.userGetMediaProgress(itemId)
            call.awaitResponse()
        }
    }

    /** The item's latest listening session; null when it cannot be read. */
    suspend fun latestSession(itemId: String, episodeId: String?): LatestSession? {
        val api = ApiClient.getApiService() ?: return null
        val call = if (episodeId != null) api.itemListeningSessions(itemId, episodeId) else api.itemListeningSessions(itemId)
        return runCatching { call.awaitResponse().body()?.latest() }.getOrNull()
    }
}
