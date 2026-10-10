package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.QueuedDownload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where a feed episode is on its way to the server (#76 step 3).
 *
 * The server reports no percentage: GET /libraries/:id/episode-downloads says
 * which episode is downloading now and which are waiting, in order. That is
 * what a row can honestly say.
 */
class DownloadProgressTest {

    private val gson = Gson()

    private fun row(guid: String, onServer: Boolean = false): EpisodeRow {
        val feed = gson.fromJson(
            """{"title":"$guid","guid":"$guid","publishedAt":1,"enclosure":{"url":"https://cdn/$guid.mp3"}}""",
            JsonObject::class.java
        )
        val server = if (onServer) gson.fromJson(
            """{"libraryItemId":"p","id":"s-$guid","index":1,"title":"$guid","publishedAt":1,"addedAt":0,"updatedAt":0,"guid":"$guid"}""",
            com.paulohenriquesg.fahrenheit.api.Episode::class.java
        ) else null
        return EpisodeList.merge(listOfNotNull(server), listOf(feed)).single()
    }

    private fun queued(guid: String?, url: String? = null, podcast: String = "p") =
        QueuedDownload(libraryItemId = podcast, guid = guid, url = url ?: "https://cdn/$guid.mp3")

    @Test
    fun `the episode the server is fetching now is downloading`() {
        val queue = DownloadQueue(currentDownload = queued("a"), queue = emptyList())

        assertEquals(DownloadState.Downloading, DownloadProgress.state(row("a"), queue, request = DownloadRequest.Asked(at = 0), now = 0))
    }

    @Test
    fun `a waiting episode says how many are ahead of it`() {
        val queue = DownloadQueue(currentDownload = queued("x"), queue = listOf(queued("y"), queued("a")))

        assertEquals(DownloadState.Waiting(ahead = 2), DownloadProgress.state(row("a"), queue, request = DownloadRequest.Asked(at = 0), now = 0))
    }

    @Test
    fun `next in line, with nothing downloading, is zero ahead`() {
        val queue = DownloadQueue(currentDownload = null, queue = listOf(queued("a")))

        assertEquals(DownloadState.Waiting(ahead = 0), DownloadProgress.state(row("a"), queue, request = DownloadRequest.Asked(at = 0), now = 0))
    }

    @Test
    fun `other podcasts' downloads count as ahead, because the server has one queue`() {
        val queue = DownloadQueue(currentDownload = queued("other", podcast = "q"), queue = listOf(queued("a")))

        assertEquals(DownloadState.Waiting(ahead = 1), DownloadProgress.state(row("a"), queue, request = DownloadRequest.Asked(at = 0), now = 0))
    }

    @Test
    fun `a queued download is matched by URL when it has no guid`() {
        val queue = DownloadQueue(currentDownload = queued(guid = null, url = "https://cdn/a.mp3"), queue = emptyList())

        assertEquals(DownloadState.Downloading, DownloadProgress.state(row("a"), queue, request = DownloadRequest.Asked(at = 0), now = 0))
    }

    private val empty = DownloadQueue(null, emptyList())
    private val window = DownloadProgress.FAIL_AFTER_MS

    // #215: an episode that lands is downloaded, whatever was said of it before - Failed included.
    @Test
    fun `an episode on the server needs no state, even one said to have failed`() {
        assertNull(DownloadProgress.state(row("a", onServer = true), empty, DownloadRequest.Asked(at = 0), now = window * 3))
        assertNull(DownloadProgress.state(row("a", onServer = true), empty, DownloadRequest.Refused, now = 0))
    }

    @Test
    fun `an episode nobody asked for, and nowhere in the queue, has no state`() {
        assertNull(DownloadProgress.state(row("a"), empty, request = null, now = 0))
    }

    // #215: on the device two empty polls 5 s apart said "failed", and the
    // server had the episode 10 s after the request.
    @Test
    fun `a requested episode missing from the queue is only requested, for a good while`() {
        listOf(5_000L, 10_000L, window - 1).forEach { now ->
            assertEquals("at $now", DownloadState.Requested, DownloadProgress.state(row("a"), empty, DownloadRequest.Asked(at = 0), now))
        }
    }

    @Test
    fun `a requested episode nowhere to be seen once the window has passed has failed`() {
        assertEquals(DownloadState.Failed, DownloadProgress.state(row("a"), empty, DownloadRequest.Asked(at = 1_000), now = 1_000 + window))
    }

    @Test
    fun `a request the server refused has failed at once`() {
        assertEquals(DownloadState.Failed, DownloadProgress.state(row("a"), empty, DownloadRequest.Refused, now = 0))
    }

    @Test
    fun `the queue response parses`() {
        // Trimmed from GET /api/libraries/:id/episode-downloads on 2.36.0.
        val queue = gson.fromJson(
            """{"currentDownload":{"id":"d1","episodeDisplayTitle":"Tabstack","url":"https://cdn/a.mp3","libraryItemId":"p",
                "libraryId":"l","isFinished":false,"failed":false,"guid":"a"},"queue":[]}""",
            DownloadQueue::class.java
        )

        assertEquals("a", queue.currentDownload?.guid)
        assertEquals(0, queue.queue?.size)
    }
}
