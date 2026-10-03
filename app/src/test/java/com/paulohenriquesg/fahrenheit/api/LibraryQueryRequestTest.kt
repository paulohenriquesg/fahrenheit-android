package com.paulohenriquesg.fahrenheit.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * What the library screen asks the server for, opened as a Home shelf's "See
 * all" (#146). The server reads `desc` as "1" or not, and a filter as
 * group.value with the value base64- then URL-encoded.
 */
@RunWith(AndroidJUnit4::class)
class LibraryQueryRequestTest {
    private val server = MockWebServer().apply { start() }
    private val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        .create(LibraryApi::class.java)

    @After
    fun tearDown() = server.shutdown()

    private fun request(query: LibraryQuery): RecordedRequest = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[],"total":0}"""))
        LibraryRepository(api).items("l1", query).getOrThrow()
        server.takeRequest(5, TimeUnit.SECONDS)!!
    }

    @Test
    fun `the library as it always opened, by title, unfiltered`() {
        val sent = request(LibraryQuery.Everything)

        assertEquals("media.metadata.title", sent.requestUrl!!.queryParameter("sort"))
        assertNull(sent.requestUrl!!.queryParameter("desc"))
        assertNull(sent.requestUrl!!.queryParameter("filter"))
    }

    @Test
    fun `recently added is newest first by date added`() {
        val sent = request(LibraryQuery.RecentlyAdded)

        assertEquals("addedAt", sent.requestUrl!!.queryParameter("sort"))
        assertEquals("1", sent.requestUrl!!.queryParameter("desc"))
        assertNull(sent.requestUrl!!.queryParameter("filter"))
    }

    @Test
    fun `in progress is filtered to started, most recently played first`() {
        val sent = request(LibraryQuery.InProgress)

        assertTrue(sent.path!!, sent.path!!.contains("filter=progress.aW4tcHJvZ3Jlc3M%3D"))
        assertEquals("progress", sent.requestUrl!!.queryParameter("sort"))
        assertEquals("1", sent.requestUrl!!.queryParameter("desc"))
    }

    @Test
    fun `finished is filtered to finished, most recently played first`() {
        val sent = request(LibraryQuery.Finished)

        assertTrue(sent.path!!, sent.path!!.contains("filter=progress.ZmluaXNoZWQ%3D"))
        assertEquals("progress", sent.requestUrl!!.queryParameter("sort"))
        assertEquals("1", sent.requestUrl!!.queryParameter("desc"))
    }

    // The existing series request (#107) keeps its exact filter.
    @Test
    fun `the series filter is unchanged`() =
        assertEquals("series.czE%3D", seriesFilter("s1"))
}
