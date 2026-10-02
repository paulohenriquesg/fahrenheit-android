package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.FeedRequest
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import kotlinx.coroutines.delay

enum class DownloadState {
    /** Asked for, and being watched for. */
    Downloading,

    /** Accepted, but not on the server yet after a while of looking. */
    Queued,
    Done,
    Failed
}

/**
 * A podcast's whole feed, read by the server, and single episodes downloaded
 * from it (#76). Admin or root only; the caller has already checked, via
 * [FeedCheck.mayCheck].
 */
class PodcastFeed(private val api: PodcastApi) {

    suspend fun episodes(feedUrl: String): Result<List<JsonObject>> = runCatching {
        api.feed(FeedRequest(feedUrl)).podcast?.episodes.orEmpty()
    }

    /**
     * Asks the server to download one feed episode, then reloads the podcast
     * until the server holds it. The request returns as soon as the download is
     * queued, so without the wait the row would never turn playable.
     *
     * @param reload fetches the podcast's episodes again, or null on failure.
     */
    suspend fun download(
        podcastId: String,
        episode: JsonObject,
        onState: (DownloadState) -> Unit,
        pause: suspend () -> Unit = { delay(POLL_INTERVAL_MS) },
        reload: suspend () -> List<Episode>?
    ) {
        onState(DownloadState.Downloading)
        val requested = runCatching { api.downloadEpisodes(podcastId, listOf(episode)) }
        if (requested.isFailure) {
            onState(DownloadState.Failed)
            return
        }
        repeat(POLL_ATTEMPTS) { attempt ->
            if (attempt > 0) pause()
            if (reload()?.any { EpisodeList.matches(it, episode) } == true) {
                onState(DownloadState.Done)
                return
            }
        }
        onState(DownloadState.Queued)
    }

    private companion object {
        /** Five minutes of looking: one episode is often a 50 MB file. */
        const val POLL_INTERVAL_MS = 5_000L
        const val POLL_ATTEMPTS = 60
    }
}
