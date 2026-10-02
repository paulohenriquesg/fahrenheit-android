package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.CheckNewResponse
import com.paulohenriquesg.fahrenheit.api.FeedRequest
import com.paulohenriquesg.fahrenheit.api.FeedResponse
import com.paulohenriquesg.fahrenheit.api.FeedEpisode
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Plain JUnit. The server owns the feed, the parser and the download queue
 * (#76); all the app decides is whether to offer the check and what to say
 * about the answer.
 */
class FeedCheckTest {

    private class FakePodcastApi(
        private val me: () -> Me = { Me(type = "admin") },
        private val checkNew: () -> CheckNewResponse = { CheckNewResponse(emptyList()) }
    ) : PodcastApi {
        var checkedId: String? = null
        var checkedLimit: Int? = null

        override suspend fun me(): Me = me.invoke()

        override suspend fun checkNew(podcastId: String, limit: Int): CheckNewResponse {
            checkedId = podcastId
            checkedLimit = limit
            return checkNew.invoke()
        }

        override suspend fun feed(request: FeedRequest) = FeedResponse(null)
        override suspend fun downloadQueue(libraryId: String) = com.paulohenriquesg.fahrenheit.api.DownloadQueue(null, emptyList())
        override suspend fun downloadEpisodes(podcastId: String, episodes: List<JsonObject>) = Unit
    }

    private val feed = "https://feeds.example/show.xml"

    @Test
    fun `an admin is offered the check`() {
        assertTrue(FeedCheck.mayCheck("admin", feed))
    }

    @Test
    fun `root is offered the check`() {
        assertTrue(FeedCheck.mayCheck("root", feed))
    }

    @Test
    fun `an ordinary account is not, because the server would answer 403`() {
        listOf("user", "guest").forEach { type ->
            assertFalse(type, FeedCheck.mayCheck(type, feed))
        }
    }

    @Test
    fun `a podcast with no feed is not, because the server would answer 400`() {
        assertFalse(FeedCheck.mayCheck("root", null))
        assertFalse(FeedCheck.mayCheck("root", ""))
    }

    @Test
    fun `not knowing who we are means no button rather than one that fails`() {
        assertFalse(FeedCheck.mayCheck(null, feed))
    }

    @Test
    fun `a check reports how many episodes the server found`() = runBlocking {
        val api = FakePodcastApi(checkNew = {
            CheckNewResponse(listOf(FeedEpisode("one"), FeedEpisode("two"), FeedEpisode("three")))
        })

        assertEquals(3, FeedCheck(api).check("podcast-1").getOrThrow())
        assertEquals("podcast-1", api.checkedId)
    }

    @Test
    fun `a check that finds nothing says zero, not failure`() = runBlocking {
        val result = FeedCheck(FakePodcastApi()).check("podcast-1")

        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun `the limit is always sent, because zero on the server means no limit`() = runBlocking {
        val api = FakePodcastApi()

        FeedCheck(api).check("podcast-1")

        assertEquals(FeedCheck.LIMIT, api.checkedLimit)
        assertTrue(FeedCheck.LIMIT > 0)
    }

    @Test
    fun `a failed check is a failure, not zero`() = runBlocking {
        val result = FeedCheck(FakePodcastApi(checkNew = { throw IOException("timeout") })).check("p")

        assertTrue(result.isFailure)
    }

    @Test
    fun `the server's answers parse`() {
        // Trimmed from 2.36.0: GET /api/me and GET /api/podcasts/:id/checknew.
        val me = Gson().fromJson(
            """{"id":"u1","username":"paulo","type":"root","isActive":true}""",
            Me::class.java
        )
        val found = Gson().fromJson(
            """{"episodes":[{"title":"Episode 1","publishedAt":1759363200000,"enclosure":{"url":"https://x/1.mp3"}}]}""",
            CheckNewResponse::class.java
        )
        val none = Gson().fromJson("""{"episodes":[]}""", CheckNewResponse::class.java)

        assertEquals("root", me.type)
        assertEquals(1, found.episodes?.size)
        assertEquals(0, none.episodes?.size)
    }
}

/** Downloads land one by one after the check returns, so the screen polls for them. */
class AwaitEpisodesTest {

    @Test
    fun `polling stops once the new episodes have arrived`() = runBlocking {
        val counts = ArrayDeque(listOf(0, 1, 3, 3, 3))
        var reloads = 0

        val arrived = awaitEpisodes(target = 3, attempts = 10, pause = {}) {
            reloads++
            counts.removeFirst()
        }

        assertTrue(arrived)
        assertEquals(3, reloads)
    }

    @Test
    fun `polling gives up rather than running for as long as the screen is open`() = runBlocking {
        var reloads = 0

        val arrived = awaitEpisodes(target = 3, attempts = 4, pause = {}) { reloads++; 0 }

        assertFalse(arrived)
        assertEquals(4, reloads)
    }

    @Test
    fun `a failed reload is waited out, not taken as an answer`() = runBlocking {
        val counts = ArrayDeque(listOf(null, null, 3))

        val arrived = awaitEpisodes(target = 3, attempts = 5, pause = {}) { counts.removeFirst() }

        assertTrue(arrived)
    }

    @Test
    fun `it pauses between reloads, not before the first`() = runBlocking {
        val events = mutableListOf<String>()

        awaitEpisodes(target = 1, attempts = 3, pause = { events += "pause" }) {
            events += "reload"
            if (events.count { it == "reload" } == 2) 1 else 0
        }

        assertEquals(listOf("reload", "pause", "reload"), events)
    }
}

/** What the screen sees when the button is pressed. */
class FeedCheckRunTest {

    private class Api(private val found: Int?) : PodcastApi {
        override suspend fun me() = Me("admin")
        override suspend fun checkNew(podcastId: String, limit: Int) =
            if (found == null) throw IOException("down")
            else CheckNewResponse(List(found) { FeedEpisode("e$it") })
        override suspend fun feed(request: FeedRequest) = FeedResponse(null)
        override suspend fun downloadQueue(libraryId: String) = com.paulohenriquesg.fahrenheit.api.DownloadQueue(null, emptyList())
        override suspend fun downloadEpisodes(podcastId: String, episodes: List<JsonObject>) = Unit
    }

    private fun run(found: Int?, before: Int = 2, counts: List<Int?> = emptyList()): Pair<List<FeedCheckState>, Int> {
        val states = mutableListOf<FeedCheckState>()
        val queue = ArrayDeque(counts)
        var reloads = 0
        runBlocking {
            FeedCheck(Api(found)).run(
                podcastId = "p",
                episodesBefore = before,
                onState = { states += it },
                pause = {},
                reload = { reloads++; queue.removeFirstOrNull() }
            )
        }
        return states to reloads
    }

    @Test
    fun `a check says it is checking, then what it found`() {
        val (states, _) = run(found = 3, counts = listOf(5))

        assertEquals(listOf(FeedCheckState.Checking, FeedCheckState.Found(3)), states)
    }

    @Test
    fun `found episodes are waited for until they are on the server`() {
        val (_, reloads) = run(found = 3, before = 2, counts = listOf(2, 3, 5))

        assertEquals(3, reloads)
    }

    @Test
    fun `nothing found means nothing to wait for`() {
        val (states, reloads) = run(found = 0)

        assertEquals(listOf(FeedCheckState.Checking, FeedCheckState.Found(0)), states)
        assertEquals(0, reloads)
    }

    @Test
    fun `a failed check says so and waits for nothing`() {
        val (states, reloads) = run(found = null)

        assertEquals(listOf(FeedCheckState.Checking, FeedCheckState.Failed), states)
        assertEquals(0, reloads)
    }
}
