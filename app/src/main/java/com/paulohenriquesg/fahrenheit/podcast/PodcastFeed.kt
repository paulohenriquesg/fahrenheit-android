package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.FeedRequest
import com.paulohenriquesg.fahrenheit.api.PodcastApi

sealed interface DownloadState {
    /** Asked for; not seen in the server's queue yet. */
    data object Requested : DownloadState

    /** In the server's queue, behind [ahead] others (from any podcast). */
    data class Waiting(val ahead: Int) : DownloadState

    /** The episode the server is fetching now. */
    data object Downloading : DownloadState

    /** Refused, or gone from the queue without arriving. */
    data object Failed : DownloadState
}

/**
 * A podcast's whole feed, read by the server (#76). Admin or root only; the
 * caller has already checked, via [FeedCheck.mayCheck]. Downloading from it is
 * [DownloadWatch]'s.
 */
class PodcastFeed(private val api: PodcastApi) {

    suspend fun episodes(feedUrl: String): Result<List<JsonObject>> = runCatching {
        api.feed(FeedRequest(feedUrl)).podcast?.episodes.orEmpty()
    }
}
