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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** The rest of a book's series, for About and "Book N of M" (#107). */
class SeriesBooksTest {
    private val server = MockWebServer().apply { start() }
    private val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        .create(LibraryApi::class.java)

    @After
    fun tearDown() = server.shutdown()

    private val three = """{"results":[
        {"id":"b1","media":{"metadata":{"title":"The First","explicit":false}}},
        {"id":"b2","media":{"metadata":{"title":"The Second","explicit":false}}},
        {"id":"b3","media":{"metadata":{"title":"The Third","explicit":false}}}],"total":3}"""

    // Review Focus 2: the server base64-decodes the id after URL-decoding it.
    @Test
    fun `the series is asked for by its encoded id, in series order`() = runBlocking {
        server.enqueue(MockResponse().setBody(three))

        LibraryRepository(api).seriesBooks(libraryId = "l1", seriesId = "s1")

        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertTrue(request.path!!, request.path!!.startsWith("/api/libraries/l1/items?"))
        assertTrue(request.path!!, request.path!!.contains("filter=series.czE%3D"))
        assertEquals("sequence", request.requestUrl!!.queryParameter("sort"))
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
}
