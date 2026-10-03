package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import retrofit2.awaitResponse

/**
 * What the continue-from-where check reads from the server, for the player
 * screen and the playback service alike (#90, #144). Quietly: a check is a
 * question, not a load, so a failed read just means no question.
 */
object ResumeSources {

    /** The server's progress for the item; null when it cannot be read or was never started. */
    suspend fun progress(itemId: String, episodeId: String?): MediaProgressResponse? {
        val api = ApiClient.getApiService() ?: return null
        val saved = SavedProgress.read(episodeId) {
            val call = if (episodeId != null) api.userGetMediaProgress(itemId, episodeId) else api.userGetMediaProgress(itemId)
            call.awaitResponse()
        }
        return (saved as? SavedProgress.Found)?.progress
    }

    /** The item's latest listening session; null when it cannot be read. */
    suspend fun latestSession(itemId: String, episodeId: String?): LatestSession? {
        val api = ApiClient.getApiService() ?: return null
        val call = if (episodeId != null) api.itemListeningSessions(itemId, episodeId) else api.itemListeningSessions(itemId)
        return runCatching { call.awaitResponse().body()?.latest() }.getOrNull()
    }
}
