package com.paulohenriquesg.fahrenheit.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * What the author's and a narrator's pages ask the server for (#230), as its
 * source reads them: an author with its series, the library's narrators, and
 * a narrator's books by the items filter on their name.
 */
@RunWith(AndroidJUnit4::class)
class PersonRequestTest {
    private val server = MockWebServer().apply { start() }
    private val retrofit = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `an author is asked for with its books and its series`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"a1","name":"An Author","libraryId":"l1","addedAt":0,"updatedAt":0,"series":[{"id":"s1","name":"S","items":[]}]}"""))

        val author = BrowseRepository(retrofit.create(BrowseApi::class.java)).author("a1").getOrThrow()

        val sent = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/api/authors/a1", sent.requestUrl!!.encodedPath)
        assertEquals("items,series", sent.requestUrl!!.queryParameter("include"))
        assertEquals("S", author.series!!.single().name)
    }

    @Test
    fun `the narrators are unwrapped from their envelope`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"narrators":[{"id":"QSBSZWFkZXI%3D","name":"A Reader","numBooks":12}]}"""))

        val narrators = BrowseRepository(retrofit.create(BrowseApi::class.java)).narrators("l1").getOrThrow()

        assertEquals("/api/libraries/l1/narrators", server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath)
        assertEquals(listOf(Narrator("QSBSZWFkZXI%3D", "A Reader", 12)), narrators)
    }

    @Test
    fun `a narrator's books are the library's items filtered on the name, expanded for their series`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[],"total":0}"""))

        LibraryRepository(retrofit.create(LibraryApi::class.java)).narratorBooks("l1", "A Reader").getOrThrow()

        val sent = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/api/libraries/l1/items", sent.requestUrl!!.encodedPath)
        assertTrue(sent.path!!, sent.path!!.contains("filter=narrators.QSBSZWFkZXI%3D"))
        assertEquals("0", sent.requestUrl!!.queryParameter("minified"))
    }
}
