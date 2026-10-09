package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Marks made on the podcast's screen, over the progress read on open (#181). */
class EpisodeMarksTest {

    private val half = EpisodeProgress.InProgress(fraction = 0.5, secondsLeft = 600.0)

    @Test
    fun `nothing marked leaves the progress as read`() {
        val progress = mapOf("e1" to half, "e2" to EpisodeProgress.Heard)

        assertEquals(progress, EpisodeMarks.over(progress, emptyMap()))
    }

    @Test
    fun `marked finished reads as finished, whatever it was`() {
        val shown = EpisodeMarks.over(mapOf("e1" to half), mapOf("e1" to true, "e2" to true))

        assertEquals(EpisodeProgress.Heard, shown["e1"])
        assertEquals(EpisodeProgress.Heard, shown["e2"])
    }

    @Test
    fun `marked unfinished is no longer finished, and a half-heard one keeps its place`() {
        val shown = EpisodeMarks.over(mapOf("e1" to EpisodeProgress.Heard, "e2" to half), mapOf("e1" to false, "e2" to false))

        assertNull(shown["e1"])
        assertEquals(half, shown["e2"])
    }

    private fun progress(at: Double?, duration: Double?) =
        MediaProgressResponse(libraryItemId = "p", episodeId = "e1", currentTime = at, duration = duration)

    @Test
    fun `an un-finished episode stays where it was`() {
        assertEquals(120.0, EpisodeMarks.keepAt(progress(120.0, 1800.0))!!, 0.0)
    }

    @Test
    fun `not where the server would finish it again, nor at the start, nor unknown`() {
        assertNull(EpisodeMarks.keepAt(progress(1795.0, 1800.0)))
        assertNull(EpisodeMarks.keepAt(progress(0.0, 1800.0)))
        assertNull(EpisodeMarks.keepAt(progress(null, 1800.0)))
        assertNull(EpisodeMarks.keepAt(progress(120.0, null)))
        assertNull(EpisodeMarks.keepAt(null))
    }
}
