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

        assertEquals(5000L, knowledge.knownAt("b1", null))
    }

    @Test
    fun `a delivered report is known, at the time it was delivered`() {
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            open = { object : ListeningDelivery {
                override suspend fun sync(report: ListeningReport) {}
                override suspend fun close(report: ListeningReport?) {}
            } },
            pause = { awaitCancellation() },
            now = { player.clock.elapsedRealtime() },
            delivered = { file -> knowledge.saw(file.itemId, file.episodeId, 7000L) }
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

        assertEquals(7000L, knowledge.knownAt("b1", null))
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
            delivered = { file -> knowledge.saw(file.itemId, file.episodeId, 7000L) }
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

        assertTrue(knowledge.knownAt("b1", null) == null)
    }
}
