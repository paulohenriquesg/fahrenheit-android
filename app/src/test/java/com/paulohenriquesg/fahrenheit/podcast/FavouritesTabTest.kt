package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The show's Favourites tab (#180, frame 2 of docs/mocks/podcast-actions.html). */
class FavouritesTabTest {

    private val gson = Gson()

    private fun server(id: String, publishedAt: Long, guid: String = "g-$id"): Episode = gson.fromJson(
        """{"libraryItemId":"li","id":"$id","index":1,"title":"$id","publishedAt":$publishedAt,"addedAt":0,"updatedAt":0,"guid":"$guid"}""",
        Episode::class.java
    )

    private fun feed(guid: String, publishedAt: Long): JsonObject = gson.fromJson(
        """{"title":"$guid","guid":"$guid","publishedAt":$publishedAt,"enclosure":{"url":"https://cdn/$guid.mp3"}}""",
        JsonObject::class.java
    )

    private val episodes = listOf(server("a", 3, "g1"), server("b", 2, "g2"), server("c", 1, "g3"))
    private val feed = FeedLoad.Loaded(listOf(feed("g1", 3), feed("g2", 2), feed("g3", 1), feed("g4", 0)))

    private fun model(
        tab: EpisodeTab = EpisodeTab.All,
        load: FeedLoad = feed,
        favourites: Set<String>? = null,
        kept: Set<String> = emptySet()
    ) = PodcastScreenModel.of(
        episodes, load, tab, null, null, now = 10, favourites = favourites, favouritesKept = kept
    )

    @Test
    fun `with None chosen there is no Favourites tab`() {
        assertEquals(listOf(EpisodeTab.All, EpisodeTab.OnServer, EpisodeTab.NotDownloaded), model().tabs!!.map { it.first })
        assertNull(model(load = FeedLoad.Unavailable).tabs)
    }

    @Test
    fun `chosen, the tab comes last and counts this show's episodes in the playlist`() {
        assertEquals(EpisodeTab.Favourites to 2, model(favourites = setOf("a", "c")).tabs!!.last())
    }

    @Test
    fun `without the feed, the tabs are All and Favourites`() {
        assertEquals(
            listOf(EpisodeTab.All to 3, EpisodeTab.Favourites to 1),
            model(load = FeedLoad.Unavailable, favourites = setOf("b")).tabs
        )
    }

    @Test
    fun `the tab lists the episodes in the playlist, newest first`() {
        assertEquals(
            listOf("server:a", "server:c"),
            model(EpisodeTab.Favourites, favourites = setOf("c", "a")).rows.map { it.key }
        )
    }

    @Test
    fun `an episode taken out while the tab is open stays listed, while the count follows`() {
        // In when the tab was chosen: a and c. Since then c went and b came.
        val m = model(EpisodeTab.Favourites, favourites = setOf("a", "b"), kept = setOf("a", "c"))

        assertEquals(listOf("server:a", "server:b", "server:c"), m.rows.map { it.key })
        assertEquals(EpisodeTab.Favourites to 2, m.tabs!!.last())
    }

    @Test
    fun `left on the tab when the choice goes, the screen shows All`() {
        assertEquals(4, model(EpisodeTab.Favourites, kept = setOf("a")).rows.size)
    }
}
