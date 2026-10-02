package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.CheckNewResponse
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.FeedRequest
import com.paulohenriquesg.fahrenheit.api.FeedResponse
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import com.paulohenriquesg.fahrenheit.api.QueuedDownload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Watching the server's queue until this podcast's downloads have landed. */
class DownloadWatchTest {

    private val gson = Gson()
    private val feedA: JsonObject = gson.fromJson(
        """{"title":"A","guid":"a","publishedAt":1,"enclosure":{"url":"https://cdn/a.mp3"}}""", JsonObject::class.java
    )
    private val landedA: Episode = gson.fromJson(
        """{"libraryItemId":"p","id":"s-a","index":1,"title":"A","publishedAt":1,"addedAt":0,"updatedAt":0,"guid":"a"}""",
        Episode::class.java
    )
    private fun q(vararg guids: String?) = DownloadQueue(
        currentDownload = guids.firstOrNull()?.let { QueuedDownload("p", it, "https://cdn/$it.mp3") },
        queue = guids.drop(1).map { QueuedDownload("p", it, "https://cdn/$it.mp3") }
    )

    private class Api(queues: List<() -> DownloadQueue>, private val refuse: Boolean = false) : PodcastApi {
        private val queues = ArrayDeque(queues)
        var queueCalls = 0
        var downloads = 0
        override suspend fun me() = Me("admin")
        override suspend fun checkNew(podcastId: String, limit: Int) = CheckNewResponse(emptyList())
        override suspend fun feed(request: FeedRequest) = FeedResponse(null)
        override suspend fun downloadQueue(libraryId: String): DownloadQueue {
            queueCalls++
            return (queues.removeFirstOrNull() ?: { DownloadQueue(null, emptyList()) }).invoke()
        }
        override suspend fun downloadEpisodes(podcastId: String, episodes: List<JsonObject>) {
            downloads++
            if (refuse) throw IOException("403")
        }
    }

    @Test
    fun `with nothing queued for this podcast, it looks once and stops`() = runBlocking {
        val api = Api(listOf({ DownloadQueue(QueuedDownload("other", "x", "u"), emptyList()) }))
        var reloads = 0

        DownloadWatch(api, "lib", "p").watch(onUpdate = { _, _ -> }, pause = {}) { reloads++; emptyList() }

        assertEquals(1, api.queueCalls)
        assertEquals(0, reloads)
    }

    @Test
    fun `it reports the queue while this podcast has downloads, and stops once they land`() = runBlocking {
        val api = Api(listOf({ q("a", "b") }, { q("b") }, { q() }))
        val seen = mutableListOf<DownloadQueue>()
        var reloads = 0

        DownloadWatch(api, "lib", "p").watch(onUpdate = { queue, _ -> seen += queue }, pause = {}) {
            reloads++
            emptyList()
        }

        assertEquals(3, seen.size)
        assertTrue("reloads as downloads leave the queue", reloads >= 2)
    }

    @Test
    fun `a requested episode that lands is no longer counted as missing`() = runBlocking {
        val api = Api(listOf({ q() }, { q() }))
        val misses = mutableListOf<Map<String, Int>>()
        val holdings = ArrayDeque(listOf(emptyList(), listOf(landedA)))
        val watch = DownloadWatch(api, "lib", "p")

        assertTrue(watch.request("feed:a", feedA))
        watch.watch(onUpdate = { _, m -> misses += m }, pause = {}) { holdings.removeFirstOrNull() ?: listOf(landedA) }

        assertEquals(mapOf("feed:a" to 1), misses.first())
        assertEquals(emptyMap<String, Int>(), misses.last())
    }

    @Test
    fun `a requested episode that never shows up is given up on, and watching stops`() = runBlocking {
        val api = Api(emptyList())
        val misses = mutableListOf<Map<String, Int>>()
        val watch = DownloadWatch(api, "lib", "p")

        watch.request("feed:a", feedA)
        watch.watch(onUpdate = { _, m -> misses += m }, pause = {}) { emptyList() }

        assertEquals(DownloadProgress.MISSES_BEFORE_FAILED, misses.last()["feed:a"])
        assertEquals(DownloadProgress.MISSES_BEFORE_FAILED, api.queueCalls)
    }

    @Test
    fun `a refused request is not watched`() = runBlocking {
        val watch = DownloadWatch(Api(emptyList(), refuse = true), "lib", "p")

        assertFalse(watch.request("feed:a", feedA))
    }

    @Test
    fun `a queue that cannot be read is tried again rather than ending the watch`() = runBlocking {
        val api = Api(listOf({ throw IOException("timeout") }, { q("a") }, { q() }))
        val seen = mutableListOf<DownloadQueue>()

        DownloadWatch(api, "lib", "p").watch(onUpdate = { queue, _ -> seen += queue }, pause = {}) { emptyList() }

        assertEquals(2, seen.size)
    }
}
