package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
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

    private fun book(currentTime: Double?, duration: Double = 3600.0): LibraryItemResponse = Gson().fromJson(
        """{"id":"b1","mediaType":"book","media":{"duration":$duration,"metadata":{"title":"A Book","explicit":false}}
            ${currentTime?.let { ""","userMediaProgress":{"currentTime":$it,"isFinished":true}""" } ?: ""}}""",
        LibraryItemResponse::class.java
    )

    private fun placeToKeep(item: LibraryItemResponse) = DetailActivity.placeToKeep(item, item.userMediaProgress)

    // #207: where it was is what the store holds now, not the copy read on open.
    @Test
    fun `an un-finished book stays where the given progress says`() =
        assertEquals(2000.0, DetailActivity.placeToKeep(book(currentTime = 1000.0), MediaProgressResponse(currentTime = 2000.0))!!, 0.0)

    // Review: un-finishing a book not playing sent it back to the start.
    @Test
    fun `an un-finished book stays where it was`() =
        assertEquals(1000.0, placeToKeep(book(currentTime = 1000.0))!!, 0.0)

    @Test
    fun `so near the end that the server would finish it again, it goes back to the start`() {
        assertNull(placeToKeep(book(currentTime = 3595.0)))
        assertNull(placeToKeep(book(currentTime = 0.0)))
        assertNull(placeToKeep(book(currentTime = null)))
    }
}
