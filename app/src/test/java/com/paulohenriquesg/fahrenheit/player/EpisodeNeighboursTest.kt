package com.paulohenriquesg.fahrenheit.player

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Previous and next episode: by when they came out, among those the server can play (#108). */
class EpisodeNeighboursTest {
    private fun episode(id: String, publishedAt: Long, index: Int = 1, audio: Boolean = true, duration: Double = 1800.0): Episode =
        Gson().fromJson(
            """{"libraryItemId":"p1","id":"$id","index":$index,"title":"Episode $id","publishedAt":$publishedAt,"addedAt":0,"updatedAt":0
               ${if (audio) ""","audioTrack":{"index":1,"startOffset":0.0,"duration":$duration,"title":"t","contentUrl":"/f/$id","mimeType":"audio/mpeg","codec":"mp3",
                 "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}""" else ""}}""",
            Episode::class.java
        )

    private val shuffled = listOf(episode("e3", 3_000), episode("e1", 1_000), episode("e2", 2_000))

    @Test fun `previous is older and next is newer`() {
        val around = EpisodeNeighbours.of(shuffled, currentId = "e2")
        assertEquals("e1", around.previous!!.id)
        assertEquals("e3", around.next!!.id)
    }

    @Test fun `the newest has no next, and the oldest no previous`() {
        assertNull(EpisodeNeighbours.of(shuffled, currentId = "e3").next)
        assertNull(EpisodeNeighbours.of(shuffled, currentId = "e1").previous)
    }

    // Review Focus 1.
    @Test fun `episodes the server has no audio for are passed over`() {
        val withGap = shuffled + episode("e2b", 2_500, audio = false)
        assertEquals("e3", EpisodeNeighbours.of(withGap, currentId = "e2").next!!.id)
    }

    // Review Focus 2.
    @Test fun `episodes out at the same moment go by the server's order`() {
        val twins = listOf(episode("a", 1_000, index = 1), episode("b", 1_000, index = 2))
        assertEquals("b", EpisodeNeighbours.of(twins, currentId = "a").next!!.id)
        assertEquals("a", EpisodeNeighbours.of(twins, currentId = "b").previous!!.id)
    }

    @Test fun `a neighbour carries its title and length, for Up next`() =
        assertEquals(EpisodeRef("e3", "Episode e3", 1800.0), EpisodeNeighbours.of(shuffled, currentId = "e2").next)

    @Test fun `an episode the list does not have has no neighbours`() =
        assertEquals(EpisodeNeighbours.Around(null, null), EpisodeNeighbours.of(shuffled, currentId = "e9"))
}
