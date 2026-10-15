package com.paulohenriquesg.fahrenheit.author

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.progress.ProgressKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The author's and a narrator's books as the page lays them out (#230): one
 * shelf per series in series order, the rest under "Other books", and the
 * book Continue plays.
 */
class PersonShelvesTest {

    @Test
    fun `books are grouped by series, by sequence, and the rest go to Other books`() {
        val shelves = PersonShelves.of(
            listOf(
                book("b3", "Book Three", series("s1", "The Long Way", "3")),
                book("q", "A Quiet Signal"),
                book("b1", "Book One", series("s1", "The Long Way", "1")),
                book("x2", "Second Light", series("s2", "Another Road", "2")),
                book("f", "Far Harbour"),
                book("b10", "Book Ten", series("s1", "The Long Way", "10")),
                book("b2", "Book Two", series("s1", "The Long Way", "2.5"))
            )
        )

        assertEquals(listOf("Another Road", "The Long Way", null), shelves.map { it.series })
        assertEquals(listOf("b1", "b2", "b3", "b10"), shelves[1].books.map { it.item.id })
        assertEquals(listOf("1", "2.5", "3", "10"), shelves[1].books.map { it.number })
        assertEquals(listOf("q", "f"), shelves[2].books.map { it.item.id })
        assertEquals(listOf(null, null), shelves[2].books.map { it.number })
    }

    @Test
    fun `a book in two series is on both shelves, and there is no Other books when every book has a series`() {
        val shelves = PersonShelves.of(
            listOf(book("b1", "Book One", series("s1", "First", "1"), series("s2", "Second", "4")))
        )

        assertEquals(listOf("First", "Second"), shelves.map { it.series })
        assertEquals(listOf("1", "4"), shelves.map { it.books.single().number })
    }

    @Test
    fun `books without a sequence come after the numbered ones, by title`() {
        val shelves = PersonShelves.of(
            listOf(
                book("z", "Zed", series("s1", "S", null)),
                book("a", "Aye", series("s1", "S", null)),
                book("one", "One", series("s1", "S", "1"))
            )
        )

        assertEquals(listOf("one", "a", "z"), shelves.single().books.map { it.item.id })
    }

    @Test
    fun `an author's books take their places from the author's series`() {
        val author = Gson().fromJson(
            """{"id":"a1","name":"An Author","libraryId":"lib","addedAt":0,"updatedAt":0,
               "libraryItems":[${json("b2", "Book Two")}, ${json("b1", "Book One")}, ${json("q", "A Quiet Signal")}],
               "series":[{"id":"s1","name":"The Long Way","items":[
                   ${json("b2", "Book Two", """{"id":"s1","name":"The Long Way","sequence":"2"}""")},
                   ${json("b1", "Book One", """{"id":"s1","name":"The Long Way","sequence":"1"}""")}]}]}""",
            AuthorDetailResponse::class.java
        )

        val shelves = PersonShelves.of(author)

        assertEquals(listOf("The Long Way", null), shelves.map { it.series })
        assertEquals(listOf("b1", "b2"), shelves[0].books.map { it.item.id })
        assertEquals(listOf("q"), shelves[1].books.map { it.item.id })
    }

    @Test
    fun `two editions of one title stay apart by their subtitle and length`() {
        val shelf = PersonShelves.of(
            listOf(
                book("long", "Book Two", series("s1", "S", "2"), subtitle = "Unabridged", duration = 52_800.0),
                book("short", "Book Two", series("s1", "S", "2"), duration = 10_860.0)
            )
        ).single()

        val cards = shelf.books.map { PersonCard.of(it, byAuthor = false) }

        assertEquals(listOf("Unabridged", null), cards.map { it.tag })
        assertEquals(listOf("#2 · 14 h 40 min", "#2 · 3 h 1 min"), cards.map { it.detail })
    }

    @Test
    fun `a narrator's card names the author instead of the length`() {
        val card = PersonCard.of(ShelfBook(book("b1", "Book One", author = "An Author"), number = null), byAuthor = true)

        assertEquals("An Author", card.detail)
        assertNull(card.tag)
    }

    @Test
    fun `Continue is the book last played among those in progress, never a finished or unstarted one`() {
        val items = listOf(book("done", "Done"), book("older", "Older"), book("newer", "Newer"), book("fresh", "Fresh"))
        val progress = mapOf(
            ProgressKey("done", null) to progress("done", 100.0, finished = true, at = 30),
            ProgressKey("older", null) to progress("older", 100.0, at = 10),
            ProgressKey("newer", null) to progress("newer", 50.0, at = 20),
            ProgressKey("fresh", null) to progress("fresh", 0.0, at = 40),
            ProgressKey("elsewhere", null) to progress("elsewhere", 10.0, at = 50)
        )

        assertEquals("newer", PersonShelves.continueOf(items, progress)?.id)
        assertNull(PersonShelves.continueOf(items.take(1), progress))
    }

    private data class SeriesJson(val id: String, val name: String, val sequence: String?)

    private fun series(id: String, name: String, sequence: String?) = SeriesJson(id, name, sequence)

    private fun progress(itemId: String, currentTime: Double, finished: Boolean = false, at: Long) =
        MediaProgressResponse(libraryItemId = itemId, currentTime = currentTime, isFinished = finished, lastUpdate = at)

    private fun book(
        id: String,
        title: String,
        vararg series: SeriesJson,
        subtitle: String? = null,
        duration: Double = 3600.0,
        author: String? = null
    ): LibraryItem {
        val list = series.joinToString(",", "[", "]") {
            """{"id":"${it.id}","name":"${it.name}"${it.sequence?.let { s -> ",\"sequence\":\"$s\"" } ?: ""}}"""
        }
        return Gson().fromJson(json(id, title, list, subtitle, duration, author), LibraryItem::class.java)
    }

    private fun json(
        id: String,
        title: String,
        series: String? = null,
        subtitle: String? = null,
        duration: Double = 3600.0,
        author: String? = null
    ): String = """{"id":"$id","mediaType":"book","media":{"duration":$duration,"metadata":{"title":"$title"
        ${subtitle?.let { ",\"subtitle\":\"$it\"" } ?: ""}${author?.let { ",\"authorName\":\"$it\"" } ?: ""}
        ${series?.let { ",\"series\":$it" } ?: ""}}}}"""
}
