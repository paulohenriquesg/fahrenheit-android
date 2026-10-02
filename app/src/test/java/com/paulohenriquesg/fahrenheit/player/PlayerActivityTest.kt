package com.paulohenriquesg.fahrenheit.player

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
 * "Go to podcast" leaves the episode: the player closes, and a closing player
 * stops playback. Opened from the podcast's own screen this already happened;
 * opened from anywhere else the episode played on behind the podcast.
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
}
