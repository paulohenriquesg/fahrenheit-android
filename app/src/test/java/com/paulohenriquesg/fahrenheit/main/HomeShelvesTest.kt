package com.paulohenriquesg.fahrenheit.main

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.main.HomeShelves.Action
import com.paulohenriquesg.fahrenheit.main.HomeShelves.Behaviour
import com.paulohenriquesg.fahrenheit.main.HomeShelves.Style
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * One table from a server shelf to how Home draws it and what its cards do
 * (#147): the shelf id first, then its type. Adding a server shelf is a row.
 */
class HomeShelvesTest {

    private val item: LibraryItem = Gson().fromJson(
        """{"id":"b1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"T"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun books(id: String, type: String) =
        Shelf(id = id, label = "", labelStringKey = "", type = type, bookEntities = listOf(item))

    @Test
    fun `continue listening plays on a press, details on a long press, See all in progress`() =
        assertEquals(
            Behaviour(Style.Covers, press = Action.Play, longPress = Action.Details, seeAll = MenuAction.LIBRARY to LibraryQuery.InProgress),
            HomeShelves.of(books("continue-listening", "book"))
        )

    @Test
    fun `listen again opens details, See all finished`() =
        assertEquals(
            Behaviour(Style.Covers, seeAll = MenuAction.LIBRARY to LibraryQuery.Finished),
            HomeShelves.of(books("listen-again", "book"))
        )

    @Test
    fun `recently added opens details, See all newest first, books and podcasts alike`() {
        val expected = Behaviour(Style.Covers, seeAll = MenuAction.LIBRARY to LibraryQuery.RecentlyAdded)
        assertEquals(expected, HomeShelves.of(books("recently-added", "book")))
        assertEquals(expected, HomeShelves.of(books("recently-added", "podcast")))
    }

    @Test
    fun `a podcast library's continue listening is episodes, with no See all`() =
        assertEquals(Behaviour(Style.Episodes), HomeShelves.of(books("continue-listening", "episode")))

    @Test
    fun `newest episodes is episodes, with no See all`() =
        assertEquals(Behaviour(Style.Episodes), HomeShelves.of(books("newest-episodes", "episode")))

    @Test
    fun `discover is covers opening details, with no See all`() =
        assertEquals(Behaviour(Style.Covers), HomeShelves.of(books("discover", "book")))

    @Test
    fun `recent series is series cards, See all the Series tab`() {
        val shelf = Shelf(id = "recent-series", label = "", labelStringKey = "", type = "series",
            seriesEntities = listOf(Series(id = "s1", name = "S")))
        assertEquals(Behaviour(Style.Series, seeAll = MenuAction.SERIES to LibraryQuery.Everything), HomeShelves.of(shelf))
    }

    @Test
    fun `newest authors is author cards, See all the Authors tab`() {
        val shelf = Shelf(id = "newest-authors", label = "", labelStringKey = "", type = "authors",
            authorEntities = listOf(Author(id = "a1", name = "A")))
        assertEquals(Behaviour(Style.Authors, seeAll = MenuAction.AUTHORS to LibraryQuery.Everything), HomeShelves.of(shelf))
    }

    // A shelf type a later server may add: drawn, not dropped.
    @Test
    fun `a shelf of a type Home does not know, holding library items, is a plain cover row`() =
        assertEquals(Behaviour(Style.Covers), HomeShelves.of(books("a-future-shelf", "some-future-type")))

    @Test
    fun `a shelf of a type Home does not know, holding nothing it can read, is not drawn`() =
        assertNull(HomeShelves.of(Shelf(id = "x", label = "", labelStringKey = "", type = "some-future-type")))

    // Matched by their type, not their id: the rows an id table could break.
    @Test
    fun `continue series, continue reading and read again are covers opening details, with no See all`() {
        for (id in listOf("continue-series", "continue-reading", "read-again")) {
            assertEquals(id, Behaviour(Style.Covers), HomeShelves.of(books(id, "book")))
        }
    }

    @Test
    fun `a podcast library's listen again is episodes, with no See all and no finished marks`() =
        assertEquals(Behaviour(Style.Episodes, marksFinished = false), HomeShelves.of(books("listen-again", "episode")))
}
