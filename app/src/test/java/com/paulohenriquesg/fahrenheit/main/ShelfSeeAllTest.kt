package com.paulohenriquesg.fahrenheit.main

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which Home shelves end in a "See all" tile, and where it leads (#123). */
class ShelfSeeAllTest {

    private fun items(n: Int) = List(n) { i ->
        Gson().fromJson(
            """{"id":"i$i","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
                "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
                "isMissing":false,"isInvalid":false,"mediaType":"book",
                "media":{"metadata":{"title":"T$i"},"tags":[],"numTracks":0,"numAudioFiles":0,
                "numChapters":0,"duration":0.0,"size":0}}""",
            LibraryItem::class.java
        )
    }

    private fun books(id: String, type: String = "book", shown: Int = 10, total: Int? = 40) =
        Shelf(id = id, label = id, labelStringKey = id, type = type, bookEntities = items(shown), total = total)

    // --- where a tile leads ---

    @Test
    fun `recent series opens the Series tab`() {
        val shelf = Shelf(
            id = "recent-series", label = "", labelStringKey = "", type = "series",
            seriesEntities = List(10) { Series(id = "s$it", name = "S$it") }, total = 25
        )

        assertEquals(ShelfSeeAll.Tile(25, MenuAction.SERIES), ShelfSeeAll.of(shelf))
    }

    @Test
    fun `newest authors opens the Authors tab`() {
        val shelf = Shelf(
            id = "newest-authors", label = "", labelStringKey = "", type = "authors",
            authorEntities = List(10) { Author(id = "a$it", name = "A$it") }, total = 60
        )

        assertEquals(ShelfSeeAll.Tile(60, MenuAction.AUTHORS), ShelfSeeAll.of(shelf))
    }

    @Test
    fun `newest episodes opens Latest Episodes`() =
        assertEquals(ShelfSeeAll.Tile(40, MenuAction.LATEST), ShelfSeeAll.of(books("newest-episodes", type = "episode")))

    @Test
    fun `recently added podcasts open the library's podcasts`() =
        assertEquals(ShelfSeeAll.Tile(40, MenuAction.LIBRARY), ShelfSeeAll.of(books("recently-added", type = "podcast")))

    // --- when there is no tile ---

    @Test
    fun `a shelf showing everything it has gets no tile`() =
        assertNull(ShelfSeeAll.of(books("newest-episodes", type = "episode", shown = 10, total = 10)))

    @Test
    fun `a shelf without a total gets no tile, as there is nothing to promise`() =
        assertNull(ShelfSeeAll.of(books("newest-episodes", type = "episode", total = null)))

    @Test
    fun `discover is random picks, so no tile`() =
        assertNull(ShelfSeeAll.of(books("discover")))

    @Test
    fun `an unknown shelf gets no tile`() =
        assertNull(ShelfSeeAll.of(books("some-future-shelf")))

    // The library screen has no sort or filter yet: a tile there would open a
    // title-sorted list that does not match the shelf.
    @Test
    fun `recently added books wait for a date-added sort`() =
        assertNull(ShelfSeeAll.of(books("recently-added")))

    @Test
    fun `continue listening waits for an in-progress filter`() =
        assertNull(ShelfSeeAll.of(books("continue-listening")))

    @Test
    fun `listen again waits for a finished filter`() =
        assertNull(ShelfSeeAll.of(books("listen-again")))

    @Test
    fun `podcast continue listening, a shelf of episodes, gets no tile either`() =
        assertNull(ShelfSeeAll.of(books("continue-listening", type = "episode")))
}
