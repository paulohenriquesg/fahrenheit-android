package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.QueuedDownload

/** An episode asked of the server from this screen. */
sealed interface DownloadRequest {
    /** Asked at [at], in ms; failed only once [DownloadProgress.FAIL_AFTER_MS] has passed with nothing to show for it. */
    data class Asked(val at: Long) : DownloadRequest

    /** The server refused it. */
    data object Refused : DownloadRequest
}

/**
 * Where a feed episode is on its way to the server (#76 step 3).
 *
 * The server reports no percentage: its queue says which episode is downloading
 * now and which wait, in order, across every podcast. That is what a row says.
 */
object DownloadProgress {

    /**
     * How long a requested episode may be nowhere to be seen - not queued, not
     * downloading, not on the server - before it counts as failed (#215). On
     * the device the server's queue read empty twice, 5 s apart, while it
     * fetched an episode it had 10 s after the request: empty polls are not
     * evidence of a failure, only time is.
     */
    const val FAIL_AFTER_MS = 120_000L

    /**
     * An episode on the server needs no state, whatever was said of it
     * before - Failed included: it is the item gaining it that proves it.
     *
     * @param request what this screen asked of the server; null when nothing.
     * @param now this device's clock, against [DownloadRequest.Asked.at].
     */
    fun state(row: EpisodeRow, queue: DownloadQueue, request: DownloadRequest?, now: Long): DownloadState? {
        if (row.downloaded) return null
        val feed = row.feed ?: return null
        if (queue.currentDownload?.let { matches(it, feed) } == true) return DownloadState.Downloading
        val waiting = queue.queue.orEmpty()
        val index = waiting.indexOfFirst { matches(it, feed) }
        if (index >= 0) {
            val downloading = if (queue.currentDownload != null) 1 else 0
            return DownloadState.Waiting(ahead = index + downloading)
        }
        return when (request) {
            null -> null
            DownloadRequest.Refused -> DownloadState.Failed
            is DownloadRequest.Asked -> if (now - request.at >= FAIL_AFTER_MS) DownloadState.Failed else DownloadState.Requested
        }
    }

    /** Whether [feed]'s episode is downloading or waiting. */
    fun queued(queue: DownloadQueue, feed: JsonObject): Boolean =
        (listOfNotNull(queue.currentDownload) + queue.queue.orEmpty()).any { matches(it, feed) }

    /** Whether the queue holds anything for this podcast, i.e. worth watching. */
    fun busy(queue: DownloadQueue, podcastId: String): Boolean =
        (listOfNotNull(queue.currentDownload) + queue.queue.orEmpty()).any { it.libraryItemId == podcastId }

    // The server's own rule, as for matching the feed to its episodes.
    private fun matches(download: QueuedDownload, feed: JsonObject): Boolean {
        val guid = feed.get("guid")?.takeUnless { it.isJsonNull }?.asString
        if (download.guid != null && download.guid == guid) return true
        val url = feed.getAsJsonObject("enclosure")?.get("url")?.takeUnless { it.isJsonNull }?.asString
        return download.url != null && download.url == url
    }
}
