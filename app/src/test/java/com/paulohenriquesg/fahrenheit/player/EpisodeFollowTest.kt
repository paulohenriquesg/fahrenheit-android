package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which episode the screen shows once the queue has moved on by itself (#108). */
class EpisodeFollowTest {
    @Test fun `the queue moved on to the next episode of the show, show that one`() =
        assertEquals("e2", EpisodeFollow.shown(QueuedFile("p1", "e2", 0.0, 1800.0), itemId = "p1", episodeId = "e1"))

    @Test fun `still on the episode shown, no change`() =
        assertEquals("e1", EpisodeFollow.shown(QueuedFile("p1", "e1", 0.0, 1800.0), itemId = "p1", episodeId = "e1"))

    @Test fun `something else entirely, or nothing, no change`() {
        assertEquals("e1", EpisodeFollow.shown(QueuedFile("b9", null, 0.0, 1800.0), itemId = "p1", episodeId = "e1"))
        assertEquals("e1", EpisodeFollow.shown(null, itemId = "p1", episodeId = "e1"))
    }

    @Test fun `a book never follows`() =
        assertEquals(null, EpisodeFollow.shown(QueuedFile("b1", null, 0.0, 1800.0), itemId = "b1", episodeId = null))
}
