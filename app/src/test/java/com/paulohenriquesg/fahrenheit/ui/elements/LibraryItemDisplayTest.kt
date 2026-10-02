package com.paulohenriquesg.fahrenheit.ui.elements

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What a card on the library grid shows. */
class LibraryItemDisplayTest {

    private fun item(
        mediaType: String = "book",
        title: String = "Dune",
        recentEpisodeTitle: String? = null,
        hasRecentEpisode: Boolean = recentEpisodeTitle != null,
        unfinished: Int? = null,
        episodes: Int? = null
    ): LibraryItem {
        val episode = if (hasRecentEpisode) {
            """"recentEpisode":{"id":"e1","libraryItemId":"li1"${recentEpisodeTitle?.let { ""","title":"$it"""" } ?: ""}},"""
        } else ""
        val incomplete = unfinished?.let { """"numEpisodesIncomplete":$it,""" } ?: ""
        return Gson().fromJson(
            """{"id":"li1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
                "isFile":true,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
                "isMissing":false,"isInvalid":false,"mediaType":"$mediaType",$episode$incomplete
                "media":{"metadata":{"title":"$title"},"tags":[],"numTracks":0,"numAudioFiles":0,
                "numChapters":0,"duration":0.0,"size":0${episodes?.let { ""","numEpisodes":$it""" } ?: ""}}}""",
            LibraryItem::class.java
        )
    }

    @Test
    fun `a book shows its own title`() =
        assertEquals("Dune", LibraryItemDisplay.title(item(title = "Dune")))

    @Test
    fun `a podcast shows the newest episode`() =
        assertEquals(
            "Episode 42",
            LibraryItemDisplay.title(item(mediaType = "podcast", title = "The Show", recentEpisodeTitle = "Episode 42"))
        )

    @Test
    fun `a podcast episode with no title of its own shows the podcast's`() =
        assertEquals(
            "The Show",
            LibraryItemDisplay.title(item(mediaType = "podcast", title = "The Show", hasRecentEpisode = true))
        )

    @Test
    fun `a podcast with no recent episode shows its own title`() =
        assertEquals("The Show", LibraryItemDisplay.title(item(mediaType = "podcast", title = "The Show")))

    @Test
    fun `unfinished episodes are counted on the badge`() =
        assertEquals("7", LibraryItemDisplay.unfinishedBadge(item(mediaType = "podcast", unfinished = 7)))

    @Test
    fun `a long backlog is shortened`() =
        assertEquals("99+", LibraryItemDisplay.unfinishedBadge(item(mediaType = "podcast", unfinished = 120)))

    // Caught up should look caught up, not carry a red zero.
    @Test
    fun `nothing unfinished means no badge`() =
        assertNull(LibraryItemDisplay.unfinishedBadge(item(mediaType = "podcast", unfinished = 0)))

    @Test
    fun `a book never carries the badge`() =
        assertNull(LibraryItemDisplay.unfinishedBadge(item(mediaType = "book", unfinished = 7)))

    @Test
    fun `a podcast the server says nothing about carries no badge`() =
        assertNull(LibraryItemDisplay.unfinishedBadge(item(mediaType = "podcast")))

    // #75: 102 of 134 podcasts on the test server had nothing downloaded, and
    // the grid drew all 134 the same.
    @Test
    fun `a podcast says how many episodes the server holds`() =
        assertEquals("56 episodes", LibraryItemDisplay.episodeCount(item(mediaType = "podcast", episodes = 56)))

    @Test
    fun `one episode is one episode`() =
        assertEquals("1 episode", LibraryItemDisplay.episodeCount(item(mediaType = "podcast", episodes = 1)))

    @Test
    fun `an empty podcast is visible before it is opened`() =
        assertEquals("Nothing downloaded", LibraryItemDisplay.episodeCount(item(mediaType = "podcast", episodes = 0)))

    @Test
    fun `no count from the server means no line, not a zero`() =
        assertNull(LibraryItemDisplay.episodeCount(item(mediaType = "podcast")))

    @Test
    fun `a book has no episode count`() =
        assertNull(LibraryItemDisplay.episodeCount(item(mediaType = "book", episodes = 3)))

    // The Home shelves draw episodes on the same card, and the server sends
    // numEpisodes 0 for every one of them - NerdCast included.
    @Test
    fun `a card standing for an episode has no count`() =
        assertNull(
            LibraryItemDisplay.episodeCount(
                item(mediaType = "podcast", recentEpisodeTitle = "NerdCast 754", episodes = 0)
            )
        )
}
