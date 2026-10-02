package com.paulohenriquesg.fahrenheit.podcast

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * #75: "No Episodes" read as "this podcast is empty", when what had happened
 * was that the server had not looked at the feed in 705 days.
 */
class EmptyPodcastTest {

    private val day = 24 * 60 * 60 * 1000L
    // 2026-10-02 12:00 UTC
    private val now = 1_790_942_400_000L

    @Test
    fun `it says nothing is downloaded, when the feed was last checked, and that nothing will check it`() {
        val lines = EmptyPodcast.lines(
            lastEpisodeCheck = now - 705 * day,
            autoDownload = false,
            now = now,
            serverFormat = "yyyy-MM-dd"
        )

        assertEquals(
            listOf(
                "Nothing downloaded yet",
                "Feed last checked: 2024-10-27",
                "Automatic downloads are off for this podcast"
            ),
            lines
        )
    }

    @Test
    fun `a feed the server never checked says so`() {
        val lines = EmptyPodcast.lines(lastEpisodeCheck = null, autoDownload = false, now = now)

        assertEquals("Feed never checked", lines[1])
    }

    @Test
    fun `zero is never, not 1970`() {
        val lines = EmptyPodcast.lines(lastEpisodeCheck = 0, autoDownload = false, now = now)

        assertEquals("Feed never checked", lines[1])
    }

    @Test
    fun `a recent check reads as recent`() {
        val lines = EmptyPodcast.lines(lastEpisodeCheck = now - day / 2, autoDownload = true, now = now)

        assertEquals("Feed last checked: Today", lines[1])
    }

    @Test
    fun `with automatic downloads on, it says new episodes will come`() {
        val lines = EmptyPodcast.lines(lastEpisodeCheck = now - day / 2, autoDownload = true, now = now)

        assertEquals("New episodes download automatically", lines[2])
    }

    @Test
    fun `an unknown setting is not guessed at`() {
        val lines = EmptyPodcast.lines(lastEpisodeCheck = null, autoDownload = null, now = now)

        assertEquals(listOf("Nothing downloaded yet", "Feed never checked"), lines)
    }
}
