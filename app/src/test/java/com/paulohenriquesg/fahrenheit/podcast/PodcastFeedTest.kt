package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.CheckNewResponse
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.FeedRequest
import com.paulohenriquesg.fahrenheit.api.FeedResponse
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PodcastFeedTest {

    private val gson = Gson()

    private class Api(
        private val feed: () -> FeedResponse = { FeedResponse(null) },
        private val download: () -> Unit = {}
    ) : PodcastApi {
        var feedAsked: FeedRequest? = null
        var downloaded: Pair<String, List<JsonObject>>? = null
        override suspend fun me() = Me("admin")
        override suspend fun checkNew(podcastId: String, limit: Int) = CheckNewResponse(emptyList())
        override suspend fun feed(request: FeedRequest): FeedResponse { feedAsked = request; return feed() }
        override suspend fun downloadEpisodes(podcastId: String, episodes: List<JsonObject>) {
            downloaded = podcastId to episodes
            download()
        }
    }

    private val episodeJson = gson.fromJson(
        """{"title":"Supergood","guid":"g3","publishedAt":1,"enclosure":{"url":"https://cdn/s.mp3"}}""",
        JsonObject::class.java
    )

    private fun serverCopy(): Episode = gson.fromJson(
        """{"libraryItemId":"li","id":"s3","index":1,"title":"Supergood","publishedAt":1,"addedAt":0,"updatedAt":0,"guid":"g3"}""",
        Episode::class.java
    )

    @Test
    fun `the server's feed response parses`() {
        // Trimmed from POST /api/podcasts/feed on 2.36.0.
        val response = gson.fromJson(
            """{"podcast":{"metadata":{"title":"APIs You Won't Hate"},"episodes":[
                {"title":"Tabstack","pubDate":"Thu, 01 Oct 2026 11:23:35 +0000","duration":"1558","durationSeconds":1558,
                 "publishedAt":1790853815000,"enclosure":{"url":"https://media.transistor.fm/060dc239/b43ece8f.mp3","length":"24942594","type":"audio/mpeg"},
                 "guid":"16ba8431-55db-416b-a8a0-39a9db4ee4b6","chaptersUrl":null,"chapters":[]}]}}""",
            FeedResponse::class.java
        )

        assertEquals(1, response.podcast?.episodes?.size)
    }

    @Test
    fun `reading the feed asks the server about the podcast's own feed URL`() = runBlocking {
        val api = Api(feed = { gson.fromJson("""{"podcast":{"episodes":[{"title":"a"},{"title":"b"}]}}""", FeedResponse::class.java) })

        val episodes = PodcastFeed(api).episodes("https://feeds.example/show").getOrThrow()

        assertEquals("https://feeds.example/show", api.feedAsked?.rssFeed)
        assertEquals(2, episodes.size)
    }

    @Test
    fun `a feed that cannot be read is a failure, not an empty feed`() = runBlocking {
        val result = PodcastFeed(Api(feed = { throw IOException("404") })).episodes("https://x")

        assertTrue(result.isFailure)
    }

    @Test
    fun `downloading sends the feed's own episode back to that podcast`() = runBlocking {
        val api = Api()

        PodcastFeed(api).download("podcast-1", episodeJson, onState = {}, pause = {}, reload = { listOf(serverCopy()) })

        assertEquals("podcast-1", api.downloaded?.first)
        assertEquals(listOf(episodeJson), api.downloaded?.second)
    }

    @Test
    fun `a download says so, and is done once the server holds the episode`() = runBlocking {
        val states = mutableListOf<DownloadState>()
        val holdings = ArrayDeque(listOf(emptyList(), emptyList(), listOf(serverCopy())))
        var reloads = 0

        PodcastFeed(Api()).download("p", episodeJson, onState = { states += it }, pause = {}) {
            reloads++
            holdings.removeFirst()
        }

        assertEquals(listOf(DownloadState.Downloading, DownloadState.Done), states)
        assertEquals(3, reloads)
    }

    @Test
    fun `a refused download says so and waits for nothing`() = runBlocking {
        val states = mutableListOf<DownloadState>()
        var reloads = 0

        PodcastFeed(Api(download = { throw IOException("403") }))
            .download("p", episodeJson, onState = { states += it }, pause = {}) { reloads++; emptyList() }

        assertEquals(listOf(DownloadState.Downloading, DownloadState.Failed), states)
        assertEquals(0, reloads)
    }

    @Test
    fun `a download that is still not there after a while stays queued, not failed`() = runBlocking {
        val states = mutableListOf<DownloadState>()

        PodcastFeed(Api()).download("p", episodeJson, onState = { states += it }, pause = {}) { emptyList() }

        assertEquals(DownloadState.Queued, states.last())
    }
}
