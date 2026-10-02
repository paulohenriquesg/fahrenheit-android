package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.QueuedDownload

/**
 * Where a feed episode is on its way to the server (#76 step 3).
 *
 * The server reports no percentage: its queue says which episode is downloading
 * now and which wait, in order, across every podcast. That is what a row says.
 */
object DownloadProgress {

    /** Times in a row a requested episode may be missing before it counts as failed. */
    const val MISSES_BEFORE_FAILED = 2

    /**
     * @param misses how many polls in a row a requested episode has been
     *   missing from both the queue and the server; null when nobody here
     *   asked for it. The first miss is forgiven: the server may not have
     *   queued it yet when the first poll lands.
     */
    fun state(row: EpisodeRow, queue: DownloadQueue, misses: Int?): DownloadState? {
        if (row.downloaded) return null
        val feed = row.feed ?: return null
        if (queue.currentDownload?.let { matches(it, feed) } == true) return DownloadState.Downloading
        val waiting = queue.queue.orEmpty()
        val index = waiting.indexOfFirst { matches(it, feed) }
        if (index >= 0) {
            val downloading = if (queue.currentDownload != null) 1 else 0
            return DownloadState.Waiting(ahead = index + downloading)
        }
        return when {
            misses == null -> null
            misses >= MISSES_BEFORE_FAILED -> DownloadState.Failed
            else -> DownloadState.Requested
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
