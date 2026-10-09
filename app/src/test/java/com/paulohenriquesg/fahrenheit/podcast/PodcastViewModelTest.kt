package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.CheckNewResponse
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.FeedEpisode
import com.paulohenriquesg.fahrenheit.api.FeedPodcast
import com.paulohenriquesg.fahrenheit.api.FeedRequest
import com.paulohenriquesg.fahrenheit.api.FeedResponse
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.MediaUpdate
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.MePermissions
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import com.paulohenriquesg.fahrenheit.api.PodcastSettingsApi
import com.paulohenriquesg.fahrenheit.api.QueuedDownload
import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.favourites.FakePlaylists
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.HeartChange
import com.paulohenriquesg.fahrenheit.favourites.LibraryFavourites
import com.paulohenriquesg.fahrenheit.favourites.MemoryChoice
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The podcast page's state (#205, #208), away from Compose. The fakes answer
 * at once and the scope is unconfined, so each event has settled by the time
 * it returns.
 */
class PodcastViewModelTest {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun stop() = scope.cancel()

    private fun item(autoDownload: Boolean = false, feedUrl: String? = "https://feeds.example/show.xml"): LibraryItemResponse = gson.fromJson(
        """{"id":"p1","libraryId":"lib","mediaType":"podcast","media":{
            "metadata":{"title":"The Show","genres":["Talk"],"description":"<p>About the show.</p>"
                ${feedUrl?.let { ""","feedUrl":"$it"""" } ?: ""}},
            "autoDownloadEpisodes":$autoDownload,"autoDownloadSchedule":"0 0 * * *","lastEpisodeCheck":0,
            "episodes":[
              {"id":"s1","title":"First Steps","publishedAt":1,"guid":"g1"},
              {"id":"s2","title":"Second Wind","publishedAt":2,"guid":"g2"}
            ]}}""",
        LibraryItemResponse::class.java
    )

    private fun feedEpisode(guid: String, title: String, at: Long): JsonObject = gson.fromJson(
        """{"title":"$title","guid":"$guid","publishedAt":$at,"enclosure":{"url":"https://cdn.example/$guid.mp3"}}""",
        JsonObject::class.java
    )

    private fun progress(episodeId: String, at: Double, finished: Boolean = false, lastUpdate: Long = 1) =
        MediaProgressResponse(libraryItemId = "p1", episodeId = episodeId, currentTime = at, duration = 1000.0, isFinished = finished, lastUpdate = lastUpdate)

    private class Api(
        var me: Me = Me(type = "user"),
        var meGate: CompletableDeferred<Unit>? = null,
        val feedEpisodes: List<JsonObject> = emptyList(),
        val found: Int? = 0,
        var queue: DownloadQueue = DownloadQueue(null, emptyList())
    ) : PodcastApi {
        var meReads = 0
        var feedReads = 0
        var queueReads = 0
        val requested = mutableListOf<List<JsonObject>>()
        override suspend fun me(): Me {
            meReads++
            meGate?.await()
            return me
        }
        override suspend fun checkNew(podcastId: String, limit: Int): CheckNewResponse =
            found?.let { CheckNewResponse(List(it) { FeedEpisode("new") }) } ?: error("refused")
        override suspend fun feed(request: FeedRequest): FeedResponse {
            feedReads++
            return FeedResponse(FeedPodcast(feedEpisodes))
        }
        override suspend fun downloadQueue(libraryId: String) = queue.also { queueReads++ }
        var refuse = false
        override suspend fun downloadEpisodes(podcastId: String, episodes: List<JsonObject>) {
            if (refuse) error("refused")
            requested += episodes
        }
    }

    private class Settings(val refuse: Boolean = false) : PodcastSettingsApi {
        val sent = mutableListOf<MediaUpdate>()
        override suspend fun updateMedia(itemId: String, update: MediaUpdate) {
            if (refuse) error("refused")
            sent += update
        }
    }

    private val store = ProgressStore()
    private var clock = 0L
    private var watchPause: (suspend () -> Unit)? = null
    private var marks = mutableListOf<Triple<String, Boolean, Double?>>()
    private var markAnswer: suspend () -> Boolean = { true }

    private fun model(
        api: Api = Api(),
        settings: PodcastSettingsApi? = Settings(),
        item: LibraryItemResponse = item(),
        reload: suspend () -> LibraryItemResponse? = { item },
        favourites: LibraryFavourites? = null
    ) = PodcastViewModel(
        itemId = "p1",
        item = item,
        podcastApi = api,
        settingsApi = settings,
        reloadItem = reload,
        markFinished = { id, finished, keepAt -> marks += Triple(id, finished, keepAt); markAnswer() },
        now = 10,
        serverFormat = null,
        favourites = favourites,
        progressStore = store,
        clock = { clock },
        watchPause = watchPause,
        scope = scope
    )

    @Test
    fun `the left column reads the show's title, kind and description`() {
        val state = model().state.value

        assertEquals("The Show", state.title)
        assertEquals("Podcast · Talk", state.byline)
        assertEquals("<p>About the show.</p>", state.description)
    }

    @Test
    fun `progress is read from the server on open, and Resume names the episode`() {
        val api = Api(me = Me(type = "user", mediaProgress = listOf(progress("s1", 400.0), progress("s2", 1000.0, finished = true))))
        val state = model(api).state.value

        assertEquals(EpisodeProgress.Heard, state.progress["s2"])
        assertTrue(state.progress["s1"] is EpisodeProgress.InProgress)
        assertEquals("Resume First Steps", state.primary)
        assertEquals("s1", state.primaryEpisode?.id)
    }

    @Test
    fun `with nothing started, the primary plays the newest episode`() {
        val state = model().state.value

        assertEquals("Play newest episode", state.primary)
        assertEquals("s2", state.primaryEpisode?.id)
    }

    @Test
    fun `coming back reads progress again`() {
        val api = Api()
        val model = model(api)
        api.me = Me(type = "user", mediaProgress = listOf(progress("s2", 1000.0, finished = true)))

        model.refresh()

        assertEquals(2, api.meReads)
        assertEquals(EpisodeProgress.Heard, model.state.value.progress["s2"])
    }

    @Test
    fun `an admin gets the whole feed, its tabs and the feed check`() {
        val api = Api(me = Me(type = "admin"), feedEpisodes = listOf(feedEpisode("g3", "Third Rail", 3), feedEpisode("g2", "Second Wind", 2)))
        val state = model(api).state.value

        assertEquals(1, api.feedReads)
        assertEquals(EpisodeTab.All to 3, state.screen.tabs?.first())
        assertEquals(FeedCheckState.Idle, state.feedCheck)
    }

    @Test
    fun `anyone else gets neither, and the feed is not asked for`() {
        val api = Api(me = Me(type = "user"))
        val state = model(api).state.value

        assertEquals(0, api.feedReads)
        assertNull(state.screen.tabs)
        assertNull(state.feedCheck)
    }

    @Test
    fun `a tab narrows the rows`() {
        val api = Api(me = Me(type = "admin"), feedEpisodes = listOf(feedEpisode("g3", "Third Rail", 3)))
        val model = model(api)

        model.chooseTab(EpisodeTab.NotDownloaded)

        assertEquals(EpisodeTab.NotDownloaded, model.state.value.tab)
        assertEquals(listOf("Third Rail"), model.state.value.screen.rows.map { it.title })
    }

    @Test
    fun `the feed check says what it is doing and what it found`() {
        val gate = CompletableDeferred<CheckNewResponse>()
        val api = object : PodcastApi by Api(me = Me(type = "admin")) {
            override suspend fun checkNew(podcastId: String, limit: Int) = gate.await()
        }
        val model = PodcastViewModel("p1", item(), api, null, { null }, { _, _, _ -> true }, 10, null, scope = scope)

        model.checkFeed()
        assertEquals(FeedCheckState.Checking, model.state.value.feedCheck)

        gate.complete(CheckNewResponse(emptyList()))
        assertEquals(FeedCheckState.Found(0), model.state.value.feedCheck)
    }

    // Review: the transitions that left DetailActivity, each pinned (#208).
    @Test
    fun `an admin's screen watches the download queue from the start`() {
        val api = Api(me = Me(type = "admin"))
        model(api)

        assertEquals(1, api.queueReads)
    }

    @Test
    fun `a check that finds episodes reads the show again, and its rows follow`() {
        val more: LibraryItemResponse = gson.fromJson(
            gson.toJson(item()).replace(""""episodes":[""", """"episodes":[{"id":"s3","title":"Third Rail","publishedAt":3,"guid":"g3"},"""),
            LibraryItemResponse::class.java
        )
        val api = Api(me = Me(type = "admin"), found = 1)
        val model = model(api, reload = { more })

        model.checkFeed()

        assertTrue(api.queueReads >= 2)
        assertEquals(FeedCheckState.Found(1), model.state.value.feedCheck)
        assertTrue(model.state.value.screen.rows.any { it.title == "Third Rail" })
    }

    @Test
    fun `coming back reads the Favourites again`() {
        playlists.add("pl_1", "Bedtime", "lib", PlaylistItem("p1", "s1"))
        val model = model(favourites = favourites())
        // A heart pressed in the player, say.
        runBlocking { favourites().apply { load() }.toggle("p1", "s2") }

        model.refresh()

        assertEquals(setOf("s1", "s2"), model.state.value.favourites)
    }

    @Test
    fun `a mark gives way to the server's word on the next read`() {
        val answer = CompletableDeferred<Boolean>()
        markAnswer = { answer.await() }
        val api = Api()
        val model = model(api)
        val episode = model.state.value.screen.rows.first { it.title == "First Steps" }.onServer!!
        model.mark(episode, true)
        assertEquals(EpisodeProgress.Heard, model.state.value.progress["s1"])

        // Played on elsewhere meanwhile: the server has it under way, and the store says so.
        store.replace(listOf(progress("s1", 100.0)), since = store.generation)
        answer.complete(true)

        assertTrue(model.state.value.progress["s1"] is EpisodeProgress.InProgress)
    }

    @Test
    fun `two reads at once ask for the feed once`() {
        val gate = CompletableDeferred<Unit>()
        val api = Api(me = Me(type = "admin"), meGate = gate)
        val model = model(api)
        model.refresh()

        gate.complete(Unit)

        assertEquals(1, api.feedReads)
    }

    // Review: before the server says who this is, the page does not claim
    // automatic downloads are off - the button may be about to say otherwise.
    @Test
    fun `until the user is known, the downloads fact waits`() {
        val api = Api(me = Me(type = "user", permissions = MePermissions(update = true)), meGate = CompletableDeferred())
        val state = model(api).state.value

        assertEquals(listOf("2 episodes on the server", "Feed never checked"), state.screen.facts.map { it.text })
    }

    @Test
    fun `a refused feed check says so`() {
        val model = model(Api(me = Me(type = "admin"), found = null))

        model.checkFeed()

        assertEquals(FeedCheckState.Failed, model.state.value.feedCheck)
    }

    @Test
    fun `asking for a missing episode sends it to the server and shows where it is`() {
        val third = feedEpisode("g3", "Third Rail", 3)
        val api = Api(me = Me(type = "admin"), feedEpisodes = listOf(third))
        val model = model(api)
        val row = model.state.value.screen.rows.first { it.title == "Third Rail" }
        api.queue = DownloadQueue(null, listOf(QueuedDownload("p1", "g3", null)))

        model.download(row)

        assertEquals(listOf(listOf(third)), api.requested)
        assertEquals(DownloadState.Waiting(ahead = 0), model.state.value.downloads[row.key])
    }

    @Test
    fun `a mark shows at once, and once taken the store says it, with no read of the page's own`() {
        val answer = CompletableDeferred<Boolean>()
        markAnswer = { answer.await() }
        val api = Api()
        val model = model(api)
        val episode = model.state.value.screen.rows.first { it.title == "First Steps" }.onServer!!

        model.mark(episode, true)
        assertEquals(EpisodeProgress.Heard, model.state.value.progress["s1"])

        // As the playback service does with a mark the server took (#207).
        store.marked("p1", "s1", ProgressMark(isFinished = true))
        answer.complete(true)
        assertEquals(listOf(Triple("s1", true, null)), marks)
        assertEquals(EpisodeProgress.Heard, model.state.value.progress["s1"])
        assertEquals(1, api.meReads)
    }

    // #207: the page reads the shared store, so the player's writes show on Back.
    @Test
    fun `an episode finished in the player shows finished, with no read of the page's own`() {
        val api = Api()
        val model = model(api)

        store.marked("p1", "s2", ProgressMark(isFinished = true))

        assertEquals(EpisodeProgress.Heard, model.state.value.progress["s2"])
        assertEquals(1, api.meReads)
    }

    @Test
    fun `un-marking keeps the place it was left at`() {
        val api = Api(me = Me(type = "user", mediaProgress = listOf(progress("s1", 400.0, finished = true))))
        val model = model(api)
        val episode = model.state.value.screen.rows.first { it.title == "First Steps" }.onServer!!

        model.mark(episode, false)

        assertEquals(listOf(Triple("s1", false, 400.0)), marks)
    }

    @Test
    fun `a refused mark goes back, and says so once`() {
        markAnswer = { false }
        val model = model()
        val episode = model.state.value.screen.rows.first { it.title == "First Steps" }.onServer!!

        model.mark(episode, true)

        assertNull(model.state.value.progress["s1"])
        assertTrue(model.state.value.markFailed)
        model.markFailureShown()
        assertFalse(model.state.value.markFailed)
    }

    @Test
    fun `whoever has the update right gets the show's download settings, and no fact repeats them`() {
        val model = model(Api(me = Me(type = "user", permissions = MePermissions(update = true))))
        val state = model.state.value

        assertEquals(DownloadSettings(false, "0 0 * * *", 0, 3), state.autoDownloads)
        assertEquals(listOf("2 episodes on the server", "Feed never checked"), state.screen.facts.map { it.text })
    }

    @Test
    fun `anyone else gets the state as a fact instead`() {
        val state = model(Api(me = Me(type = "user"))).state.value

        assertNull(state.autoDownloads)
        assertEquals("Automatic downloads off", state.screen.facts.last().text)
    }

    @Test
    fun `a change to the settings shows at once and is saved`() {
        val settings = Settings()
        val model = model(Api(me = Me(type = "user", permissions = MePermissions(update = true))), settings)

        model.changeDownloads(DownloadChange.Enabled(true))

        assertEquals(true, model.state.value.autoDownloads?.enabled)
        assertEquals(listOf(MediaUpdate(autoDownloadEpisodes = true)), settings.sent)
    }

    @Test
    fun `a refused change goes back and says so, until the panel closes`() {
        val model = model(Api(me = Me(type = "user", permissions = MePermissions(update = true))), Settings(refuse = true))

        model.changeDownloads(DownloadChange.Enabled(true))

        assertEquals(false, model.state.value.autoDownloads?.enabled)
        assertTrue(model.state.value.downloadsFailed)
        model.downloadsSeen()
        assertFalse(model.state.value.downloadsFailed)
    }

    // #211's hearts and Favourites tab, now the ViewModel's (#205).
    private val playlists = FakePlaylists()
    private fun favourites(chosen: Boolean = true) =
        LibraryFavourites(Favourites(playlists, if (chosen) MemoryChoice("lib" to "pl_1") else MemoryChoice()), "lib")

    @Test
    fun `the library's Favourites gives the hearts and a tab of their own`() {
        playlists.add("pl_1", "Bedtime", "lib", PlaylistItem("p1", "s1"))
        val model = model(favourites = favourites())

        assertEquals("Bedtime", model.state.value.favouritesPlaylist)
        assertEquals(setOf("s1"), model.state.value.favourites)
        model.chooseTab(EpisodeTab.Favourites)
        assertEquals(listOf("First Steps"), model.state.value.screen.rows.map { it.title })
    }

    @Test
    fun `a heart adds the episode, and says so once`() {
        playlists.add("pl_1", "Bedtime", "lib", PlaylistItem("p1", "s1"))
        val model = model(favourites = favourites())
        val episode = model.state.value.screen.rows.first { it.title == "Second Wind" }.onServer!!

        model.toggleFavourite(episode)

        assertEquals(setOf("s1", "s2"), model.state.value.favourites)
        assertEquals(HeartChange.Added("Bedtime"), model.state.value.heartChange)
        model.heartChangeShown()
        assertNull(model.state.value.heartChange)
    }

    @Test
    fun `with None chosen there are no hearts`() {
        val state = model(favourites = favourites(chosen = false)).state.value

        assertNull(state.favouritesPlaylist)
        assertNull(state.screen.tabs)
    }

    // #215: an empty queue a few seconds after asking is not a failure.
    @Test
    fun `an episode asked for, with nothing in the queue yet, stays requested`() {
        val api = Api(me = Me(type = "admin"), feedEpisodes = listOf(feedEpisode("g3", "Third Rail", 3)))
        val model = model(api)
        val row = model.state.value.screen.rows.first { it.title == "Third Rail" }

        model.download(row)
        clock = 10_000
        model.chooseTab(EpisodeTab.All)

        assertEquals(DownloadState.Requested, model.state.value.downloads[row.key])
    }

    @Test
    fun `a refused request says so, and the queue watched for another does not take that back`() {
        val api = Api(me = Me(type = "admin"), feedEpisodes = listOf(feedEpisode("g3", "Third Rail", 3), feedEpisode("g4", "Fourth Wall", 4)))
        val model = model(api)
        val refused = model.state.value.screen.rows.first { it.title == "Third Rail" }
        val other = model.state.value.screen.rows.first { it.title == "Fourth Wall" }

        api.refuse = true
        model.download(refused)
        api.refuse = false
        model.download(other)

        assertEquals(DownloadState.Failed, model.state.value.downloads[refused.key])
        assertEquals(DownloadState.Requested, model.state.value.downloads[other.key])
    }

    // Review: the whole path through the watch, as on the device - the queue
    // empty throughout, the episode on the server only after the window.
    @Test
    fun `an episode that lands after the window goes requested, failed, then downloaded, without reopening`() {
        val third = feedEpisode("g3", "Third Rail", 3)
        val landed: LibraryItemResponse = gson.fromJson(
            gson.toJson(item()).replace(""""episodes":[""", """"episodes":[{"id":"s3","title":"Third Rail","publishedAt":3,"guid":"g3"},"""),
            LibraryItemResponse::class.java
        )
        val seen = mutableMapOf<Long, DownloadState?>()
        lateinit var model: PodcastViewModel
        watchPause = {
            seen[clock] = model.state.value.downloads["feed:g3"]
            clock += 5_000
        }
        val late = DownloadProgress.FAIL_AFTER_MS + 30_000
        model = model(Api(me = Me(type = "admin"), feedEpisodes = listOf(third)), reload = { if (clock >= late) landed else item() })
        val row = model.state.value.screen.rows.first { it.title == "Third Rail" }

        model.download(row)

        assertEquals(DownloadState.Requested, seen[10_000])
        assertEquals(DownloadState.Failed, seen[DownloadProgress.FAIL_AFTER_MS + 5_000])
        val now = model.state.value.screen.rows.first { it.title == "Third Rail" }
        assertTrue(now.downloaded)
        assertNull(model.state.value.downloads[now.key])
    }
}
