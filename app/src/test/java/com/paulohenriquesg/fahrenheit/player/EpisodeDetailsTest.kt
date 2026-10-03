package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What an episode says about itself under its title (#108; frame "C, playing an episode"). */
class EpisodeDetailsTest {

    @Test fun `a bonus or a trailer is badged, a regular episode is not`() {
        assertEquals("Bonus", EpisodeDetails.badge("bonus"))
        assertEquals("Trailer", EpisodeDetails.badge("trailer"))
        assertNull(EpisodeDetails.badge("full"))
        assertNull(EpisodeDetails.badge(null))
    }

    @Test fun `season, number and length`() =
        assertEquals("Season 2 · Episode 295 · 30 min 0 s", EpisodeDetails.line(season = "2", episode = "295", length = 1800.0))

    @Test fun `a missing part is left out`() {
        assertEquals("Episode 295 · 30 min 0 s", EpisodeDetails.line(season = " ", episode = "295", length = 1800.0))
        assertEquals("30 min 0 s", EpisodeDetails.line(season = null, episode = null, length = 1800.0))
        assertNull(EpisodeDetails.line(season = null, episode = null, length = null))
    }

    @Test fun `the notes are the feed's subtitle where there is one`() =
        assertEquals("A short subtitle", EpisodeDetails.notes(subtitle = "A short subtitle", description = "<p>The long notes</p>"))

    @Test fun `otherwise the start of the description, as plain text`() =
        assertEquals(
            "Dale has written a book & the town reads it. Plus the weather.",
            EpisodeDetails.notes(subtitle = " ", description = "<p>Dale has <b>written</b> a book &amp; the town reads it.</p><p>Plus the weather.</p>")
        )

    @Test fun `no subtitle and no description, no notes`() =
        assertNull(EpisodeDetails.notes(subtitle = null, description = "<p> </p>"))
}
