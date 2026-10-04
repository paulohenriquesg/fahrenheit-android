package com.paulohenriquesg.fahrenheit.screensaver

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The now-playing line's time left, in whole minutes (#172). */
@RunWith(RobolectricTestRunner::class)
class NowPlayingDetailTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    private fun detail(chapter: String? = null, number: Int? = null, episode: Boolean = false, left: Double) =
        nowPlayingDetail(resources, chapter, number, episode, left)

    @Test
    fun `a book says its chapter and the minutes left in it`() =
        assertEquals("Opening · 18 min left in chapter", detail(chapter = "Opening", number = 1, left = 18 * 60.0))

    @Test
    fun `a chapter without a title is called by its number`() =
        assertEquals("Chapter 12 · 18 min left in chapter", detail(number = 12, left = 18 * 60.0))

    @Test
    fun `an episode says the minutes left`() =
        assertEquals("25 min left", detail(episode = true, left = 25 * 60.0))

    @Test
    fun `a book without chapters says the minutes left`() =
        assertEquals("25 min left", detail(left = 25 * 60.0))

    @Test
    fun `minutes are whole, rounded up, so a part minute still counts`() {
        assertEquals("5 min left", detail(episode = true, left = 4 * 60.0 + 59))
        assertEquals("1 min left", detail(episode = true, left = 20.0))
        assertEquals("1 min left", detail(episode = true, left = 0.0))
    }

    @Test
    fun `past an hour, hours and minutes`() {
        assertEquals("1 h 5 min left", detail(episode = true, left = 65 * 60.0))
        assertEquals("2 h left", detail(episode = true, left = 120 * 60.0))
    }
}
