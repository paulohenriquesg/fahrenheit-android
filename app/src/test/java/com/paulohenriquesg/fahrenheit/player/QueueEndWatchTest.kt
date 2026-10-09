package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The player screen hears that the queue it shows has ended, so it can close
 * (#179) - and only then: a queue it does not show going is not its end.
 */
@RunWith(AndroidJUnit4::class)
class QueueEndWatchTest {

    private val player: ExoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
        .setMediaSourceFactory(hourLongFiles())
        .build()
    private var ended = 0

    @After
    fun tearDown() = player.release()

    private fun episode(id: String) = NowPlaying(
        itemId = "p1", title = id,
        timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/$id"))),
        mediaDuration = null, chapters = null, episodeId = id, goToPodcast = true, description = null
    )

    private fun queue(nowPlaying: NowPlaying, startAt: Double = 0.0, on: Player = player) {
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        on.setMediaItems(queue.items, queue.index, queue.positionMs)
        run(player).untilPendingCommandsAreFullyHandled()
    }

    private fun watch(itemId: String = "p1", episodeId: String? = "e1") =
        player.addListener(QueueEndWatch(player, itemId, episodeId) { ended++ })

    private fun clear() {
        player.clearMediaItems()
        run(player).untilPendingCommandsAreFullyHandled()
    }

    @Test
    fun `the queue it shows emptying is its end`() {
        queue(episode("e1"))
        watch()
        clear()
        assertEquals(1, ended)
    }

    @Test
    fun `queued after the screen opened, then emptied, is its end too`() {
        watch()
        queue(episode("e1"))
        clear()
        assertEquals(1, ended)
    }

    // Previous or Next episode: the old screen ends the old queue as the new one connects.
    @Test
    fun `another episode's queue going is not its end`() {
        queue(episode("e2"))
        watch(episodeId = "e1")
        clear()
        assertEquals(0, ended)
    }

    @Test
    fun `nothing queued is not an end`() {
        watch()
        clear()
        assertEquals(0, ended)
    }

    @Test
    fun `a queue replaced, never empty, is not an end`() {
        queue(episode("e1"))
        watch()
        queue(episode("e2"))
        assertEquals(0, ended)
    }

    // Put together as the service puts it: played to the end, QueueEnd empties the queue.
    @Test
    fun `played to the end of the queue, it hears the end once`() {
        val guarded = LeavingGuard(player) {}
        player.addListener(QueueEnd(guarded))
        queue(episode("e1"), startAt = 3595.0, on = guarded)
        watch()
        player.prepare()
        player.play()
        run(player).untilState(Player.STATE_IDLE)
        run(player).untilPendingCommandsAreFullyHandled()
        assertEquals(0, player.mediaItemCount)
        assertEquals(1, ended)
    }
}
