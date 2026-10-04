package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the player knows of the server's state is kept as it happens: when it
 * queues an item from the server's position, and when a report reaches the
 * server (#90).
 */
@RunWith(AndroidJUnit4::class)
class ResumeKnowledgeTest {

    private lateinit var player: ExoPlayer
    private val knowledge = ServerKnowledge()

    private val oneFile = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/b1")))
    private fun book(id: String) = NowPlaying(id, id, oneFile, null, null, null, false, null)
    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
    }

    @After
    fun tearDown() = player.release()

    @Test
    fun `queuing from the server's position knows that position's time`() {
        runBlocking {
            PlayerStart(autoPlay = false, knowledge = knowledge)
                .begin(player, book("b1"), { MediaProgressResponse(currentTime = 900.0, lastUpdate = 5000L) }, resolve)
        }

        assertEquals(KnownProgress.ServerCopy(5000L), knowledge.known("b1", null))
    }

    // A chapter chosen on the details screen starts where it was asked, not
    // from the server's position: still, the player knows the server as of now.
    @Test
    fun `a chosen start counts as known, at this device's time`() {
        runBlocking {
            PlayerStart(autoPlay = false, startAt = 600.0, knowledge = knowledge, now = { 42L })
                .begin(player, book("b1"), { error("not read") }, resolve)
        }

        assertEquals(KnownProgress.Since(42L), knowledge.known("b1", null))
    }

    @Test
    fun `coming back with a chosen start says so, so nothing is asked over it`() {
        runBlocking { PlayerStart(autoPlay = false, knowledge = ServerKnowledge()).begin(player, book("b1"), { null }, resolve) }
        val start = PlayerStart(autoPlay = false, startAt = 600.0, knowledge = ServerKnowledge())

        runBlocking { start.begin(player, book("b1"), { null }, resolve) }

        assertTrue(start.choseStart)
    }

    @Test
    fun `a delivered report is known by the position it wrote`() {
        val written = mutableListOf<Double>()
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            open = { object : ListeningDelivery {
                override suspend fun sync(report: ListeningReport) { written += report.currentTime }
                override suspend fun close(report: ListeningReport?) { report?.let { written += it.currentTime } }
            } },
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() },
            delivered = { file, position -> knowledge.wrote(file.itemId, file.episodeId, position) }
        )
        player.addListener(reporting)
        // From 15:00: a report from the very start is never sent.
        runBlocking {
            PlayerStart(autoPlay = true, knowledge = ServerKnowledge())
                .begin(player, book("b1"), { MediaProgressResponse(currentTime = 900.0) }, resolve)
        }
        run(player).untilPositionAtLeast(902_000)
        player.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        // Exactly what the server was sent last.
        assertEquals(KnownProgress.Wrote(written.last()), knowledge.known("b1", null))
    }

    @Test
    fun `a report that fails to arrive is not known`() {
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            open = { object : ListeningDelivery {
                override suspend fun sync(report: ListeningReport) = error("offline")
                override suspend fun close(report: ListeningReport?) = error("offline")
            } },
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() },
            delivered = { file, position -> knowledge.wrote(file.itemId, file.episodeId, position) }
        )
        player.addListener(reporting)
        // From 15:00: a report from the very start is never sent.
        runBlocking {
            PlayerStart(autoPlay = true, knowledge = ServerKnowledge())
                .begin(player, book("b1"), { MediaProgressResponse(currentTime = 900.0) }, resolve)
        }
        run(player).untilPositionAtLeast(902_000)
        player.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        assertTrue(knowledge.known("b1", null) == null)
    }
}
