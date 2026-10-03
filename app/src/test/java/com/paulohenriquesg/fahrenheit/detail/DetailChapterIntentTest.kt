package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A chapter chosen on the details screen opens the player there, playing (#105). */
@RunWith(AndroidJUnit4::class)
class DetailChapterIntentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a chapter opens the player at its start, playing`() {
        val intent = DetailActivity.playChapterIntent(context, "b1", start = 1800.0)

        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("b1", intent.getStringExtra("item_id"))
        assertTrue(intent.getBooleanExtra("auto_play", false))
        assertEquals(1800.0, PlayerActivity.startAtOf(intent)!!, 0.0)
    }
}
