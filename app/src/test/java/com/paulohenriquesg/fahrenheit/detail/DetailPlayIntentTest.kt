package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Play on a book's screen, and an episode picked on a podcast's screen, used
 * to open the player paused - a second press of Play to hear anything (#122).
 * Home's podcast shelves already asked the player to start; these now do too.
 *
 * Only the intent is checked: building the real PlayerActivity here would
 * leave state behind that breaks later Compose tests.
 */
@RunWith(RobolectricTestRunner::class)
class DetailPlayIntentTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a book's Play starts playing`() {
        val intent = DetailActivity.playBook(context, "b1")

        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("b1", intent.getStringExtra("item_id"))
        assertTrue(intent.getBooleanExtra("auto_play", false))
    }

    @Test
    fun `an episode chosen on a podcast's screen starts playing`() {
        val intent = DetailActivity.playEpisode(context, "p1", "e1")

        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("p1", intent.getStringExtra("item_id"))
        assertEquals("e1", intent.getStringExtra("episode_id"))
        assertTrue(intent.getBooleanExtra("auto_play", false))
    }
}
