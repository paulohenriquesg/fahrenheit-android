package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Heard and half-heard episodes (#78). Progress is in no part of the item or
 * recent-episodes responses; GET /api/me carries it for everything the user
 * has touched.
 */
class EpisodeProgressTest {

    private fun progress(
        episode: String,
        podcast: String = "p",
        currentTime: Double = 0.0,
        duration: Double = 1500.0,
        finished: Boolean = false,
        lastUpdate: Long = 1
    ) = MediaProgressResponse(
        libraryItemId = podcast, episodeId = episode, currentTime = currentTime, duration = duration,
        progress = currentTime / duration, isFinished = finished, lastUpdate = lastUpdate
    )

    @Test
    fun `a finished episode is heard`() {
        val index = EpisodeProgress.index(listOf(progress("e1", finished = true)), podcastId = "p")

        assertEquals(EpisodeProgress.Heard, index["e1"])
    }

    @Test
    fun `a half-heard episode says how far in and how long is left`() {
        val index = EpisodeProgress.index(listOf(progress("e1", currentTime = 541.0, duration = 1563.0)), podcastId = "p")

        val p = index["e1"] as EpisodeProgress.InProgress
        assertEquals(541.0 / 1563.0, p.fraction, 0.0001)
        assertEquals(1022.0, p.secondsLeft, 0.0001)
    }

    @Test
    fun `an episode opened but not played is neither`() {
        assertNull(EpisodeProgress.index(listOf(progress("e1", currentTime = 0.0)), podcastId = "p")["e1"])
    }

    @Test
    fun `other podcasts, and books, are left out`() {
        val index = EpisodeProgress.index(
            listOf(
                progress("e1", podcast = "other", finished = true),
                MediaProgressResponse(libraryItemId = "p", episodeId = null, currentTime = 10.0, isFinished = false)
            ),
            podcastId = "p"
        )

        assertEquals(emptyMap<String, EpisodeProgress>(), index)
    }

    @Test
    fun `the episode to resume is the one played most recently`() {
        val list = listOf(
            progress("old", currentTime = 100.0, lastUpdate = 1),
            progress("recent", currentTime = 100.0, lastUpdate = 9),
            progress("heard", finished = true, lastUpdate = 20)
        )

        assertEquals("recent", EpisodeProgress.resumable(list, podcastId = "p", onServer = setOf("old", "recent", "heard")))
    }

    @Test
    fun `an episode no longer on the server cannot be resumed`() {
        val list = listOf(progress("gone", currentTime = 100.0, lastUpdate = 9))

        assertNull(EpisodeProgress.resumable(list, podcastId = "p", onServer = emptySet()))
    }

    @Test
    fun `the server's me response carries the progress`() {
        // Trimmed from GET /api/me on 2.36.0.
        val me = Gson().fromJson(
            """{"id":"u","type":"root","mediaProgress":[{"libraryItemId":"57b7","episodeId":"0fdc","progress":0.346,
                "currentTime":541.4,"duration":1563.6,"isFinished":false,"lastUpdate":1730544260596}]}""",
            Me::class.java
        )

        assertEquals("root", me.type)
        assertEquals("0fdc", me.mediaProgress?.single()?.episodeId)
    }
}
