package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonParser
import com.paulohenriquesg.fahrenheit.api.LibraryApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Mark finished, as the server will keep it (#107).
 *
 * The server un-finishes an item on any later update that moves its
 * currentTime, and a playing book keeps reporting. So the mark pauses first
 * and carries the paused position: the closing report then moves nothing.
 */
@RunWith(AndroidJUnit4::class)
class FinishMarkTest {
    private val server = MockWebServer().apply { start() }
    private val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        .create(LibraryApi::class.java)
    private val player: ExoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
        .setMediaSourceFactory(hourLongFiles())
        .build()
    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )
    private val book = NowPlaying("b1", "b1", twoParts, null, null, null, false, null)

    @After
    fun tearDown() {
        player.release()
        server.shutdown()
    }

    private fun next() = server.takeRequest(5, TimeUnit.SECONDS) ?: throw AssertionError("expected a request")

    // Review Focus 1.
    @Test
    fun `marking finished pauses first, and sends the paused position`() = runBlocking {
        val queue = PlaybackQueue.of(book, 3602.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        player.play()
        run(player).untilPositionAtLeast(5_000)
        server.enqueue(MockResponse().setBody("OK"))

        val result = FinishMark(api).finished(player, BookPlayback(player, twoParts), itemId = "b1", episodeId = null)

        assertTrue(result.isSuccess)
        assertFalse(player.playWhenReady)
        val request = next()
        assertEquals("PATCH", request.method)
        assertEquals("/api/me/progress/b1", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertTrue(body.get("isFinished").asBoolean)
        assertEquals(BookPlayback(player, twoParts).bookPosition(), body.get("currentTime").asDouble, 0.0)
    }

    @Test
    fun `an episode is marked under its podcast`() = runBlocking {
        server.enqueue(MockResponse().setBody("OK"))
        val episode = NowPlaying("p1", "e1", twoParts, null, null, "e1", true, null)
        val queue = PlaybackQueue.of(episode, 0.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)

        FinishMark(api).finished(player, BookPlayback(player, twoParts), itemId = "p1", episodeId = "e1")

        assertEquals("/api/me/progress/p1/e1", next().path)
    }

    @Test
    fun `unfinished says only that, and leaves the player alone`() = runBlocking {
        server.enqueue(MockResponse().setBody("OK"))
        player.playWhenReady = true

        FinishMark(api).unfinished(itemId = "b1", episodeId = null)

        assertEquals("""{"isFinished":false}""", next().body.readUtf8())
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `a failed mark is a failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(FinishMark(api).unfinished(itemId = "b1", episodeId = null).isFailure)
    }
}
