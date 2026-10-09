package com.paulohenriquesg.fahrenheit.api

import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path

/**
 * A show's own settings on the server (#182). Its own interface, so the fakes
 * of [PodcastApi] need not grow.
 */
interface PodcastSettingsApi {
    /**
     * Changes the item's media. The server touches only the keys sent; it
     * answers 403 to a user without update rights, 400 to a bad cron.
     */
    @PATCH("api/items/{itemId}/media")
    suspend fun updateMedia(@Path("itemId") itemId: String, @Body update: MediaUpdate)
}

/** A podcast's auto-download fields. Gson leaves nulls out, so only what is set is sent. */
data class MediaUpdate(
    val autoDownloadEpisodes: Boolean? = null,
    val autoDownloadSchedule: String? = null,
    val maxEpisodesToKeep: Int? = null,
    val maxNewEpisodesToDownload: Int? = null
)
