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

        assertEquals(DownloadState.Downloading, DownloadProgress.state(row("a"), queue, misses = 0))
    }

    @Test
    fun `a waiting episode says how many are ahead of it`() {
        val queue = DownloadQueue(currentDownload = queued("x"), queue = listOf(queued("y"), queued("a")))

        assertEquals(DownloadState.Waiting(ahead = 2), DownloadProgress.state(row("a"), queue, misses = 0))
    }

    @Test
    fun `next in line, with nothing downloading, is zero ahead`() {
        val queue = DownloadQueue(currentDownload = null, queue = listOf(queued("a")))

        assertEquals(DownloadState.Waiting(ahead = 0), DownloadProgress.state(row("a"), queue, misses = 0))
    }

    @Test
    fun `other podcasts' downloads count as ahead, because the server has one queue`() {
        val queue = DownloadQueue(currentDownload = queued("other", podcast = "q"), queue = listOf(queued("a")))

        assertEquals(DownloadState.Waiting(ahead = 1), DownloadProgress.state(row("a"), queue, misses = 0))
    }

    @Test
    fun `a queued download is matched by URL when it has no guid`() {
        val queue = DownloadQueue(currentDownload = queued(guid = null, url = "https://cdn/a.mp3"), queue = emptyList())

        assertEquals(DownloadState.Downloading, DownloadProgress.state(row("a"), queue, misses = 0))
    }

    @Test
    fun `an episode on the server needs no state`() {
        assertNull(DownloadProgress.state(row("a", onServer = true), DownloadQueue(null, emptyList()), misses = 5))
    }

    @Test
    fun `an episode nobody asked for, and nowhere in the queue, has no state`() {
        assertNull(DownloadProgress.state(row("a"), DownloadQueue(null, emptyList()), misses = null))
    }

    @Test
    fun `a requested episode missing from the queue once is not failed yet, since the server may not have queued it`() {
        assertEquals(DownloadState.Requested, DownloadProgress.state(row("a"), DownloadQueue(null, emptyList()), misses = 1))
    }

    @Test
    fun `a requested episode that left the queue without arriving has failed`() {
        assertEquals(DownloadState.Failed, DownloadProgress.state(row("a"), DownloadQueue(null, emptyList()), misses = 2))
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
