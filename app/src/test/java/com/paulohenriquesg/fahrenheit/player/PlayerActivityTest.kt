package com.paulohenriquesg.fahrenheit.player

import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Leaving the player for another screen: "Go to podcast" (the episode plays on,
 * as after Back, #155), and switching to another book or episode.
 */
@RunWith(AndroidJUnit4::class)
class PlayerActivityTest {

    private val screen: ActivityController<ComponentActivity> = Robolectric.buildActivity(ComponentActivity::class.java)

    @After
    fun tearDown() {
        screen.destroy()
    }

    @Test
    fun `going to the podcast opens it and closes the player`() {
        val player = screen.create().get()

        PlayerActivity.leaveForPodcast(player, "p1")

        val started = shadowOf(player).nextStartedActivity
        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("p1", started.getStringExtra("item_id"))
        assertTrue(player.isFinishing)
    }

    // About's "Play <title> instead?": this book stops before the other opens.
    @Test
    fun `switching books stops this one and opens the other, playing`() {
        val player = screen.create().get()
        val playing = TestExoPlayerBuilder(player).setMediaSourceFactory(hourLongFiles()).build()
        val book = NowPlaying("b2", "b2", TrackTimeline(listOf(TimelineTrack(1, 0.0, 600.0, "/f"))), null, null, null, false, null)
        val queue = PlaybackQueue.of(book, 0.0) { "https://abs.test$it" }!!
        playing.setMediaItems(queue.items, queue.index, queue.positionMs)

        PlayerActivity.switchTo(player, playing, "b3")

        assertEquals(0, playing.mediaItemCount)
        val started = shadowOf(player).nextStartedActivity
        assertEquals(PlayerActivity::class.java.name, started.component?.className)
        assertEquals("b3", started.getStringExtra("item_id"))
        assertTrue(started.getBooleanExtra("auto_play", false))
        assertTrue(player.isFinishing)
        playing.release()
    }

    // #108: Previous and Next episode.
    @Test
    fun `switching to another episode opens it, playing`() {
        val player = screen.create().get()
        val playing = TestExoPlayerBuilder(player).build()

        PlayerActivity.switchTo(player, playing, "p1", episodeId = "e3")

        val started = shadowOf(player).nextStartedActivity
        assertEquals("p1", started.getStringExtra("item_id"))
        assertEquals("e3", started.getStringExtra("episode_id"))
        assertTrue(started.getBooleanExtra("auto_play", false))
        playing.release()
    }
}
