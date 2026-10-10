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

    // A clock that moves on by a poll's interval at each pause.
    private var clock = 0L
    private val pause: suspend () -> Unit = { clock += 5_000 }

    @Test
    fun `a requested episode that lands is no longer awaited`() = runBlocking {
        val api = Api(listOf({ q() }, { q() }))
        val requests = mutableListOf<Map<String, DownloadRequest>>()
        val holdings = ArrayDeque(listOf(emptyList(), listOf(landedA)))
        val watch = DownloadWatch(api, "lib", "p", now = { clock })

        assertTrue(watch.request("feed:a", feedA))
        watch.watch(onUpdate = { _, r -> requests += r }, pause = pause) { holdings.removeFirstOrNull() ?: listOf(landedA) }

        assertEquals(mapOf("feed:a" to DownloadRequest.Asked(at = 0)), requests.first())
        assertEquals(emptyMap<String, DownloadRequest>(), requests.last())
    }

    // #215: the device's case - the queue showed nothing, twice, and the
    // episode was on the server some 10 s after it was asked for.
    @Test
    fun `empty queues while the item has not caught up keep it awaited, and the item is read each time`() = runBlocking {
        val api = Api(emptyList())
        val requests = mutableListOf<Map<String, DownloadRequest>>()
        var reloads = 0
        val watch = DownloadWatch(api, "lib", "p", now = { clock })

        watch.request("feed:a", feedA)
        watch.watch(onUpdate = { _, r -> requests += r }, pause = pause) {
            reloads++
            if (clock >= 10_000) listOf(landedA) else emptyList()
        }

        assertEquals(3, api.queueCalls)
        assertEquals(3, reloads)
        assertEquals(mapOf("feed:a" to DownloadRequest.Asked(at = 0)), requests[1])
        assertEquals(emptyMap<String, DownloadRequest>(), requests.last())
    }

    @Test
    fun `past the window it is still looked for, so a late arrival is seen`() = runBlocking {
        val api = Api(emptyList())
        val requests = mutableListOf<Map<String, DownloadRequest>>()
        val watch = DownloadWatch(api, "lib", "p", now = { clock })
        val late = DownloadProgress.FAIL_AFTER_MS + 30_000

        watch.request("feed:a", feedA)
        watch.watch(onUpdate = { _, r -> requests += r }, pause = pause) { if (clock >= late) listOf(landedA) else emptyList() }

        assertTrue("watched past the window", api.queueCalls > DownloadProgress.FAIL_AFTER_MS / 5_000)
        assertEquals(emptyMap<String, DownloadRequest>(), requests.last())
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

    // Review: kept until it lands, one that never does was polled for as long
    // as the page lived. After an hour it is given up on - still reported, so
    // its row keeps saying it failed - and the watch ends.
    @Test
    fun `one nowhere to be seen for an hour is given up on, and the watch ends`() = runBlocking {
        val api = Api(emptyList())
        val requests = mutableListOf<Map<String, DownloadRequest>>()
        val watch = DownloadWatch(api, "lib", "p", now = { clock })

        watch.request("feed:a", feedA)
        // As the page runs it: again for as long as anything is awaited.
        do {
            watch.watch(onUpdate = { _, r -> requests += r }, pause = pause) { emptyList() }
        } while (watch.waiting)

        assertFalse(watch.waiting)
        assertTrue("stopped near the hour, at $clock", clock in DownloadWatch.GIVE_UP_AFTER_MS..DownloadWatch.GIVE_UP_AFTER_MS + 5_000)
        assertEquals(mapOf("feed:a" to DownloadRequest.Asked(at = 0)), requests.last())
    }
}
