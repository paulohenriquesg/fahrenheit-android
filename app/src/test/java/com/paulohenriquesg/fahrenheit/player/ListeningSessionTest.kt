package com.paulohenriquesg.fahrenheit.player

import com.google.gson.JsonParser
import com.paulohenriquesg.fahrenheit.api.ApiService
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Listening reported the way Audiobookshelf's own clients do: a session (#92). */
class ListeningSessionTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ApiService
    private val device = PlayLibraryItemDeviceInfo("Fire Stick", "Fahrenheit", "test", "Amazon", "AFT", 25)
    private val report = ListeningReport(currentTime = 3605.0, duration = 7632.9, timeListened = 5.0)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun session(episodeId: String? = null) =
        ListeningSession(QueuedFile("b1", episodeId, 0.0, 7632.9), { api }, device)

    private fun ok(body: String = "") = MockResponse().setResponseCode(200).setBody(body)
    private fun opened(id: String) = ok("""{"id":"$id"}""")
    /** The next request; a request that never comes fails the test instead of hanging it. */
    private fun next() = server.takeRequest(5, TimeUnit.SECONDS) ?: throw AssertionError("expected another request")

    private fun json(body: String) = JsonParser.parseString(body).asJsonObject

    @Test
    fun `a book's session opens with direct play, then syncs`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        session().sync(report)
        val open = next()
        assertEquals("/api/items/b1/play", open.path)
        assertTrue(json(open.body.readUtf8()).get("forceDirectPlay").asBoolean)
        val sync = next()
        assertEquals("/api/session/s1/sync", sync.path)
        val body = json(sync.body.readUtf8())
        assertEquals(3605.0, body.get("currentTime").asDouble, 1e-9)
        assertEquals(5.0, body.get("timeListened").asDouble, 1e-9)
        assertEquals(7632.9, body.get("duration").asDouble, 1e-9)
    }

    @Test
    fun `an episode's session opens on the episode`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        session(episodeId = "e1").sync(report)
        assertEquals("/api/items/b1/play/e1", next().path)
    }

    @Test
    fun `later reports use the same session`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok()); server.enqueue(ok())
        val s = session()
        s.sync(report); s.sync(report)
        assertEquals(listOf("/api/items/b1/play", "/api/session/s1/sync", "/api/session/s1/sync"), List(3) { next().path })
    }

    // Review Focus 2.
    @Test
    fun `a session the server forgot is reopened once, and the report resent`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(opened("s2")); server.enqueue(ok())
        val s = session()
        s.sync(report); s.sync(report)
        assertEquals(
            listOf("/api/items/b1/play", "/api/session/s1/sync", "/api/session/s1/sync", "/api/items/b1/play", "/api/session/s2/sync"),
            List(5) { next().path }
        )
    }

    // Review Focus 4.
    @Test
    fun `when no session can be opened, the position is still saved`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500)); server.enqueue(ok())
        session().sync(report)
        next()
        val patch = next()
        assertEquals("PATCH", patch.method)
        assertEquals("/api/me/progress/b1", patch.path)
        assertEquals(3605.0, json(patch.body.readUtf8()).get("currentTime").asDouble, 1e-9)
    }

    @Test
    fun `a rejected sync is not delivered`() {
        server.enqueue(opened("s1")); server.enqueue(MockResponse().setResponseCode(500))
        val failed = runCatching { runBlocking { session().sync(report) } }
        assertTrue(failed.isFailure)
    }

    @Test
    fun `closing sends the last report and ends the session`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok()); server.enqueue(ok())
        server.enqueue(opened("s2")); server.enqueue(ok())
        val s = session()
        s.sync(report); s.close(report); s.sync(report)
        val paths = List(5) { next().path }
        assertEquals("/api/session/s1/close", paths[2])
        assertEquals("/api/items/b1/play", paths[3])
    }

    @Test
    fun `closing with nothing new ends the session without moving the position`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok()); server.enqueue(ok())
        val s = session()
        s.sync(report); s.close(null)
        next(); next()
        val close = next()
        assertEquals("/api/session/s1/close", close.path)
        assertEquals("{}", close.body.readUtf8())
    }

    // Review Focus 5.
    @Test
    fun `closing a session never opened opens it to close it`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        session().close(report)
        assertEquals(listOf("/api/items/b1/play", "/api/session/s1/close"), List(2) { next().path })
    }

    @Test
    fun `closing a session never opened, with nothing to say, sends nothing`() = runBlocking {
        session().close(null)
        assertEquals(0, server.requestCount)
    }
}
