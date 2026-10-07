package com.paulohenriquesg.fahrenheit.favourites

import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** The player's heart (#180; frame 1 of docs/mocks/podcast-actions.html). */
class FavouriteHeartTest {

    private val server = FakePlaylists()

    private fun heart(choice: MemoryChoice = MemoryChoice("lib_1" to "pl_1"), episodeId: String? = "ep_1") =
        FavouriteHeart(Favourites(server, choice), "lib_1", "li_1", episodeId)

    @Test
    fun `with None chosen there is no heart`() = runBlocking {
        val heart = heart(MemoryChoice())

        heart.load()

        assertNull(heart.playlist)
    }

    @Test
    fun `filled when the episode is in the playlist`() = runBlocking {
        server.add("pl_1", "Favourites", "lib_1", PlaylistItem("li_1", "ep_1"))
        val heart = heart()

        heart.load()

        assertTrue(heart.filled)
    }

    @Test
    fun `pressed, it adds and fills, and says where`() = runBlocking {
        server.add("pl_1", "Bedtime")
        val heart = heart().apply { load() }

        assertEquals(HeartChange.Added("Bedtime"), heart.toggle())
        assertTrue(heart.filled)
    }

    @Test
    fun `pressed again, it takes out and empties`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"), PlaylistItem("li_1", "ep_2"))
        val heart = heart().apply { load() }

        assertEquals(HeartChange.Removed("Bedtime"), heart.toggle())
        assertFalse(heart.filled)
    }

    @Test
    fun `a book has its heart too`() = runBlocking {
        server.add("pl_1", "Bedtime")
        val heart = heart(episodeId = null).apply { load() }

        heart.toggle()

        assertTrue(server.playlists.single().holds("li_1", null))
        assertTrue(heart.filled)
    }

    @Test
    fun `a failure says so and leaves the heart as it was`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val heart = heart().apply { load() }
        server.failing = IOException("offline")

        assertEquals(HeartChange.Failed, heart.toggle())
        assertTrue(heart.filled)
    }
}
