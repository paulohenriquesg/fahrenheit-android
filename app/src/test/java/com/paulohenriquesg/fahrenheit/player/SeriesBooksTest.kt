package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.LibraryApi
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** The rest of a book's series, for About and "Book N of M" (#107). */
@RunWith(AndroidJUnit4::class)
class SeriesBooksTest {
    private val server = MockWebServer().apply { start() }
    private val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        .create(LibraryApi::class.java)

    @After
    fun tearDown() = server.shutdown()

    private val three = """{"results":[
        {"id":"b1","media":{"numTracks":1,"metadata":{"title":"The First","explicit":false}}},
        {"id":"b2","media":{"numTracks":2,"metadata":{"title":"The Second","explicit":false}}},
        {"id":"b3","media":{"numTracks":1,"metadata":{"title":"The Third","explicit":false}}}],"total":3}"""

    // Review Focus 2: the server base64-decodes the id after URL-decoding it.
    @Test
    fun `the series is asked for by its encoded id, in series order`() = runBlocking {
        server.enqueue(MockResponse().setBody(three))

        LibraryRepository(api).seriesBooks(libraryId = "l1", seriesId = "s1")

        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertTrue(request.path!!, request.path!!.startsWith("/api/libraries/l1/items?"))
        assertTrue(request.path!!, request.path!!.contains("filter=series.czE%3D"))
        assertEquals("sequence", request.requestUrl!!.queryParameter("sort"))
        // Review: only ids, titles and track counts are needed.
        assertEquals("1", request.requestUrl!!.queryParameter("minified"))
    }

    @Test
    fun `the books come in the server's order, with this one's place`() = runBlocking {
        server.enqueue(MockResponse().setBody(three))

        val series = SeriesBooks.of(LibraryRepository(api).seriesBooks("l1", "s1").getOrThrow(), currentId = "b2")

        assertEquals(listOf("The First", "The Second", "The Third"), series.books.map { it.title })
        assertEquals(1, series.current)
        assertEquals(3, series.total)
    }

    @Test
    fun `a book the list does not have has no place`() =
        assertNull(SeriesBooks(listOf(SeriesBook("b1", "The First")), currentId = "b9").current)

    @Test
    fun `a failed request is a failure, not an empty series`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(LibraryRepository(api).seriesBooks("l1", "s1").isFailure)
    }

    // Review: an ebook-only book in the series would stop this one for an error screen.
    @Test
    fun `books with nothing to play are left out`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[
            {"id":"b1","media":{"numTracks":1,"metadata":{"title":"The First","explicit":false}}},
            {"id":"e2","media":{"numTracks":0,"metadata":{"title":"Only to Read","explicit":false}}}],"total":2}"""))

        val series = SeriesBooks.of(LibraryRepository(api).seriesBooks("l1", "s1").getOrThrow(), currentId = "b1")

        assertEquals(listOf("The First"), series.books.map { it.title })
    }

    // Device check: filtered by series, the server gives each book's
    // metadata.series as one object, not the list an item usually has.
    @Test
    fun `a series reply with each book's series as one object still reads`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[
            {"id":"b1","media":{"numTracks":1,"metadata":{"title":"The First","explicit":false,
              "series":{"id":"s1","name":"A Long Saga","sequence":"1"}}}},
            {"id":"b2","media":{"numTracks":1,"metadata":{"title":"The Second","explicit":false,
              "series":{"id":"s1","name":"A Long Saga","sequence":null}}}}],"total":2,"sortBy":"sequence"}"""))

        val books = LibraryRepository(api).seriesBooks("l1", "s1").getOrThrow()

        assertEquals(listOf("The First", "The Second"), books.map { it.media.metadata.title })
        assertEquals("A Long Saga", books[0].media.metadata.series!!.single().name)
    }

    // #194: every card read "<Series title> a…"; each now says where it sits.
    @Test
    fun `each book carries its place in the series`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[
            {"id":"b1","media":{"numTracks":1,"metadata":{"title":"The First","explicit":false,
              "series":{"id":"s1","name":"A Long Saga","sequence":"2.5"}}}},
            {"id":"b2","media":{"numTracks":1,"metadata":{"title":"The Second","explicit":false,
              "series":{"id":"s1","name":"A Long Saga","sequence":null}}}}],"total":2,"sortBy":"sequence"}"""))

        val series = SeriesBooks.of(LibraryRepository(api).seriesBooks("l1", "s1").getOrThrow(), currentId = "b1")

        assertEquals(listOf("2.5", null), series.books.map { it.sequence })
    }

    @Test
    fun `a book is labelled by its place, or by its title without one`() {
        assertEquals("Book 1", SeriesBook("b1", "The First", sequence = "1").label)
        assertEquals("Book 2.5", SeriesBook("b2", "The Second", sequence = " 2.5 ").label)
        assertEquals("The Third", SeriesBook("b3", "The Third", sequence = " ").label)
        assertEquals("The Fourth", SeriesBook("b4", "The Fourth").label)
    }
}
