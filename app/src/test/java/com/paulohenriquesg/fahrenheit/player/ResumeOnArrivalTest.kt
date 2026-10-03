package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** An episode moved on to starts where it was left (#108, Ruling 4), with or without a screen open. */
@RunWith(AndroidJUnit4::class)
class ResumeOnArrivalTest {
    private val player: ExoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
        .setMediaSourceFactory(hourLongFiles())
        .build()
    private val hour = TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/e")))
    private val e1 = NowPlaying("p1", "e1", hour, null, null, "e1", true, null)
    private val e2 = NowPlaying("p1", "e2", hour, null, null, "e2", true, null)
    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @After
    fun tearDown() = player.release()

    private fun queueWithNextAt(startAt: Double) {
        player.addListener(ResumeOnArrival(player))
        val items = PlaybackQueue.itemsOf(e1, resolve)!! + PlaybackQueue.itemsOf(e2, resolve, startAt = startAt)!!
        player.setMediaItems(items, 0, 3_595_000L)
        player.prepare()
        player.play()
    }

    @Test
    fun `moving on, the next episode starts where it was left`() {
        queueWithNextAt(600.0)
        run(player).untilPositionAtLeast(1, 1)
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(1, player.currentMediaItemIndex)
        assertTrue("at ${player.currentPosition}", player.currentPosition >= 600_000L)
    }

    @Test
    fun `an episode never started starts at the beginning`() {
        queueWithNextAt(0.0)
        run(player).untilPositionAtLeast(1, 1)

        assertTrue("at ${player.currentPosition}", player.currentPosition < 60_000L)
    }
}
