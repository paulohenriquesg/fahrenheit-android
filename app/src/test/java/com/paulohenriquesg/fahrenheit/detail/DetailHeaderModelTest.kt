package com.paulohenriquesg.fahrenheit.detail

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.podcast.Fact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The header a book and a podcast share (frame 3 of docs/mocks/screens.html):
 * title, a by-line, facts as chips, one primary action, the description.
 */
class DetailHeaderModelTest {

    private fun book(
        authors: String = """[{"id":"a1","name":"Andy Weir"}]""",
        narrators: String = """["Ray Porter"]""",
        genres: String = """["Science Fiction"]""",
        year: String? = "2021",
        progress: String? = null
    ): LibraryItemResponse = Gson().fromJson(
        """{"id":"b1","mediaType":"book","media":{"duration":58200.0,"metadata":{"title":"Project Hail Mary",
            "authors":$authors,"narrators":$narrators,"genres":$genres,
            ${year?.let { """"publishedYear":"$it",""" } ?: ""}"description":"<p>Ryland Grace</p>","explicit":false}}
            ${progress?.let { ""","userMediaProgress":$it""" } ?: ""}}""",
        LibraryItemResponse::class.java
    )

    /** The item's own copy of its progress, as most cases here need; the screen passes the store's (#207). */
    private fun header(item: LibraryItemResponse) = DetailHeaderModel.book(item, item.userMediaProgress)

    private fun progressOf(item: LibraryItemResponse) = DetailHeaderModel.progressOf(item.userMediaProgress)

    // #207: the item was read on open; what the player and the marks wrote since is in the store.
    @Test
    fun `the header says what it is given, not the item's copy from when the page opened`() {
        val opened = book(progress = """{"progress":0.34,"currentTime":19680.0,"isFinished":false}""")
        val now = MediaProgressResponse(libraryItemId = "b1", currentTime = 58200.0, progress = 1.0, isFinished = true)

        val header = DetailHeaderModel.book(opened, now)

        assertEquals(Fact("Finished"), header.chips.last())
        assertEquals("Play", header.primary)
    }

    private fun podcast(genres: String = """["Technology","News:Tech News"]""", episodes: Int = 2): LibraryItemResponse =
        Gson().fromJson(
            """{"id":"p1","mediaType":"podcast","media":{"metadata":{"title":"APIs You Won't Hate","author":"APIs You Won't Hate",
                "genres":$genres,"explicit":false},"episodes":[${(1..episodes).joinToString(",") { """{"id":"e$it","title":"E$it","publishedAt":$it}""" }}]}}""",
            LibraryItemResponse::class.java
        )

    @Test
    fun `a book's by-line names its authors and narrators`() {
        assertEquals("Andy Weir · narrated by Ray Porter", header(book()).byline)
    }

    @Test
    fun `several authors are listed, and a book with no narrator says only who wrote it`() {
        val header = header(
            book(authors = """[{"id":"a","name":"Terry Pratchett"},{"id":"b","name":"Neil Gaiman"}]""", narrators = "[]")
        )

        assertEquals("Terry Pratchett, Neil Gaiman", header.byline)
    }

    @Test
    fun `a book's facts are its length, year, genre and how far in`() {
        val header = header(book(progress = """{"progress":0.34,"currentTime":19680.0,"isFinished":false}"""))

        assertEquals(
            listOf(Fact("16h 10m"), Fact("2021"), Fact("Science Fiction"), Fact("34% in")),
            header.chips
        )
    }

    @Test
    fun `facts the server did not send are left out`() {
        val header = header(book(genres = "[]", year = null))

        assertEquals(listOf(Fact("16h 10m")), header.chips)
    }

    // #194: no genres, or only blank ones, leave the genre out rather than an empty fact.
    @Test
    fun `blank genres are left out`() {
        assertEquals(listOf(Fact("16h 10m")), header(book(genres = """[" ",""]""", year = null)).chips)
        assertEquals(
            listOf(Fact("16h 10m"), Fact("Science Fiction")),
            header(book(genres = """[" ","Science Fiction"]""", year = null)).chips
        )
    }

    @Test
    fun `a finished book says so`() {
        val header = header(book(progress = """{"progress":1.0,"currentTime":58200.0,"isFinished":true}"""))

        assertEquals(Fact("Finished"), header.chips.last())
    }

    @Test
    fun `a book in progress says where it resumes`() {
        val header = header(book(progress = """{"progress":0.34,"currentTime":19680.0,"isFinished":false}"""))

        assertEquals("Resume at 5h 28m", header.primary)
    }

    @Test
    fun `a book not started, or finished, just plays`() {
        assertEquals("Play", header(book()).primary)
        assertEquals(
            "Play",
            header(book(progress = """{"progress":1.0,"currentTime":58200.0,"isFinished":true}""")).primary
        )
    }

    @Test
    fun `a podcast's by-line says what it is, with its genre`() {
        assertEquals("Podcast · Technology", DetailHeaderModel.podcast(podcast(), emptyList()).byline)
        assertEquals("Podcast", DetailHeaderModel.podcast(podcast(genres = "[]"), emptyList()).byline)
    }

    @Test
    fun `a podcast plays its newest episode, when it has one`() {
        assertEquals("Play newest episode", DetailHeaderModel.podcast(podcast(), emptyList()).primary)
        assertNull(DetailHeaderModel.podcast(podcast(episodes = 0), emptyList()).primary)
    }

    @Test
    fun `a podcast's chips are the facts about its feed`() {
        val facts = listOf(Fact("4 of 56 on the server"), Fact("Automatic downloads off", warn = true))

        assertEquals(facts, DetailHeaderModel.podcast(podcast(), facts).chips)
    }

    @Test
    fun `the title comes through`() {
        assertEquals("Project Hail Mary", header(book()).title)
    }

    // Seen on the stick: 14 seconds in read "Resume at 0m" and "0% in".
    @Test
    fun `a book only seconds in does not claim to resume at zero`() {
        val header = header(book(progress = """{"progress":0.00024,"currentTime":14.0,"isFinished":false}"""))

        assertEquals("Resume", header.primary)
        assertEquals(Fact("Just started"), header.chips.last())
    }

    @Test
    fun `the description is passed on whole`() {
        assertEquals("<p>Ryland Grace</p>", header(book()).description)
    }

    @Test
    fun `a podcast with an episode in progress resumes it, by name`() {
        assertEquals(
            "Resume E2",
            DetailHeaderModel.podcast(podcast(), emptyList(), resumeTitle = "E2").primary
        )
    }

    // #134: the book screen's facts list says how far in, where the chip did.
    @Test
    fun `how far in, as the book screen's facts say it`() {
        assertEquals("34% in", progressOf(book(progress = """{"currentTime":19788.0,"progress":0.34,"isFinished":false}""")))
        assertEquals("Finished", progressOf(book(progress = """{"currentTime":100.0,"progress":1.0,"isFinished":true}""")))
        assertEquals(null, progressOf(book()))
    }
}
