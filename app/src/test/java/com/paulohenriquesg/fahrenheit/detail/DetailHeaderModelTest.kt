package com.paulohenriquesg.fahrenheit.detail

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
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

    private fun podcast(genres: String = """["Technology","News:Tech News"]""", episodes: Int = 2): LibraryItemResponse =
        Gson().fromJson(
            """{"id":"p1","mediaType":"podcast","media":{"metadata":{"title":"APIs You Won't Hate","author":"APIs You Won't Hate",
                "genres":$genres,"explicit":false},"episodes":[${(1..episodes).joinToString(",") { """{"id":"e$it","title":"E$it","publishedAt":$it}""" }}]}}""",
            LibraryItemResponse::class.java
        )

    @Test
    fun `a book's by-line names its authors and narrators`() {
        assertEquals("Andy Weir · narrated by Ray Porter", DetailHeaderModel.book(book()).byline)
    }

    @Test
    fun `several authors are listed, and a book with no narrator says only who wrote it`() {
        val header = DetailHeaderModel.book(
            book(authors = """[{"id":"a","name":"Terry Pratchett"},{"id":"b","name":"Neil Gaiman"}]""", narrators = "[]")
        )

        assertEquals("Terry Pratchett, Neil Gaiman", header.byline)
    }

    @Test
    fun `a book's facts are its length, year, genre and how far in`() {
        val header = DetailHeaderModel.book(book(progress = """{"progress":0.34,"currentTime":19680.0,"isFinished":false}"""))

        assertEquals(
            listOf(Fact("16h 10m"), Fact("2021"), Fact("Science Fiction"), Fact("34% in")),
            header.chips
        )
    }

    @Test
    fun `facts the server did not send are left out`() {
        val header = DetailHeaderModel.book(book(genres = "[]", year = null))

        assertEquals(listOf(Fact("16h 10m")), header.chips)
    }

    @Test
    fun `a finished book says so`() {
        val header = DetailHeaderModel.book(book(progress = """{"progress":1.0,"currentTime":58200.0,"isFinished":true}"""))

        assertEquals(Fact("Finished"), header.chips.last())
    }

    @Test
    fun `a book in progress says where it resumes`() {
        val header = DetailHeaderModel.book(book(progress = """{"progress":0.34,"currentTime":19680.0,"isFinished":false}"""))

        assertEquals("Resume at 5h 28m", header.primary)
    }

    @Test
    fun `a book not started, or finished, just plays`() {
        assertEquals("Play", DetailHeaderModel.book(book()).primary)
        assertEquals(
            "Play",
            DetailHeaderModel.book(book(progress = """{"progress":1.0,"currentTime":58200.0,"isFinished":true}""")).primary
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
        assertEquals("Project Hail Mary", DetailHeaderModel.book(book()).title)
    }

    // Seen on the stick: 14 seconds in read "Resume at 0m" and "0% in".
    @Test
    fun `a book only seconds in does not claim to resume at zero`() {
        val header = DetailHeaderModel.book(book(progress = """{"progress":0.00024,"currentTime":14.0,"isFinished":false}"""))

        assertEquals("Resume", header.primary)
        assertEquals(Fact("Just started"), header.chips.last())
    }

    // Also seen on the stick: <br /><br /> spent the three-line preview on a
    // blank line and a lone ellipsis.
    @Test
    fun `the preview runs paragraphs together instead of spending lines on breaks`() {
        val header = DetailHeaderModel.book(book())
        val broken = DetailHeaderModel.previewOf("<b>The first novel!</b><br /><br />When a shuttle fails,<p>Pike</p><p>suspects</p>")

        assertEquals("<b>The first novel!</b> When a shuttle fails, Pike suspects", broken)
        // The model passes it on whole: a book shows all of it, a podcast a preview.
        assertEquals("<p>Ryland Grace</p>", header.description)
    }

    @Test
    fun `a podcast with an episode in progress resumes it, by name`() {
        assertEquals(
            "Resume E2",
            DetailHeaderModel.podcast(podcast(), emptyList(), resumeTitle = "E2").primary
        )
    }
}
