package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every episode in the feed, and whether the server has it (#76).
 *
 * Matching is the server's own rule (PodcastEpisode.checkMatchesGuidOrEnclosureUrl
 * at 2.36.0): the same guid, or else the same enclosure URL.
 */
class EpisodeListTest {

    private val gson = Gson()

    private fun server(id: String, title: String, publishedAt: Long, guid: String? = null, url: String? = null): Episode =
        gson.fromJson(
            """{"libraryItemId":"li","id":"$id","index":1,"title":"$title","publishedAt":$publishedAt,
                "addedAt":0,"updatedAt":0${guid?.let { ""","guid":"$it"""" } ?: ""}
                ${url?.let { ""","enclosure":{"url":"$it","type":"audio/mpeg"}""" } ?: ""}}""",
            Episode::class.java
        )

    private fun feed(title: String, publishedAt: Long, guid: String? = null, url: String = "https://cdn/$title.mp3"): JsonObject =
        gson.fromJson(
            """{"title":"$title","publishedAt":$publishedAt,"durationSeconds":1558,"pubDate":"Thu, 01 Oct 2026 11:23:35 +0000",
                ${guid?.let { """"guid":"$it",""" } ?: ""}"enclosure":{"url":"$url","length":"1","type":"audio/mpeg"}}""",
            JsonObject::class.java
        )

    @Test
    fun `a feed episode the server holds is matched by guid`() {
        val rows = EpisodeList.merge(
            server = listOf(server("s1", "Tabstack", 3, guid = "g1", url = "https://other/x.mp3")),
            feed = listOf(feed("Tabstack", 3, guid = "g1"))
        )

        assertEquals(1, rows.size)
        assertEquals("s1", rows[0].onServer?.id)
        assertTrue(rows[0].feed != null)
    }

    @Test
    fun `without a guid match, the enclosure URL decides`() {
        val rows = EpisodeList.merge(
            server = listOf(server("s1", "Anvil", 2, guid = "old-guid", url = "https://cdn/Anvil.mp3")),
            feed = listOf(feed("Anvil", 2, guid = "new-guid"))
        )

        assertEquals("s1", rows.single().onServer?.id)
    }

    @Test
    fun `two missing guids are not a match`() {
        val rows = EpisodeList.merge(
            server = listOf(server("s1", "Old", 1, url = "https://cdn/old.mp3")),
            feed = listOf(feed("New", 2, url = "https://cdn/new.mp3"))
        )

        assertEquals(2, rows.size)
        assertNull(rows.first { it.title == "New" }.onServer)
    }

    @Test
    fun `an episode only in the feed is listed as not downloaded`() {
        val rows = EpisodeList.merge(server = emptyList(), feed = listOf(feed("Supergood", 1, guid = "g3")))

        assertFalse(rows.single().downloaded)
    }

    @Test
    fun `an episode the feed has dropped is still listed, because the server still has it`() {
        val rows = EpisodeList.merge(
            server = listOf(server("s9", "From 2019", 1, guid = "gone")),
            feed = listOf(feed("New", 5, guid = "g1"))
        )

        assertEquals(listOf("New", "From 2019"), rows.map { it.title })
        assertTrue(rows[1].downloaded)
    }

    @Test
    fun `newest first, whichever side an episode came from`() {
        val rows = EpisodeList.merge(
            server = listOf(server("s1", "Middle", 2, guid = "only-on-server")),
            feed = listOf(feed("Oldest", 1, guid = "a"), feed("Newest", 3, guid = "b"))
        )

        assertEquals(listOf("Newest", "Middle", "Oldest"), rows.map { it.title })
    }

    @Test
    fun `without the feed, the list is what the server has`() {
        val rows = EpisodeList.merge(server = listOf(server("s1", "A", 1), server("s2", "B", 2)), feed = null)

        assertEquals(listOf("B", "A"), rows.map { it.title })
        assertTrue(rows.all { it.downloaded })
    }

    @Test
    fun `a feed episode keeps the server's own copy of it, to send back for download`() {
        val raw = feed("Supergood", 1, guid = "g3")

        val row = EpisodeList.merge(server = emptyList(), feed = listOf(raw)).single()

        assertEquals(raw, row.feed)
    }

    @Test
    fun `the tabs filter and count`() {
        val rows = EpisodeList.merge(
            server = listOf(server("s1", "Have", 3, guid = "g1")),
            feed = listOf(feed("Have", 3, guid = "g1"), feed("Missing 1", 2, guid = "g2"), feed("Missing 2", 1, guid = "g3"))
        )

        assertEquals(3, EpisodeList.filter(rows, EpisodeTab.All).size)
        assertEquals(listOf("Have"), EpisodeList.filter(rows, EpisodeTab.OnServer).map { it.title })
        assertEquals(listOf("Missing 1", "Missing 2"), EpisodeList.filter(rows, EpisodeTab.NotDownloaded).map { it.title })
    }

    @Test
    fun `a row carries what the screen shows`() {
        val row = EpisodeList.merge(server = emptyList(), feed = listOf(feed("Supergood", 1_790_853_815_000, guid = "g3"))).single()

        assertEquals("Supergood", row.title)
        assertEquals(1_790_853_815_000, row.publishedAt)
        assertEquals(1558.0, row.durationSeconds!!, 0.0)
    }
}
