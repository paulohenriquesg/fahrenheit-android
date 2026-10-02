package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What a podcast's screen says (#75, #76). "No Episodes" read as "this feed is
 * empty", when the server had not looked at it in 705 days.
 */
class PodcastScreenModelTest {

    private val gson = Gson()
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_790_942_400_000L // 2026-10-02 12:00 UTC

    private fun server(id: String, publishedAt: Long = 1, guid: String = "g-$id"): Episode = gson.fromJson(
        """{"libraryItemId":"li","id":"$id","index":1,"title":"$id","publishedAt":$publishedAt,"addedAt":0,"updatedAt":0,"guid":"$guid"}""",
        Episode::class.java
    )

    private fun feed(guid: String, publishedAt: Long = 1): JsonObject = gson.fromJson(
        """{"title":"$guid","guid":"$guid","publishedAt":$publishedAt,"enclosure":{"url":"https://cdn/$guid.mp3"}}""",
        JsonObject::class.java
    )

    private fun model(
        server: List<Episode> = emptyList(),
        feed: FeedLoad = FeedLoad.Unavailable,
        tab: EpisodeTab = EpisodeTab.All,
        lastCheck: Long? = now - 705 * day,
        autoDownload: Boolean? = false
    ) = PodcastScreenModel.of(server, feed, tab, lastCheck, autoDownload, now, serverFormat = "yyyy-MM-dd")

    @Test
    fun `with the feed read, it says how much of it the server holds`() {
        val m = model(
            server = listOf(server("a", guid = "g1")),
            feed = FeedLoad.Loaded(listOf(feed("g1"), feed("g2"), feed("g3")))
        )

        assertEquals(
            listOf(
                Fact("1 of 3 on the server"),
                Fact("Feed last checked: 2024-10-27"),
                Fact("Automatic downloads off", warn = true)
            ),
            m.facts
        )
    }

    @Test
    fun `without the feed, it counts only what the server has`() {
        assertEquals(Fact("2 episodes on the server"), model(server = listOf(server("a"), server("b"))).facts[0])
        assertEquals(Fact("1 episode on the server"), model(server = listOf(server("a"))).facts[0])
    }

    @Test
    fun `a feed never checked says so, and zero is never rather than 1970`() {
        assertEquals(Fact("Feed never checked"), model(lastCheck = null).facts[1])
        assertEquals(Fact("Feed never checked"), model(lastCheck = 0).facts[1])
    }

    @Test
    fun `a recent check reads as recent, and automatic downloads on is not a warning`() {
        val m = model(lastCheck = now - day / 2, autoDownload = true)

        assertEquals(Fact("Feed last checked: Today"), m.facts[1])
        assertEquals(Fact("Automatic downloads on"), m.facts[2])
    }

    @Test
    fun `an unknown setting is not guessed at`() {
        assertEquals(2, model(autoDownload = null).facts.size)
    }

    @Test
    fun `tabs come with counts once the feed is read`() {
        val m = model(
            server = listOf(server("a", guid = "g1")),
            feed = FeedLoad.Loaded(listOf(feed("g1"), feed("g2"), feed("g3")))
        )

        assertEquals(
            listOf(EpisodeTab.All to 3, EpisodeTab.OnServer to 1, EpisodeTab.NotDownloaded to 2),
            m.tabs
        )
    }

    @Test
    fun `the chosen tab decides the rows`() {
        val m = model(
            server = listOf(server("a", guid = "g1")),
            feed = FeedLoad.Loaded(listOf(feed("g1", 3), feed("g2", 2))),
            tab = EpisodeTab.NotDownloaded
        )

        assertEquals(listOf("g2"), m.rows.map { it.title })
    }

    @Test
    fun `there are no tabs when there is only one kind of row`() {
        assertNull(model(server = listOf(server("a"))).tabs)
    }

    @Test
    fun `while the feed is read, the server's episodes show and the screen says it is reading`() {
        val m = model(server = listOf(server("a")), feed = FeedLoad.Loading)

        assertEquals(1, m.rows.size)
        assertEquals(Note(body = "Reading the feed…"), m.note)
    }

    @Test
    fun `a feed that cannot be read says so, and shows what the server has`() {
        val m = model(server = listOf(server("a")), feed = FeedLoad.Failed)

        assertEquals(1, m.rows.size)
        assertEquals(Note(body = "Could not read the feed. Showing what the server has."), m.note)
    }

    @Test
    fun `someone who is not an admin is told the list is partial`() {
        val m = model(server = listOf(server("a")), feed = FeedLoad.Unavailable)

        assertEquals(
            Note(body = "Only episodes the server has downloaded are listed. An admin can fetch more from the feed."),
            m.note
        )
    }

    @Test
    fun `nothing downloaded and no feed says why, not "No Episodes"`() {
        val m = model(feed = FeedLoad.Unavailable)

        assertEquals(Fact("Nothing downloaded yet"), m.facts[0])
        assertEquals(
            Note(
                title = "Nothing downloaded yet",
                body = "The server holds no episodes of this podcast. An admin can fetch them from the feed."
            ),
            m.note
        )
    }

    @Test
    fun `an admin with the feed read needs no note`() {
        assertNull(model(feed = FeedLoad.Loaded(listOf(feed("g1")))).note)
    }
}
