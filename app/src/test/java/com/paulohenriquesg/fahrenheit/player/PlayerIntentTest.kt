package com.paulohenriquesg.fahrenheit.player

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerIntentTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `a book opens the one player, with no episode`() {
        val intent = PlayerActivity.createIntent(context, "b1")

        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("b1", intent.getStringExtra("item_id"))
        assertNull(intent.getStringExtra("episode_id"))
        assertFalse(intent.getBooleanExtra("auto_play", true))
    }

    @Test
    fun `an episode opens the same player, on that episode`() {
        val intent = PlayerActivity.createIntent(context, "p1", episodeId = "e1", autoPlay = true)

        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("p1", intent.getStringExtra("item_id"))
        assertEquals("e1", intent.getStringExtra("episode_id"))
        assertTrue(intent.getBooleanExtra("auto_play", false))
    }

    @Test
    fun `going to the podcast returns to its screen rather than stacking another`() {
        val intent = PlayerActivity.podcastIntent(context, "p1")

        assertEquals(DetailActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    // #105: a chapter chosen on the details screen.
    @Test
    fun `a start position travels with the intent`() {
        assertEquals(3900.0, PlayerActivity.startAtOf(PlayerActivity.createIntent(context, "b1", autoPlay = true, startAt = 3900.0))!!, 0.0)
        assertNull(PlayerActivity.startAtOf(PlayerActivity.createIntent(context, "b1")))
    }
}
