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

    // #223 entry points 3 and 6: Now playing reattaches, and hands over its title.
    @Test
    fun `Now playing reopens the player on what is queued, with its title`() {
        val entry = RailEntry("p1", "e1", "An Invented Episode", playing = true, progress = 0.5f, chapter = null, leftSeconds = 60.0)

        val intent = PlayerActivity.reopenIntent(context, entry)

        assertEquals("p1", intent.getStringExtra("item_id"))
        assertEquals("e1", intent.getStringExtra("episode_id"))
        assertFalse(intent.getBooleanExtra("auto_play", true))
        assertEquals("An Invented Episode", PlayerActivity.previewOf(intent)?.title)
    }

    // #223 entry point 9: the notification's Play knows only what the session holds.
    @Test
    fun `a Play from outside hands over the queued title, or nothing`() {
        val file = QueuedFile("p1", "e1", startOffset = 0.0, bookTotal = 600.0)

        val titled = PlayerActivity.outsidePlayIntent(context, file, "An Invented Episode")
        val untitled = PlayerActivity.outsidePlayIntent(context, file, null)

        assertTrue(PlayerActivity.asksThenPlays(titled))
        assertEquals("p1", titled.getStringExtra("item_id"))
        assertEquals("e1", titled.getStringExtra("episode_id"))
        assertTrue((titled.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertEquals("An Invented Episode", PlayerActivity.previewOf(titled)?.title)
        assertNull(PlayerActivity.previewOf(untitled))
    }

    // #144: opened by a Play from outside, to ask and then play.
    @Test
    fun `a Play from outside travels with the intent`() {
        assertTrue(PlayerActivity.asksThenPlays(PlayerActivity.createIntent(context, "b1", askThenPlay = true)))
        assertFalse(PlayerActivity.asksThenPlays(PlayerActivity.createIntent(context, "b1")))
    }
}
