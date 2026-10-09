package com.paulohenriquesg.fahrenheit.favourites

import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/** One library's Favourites, for a screen with many hearts (#180, frame 2). */
class LibraryFavouritesTest {

    private val server = FakePlaylists()

    private fun library(choice: MemoryChoice = MemoryChoice("lib_1" to "pl_1")) =
        LibraryFavourites(Favourites(server, choice), "lib_1")

    @Test
    fun `with None chosen there is nothing, and no episodes`() = runBlocking {
        val library = library(MemoryChoice()).apply { load() }

        assertNull(library.playlist)
        assertNull(library.episodesOf("li_1"))
    }

    @Test
    fun `a show's episodes in the playlist, and only that show's`() = runBlocking {
        server.add(
            "pl_1", "Favourites", "lib_1",
            PlaylistItem("li_1", "ep_1"), PlaylistItem("li_2", "ep_9"), PlaylistItem("li_1", "ep_3"), PlaylistItem("li_1", null)
        )
        val library = library().apply { load() }

        assertEquals(setOf("ep_1", "ep_3"), library.episodesOf("li_1"))
        assertEquals(emptySet<String>(), library.episodesOf("li_5"))
    }

    @Test
    fun `episodes are added and taken out one after another`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val library = library().apply { load() }

        assertEquals(HeartChange.Added("Bedtime"), library.toggle("li_1", "ep_2"))
        assertEquals(HeartChange.Removed("Bedtime"), library.toggle("li_1", "ep_1"))

        assertEquals(setOf("ep_2"), library.episodesOf("li_1"))
    }

    @Test
    fun `a failure says so and leaves the playlist as it was`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val library = library().apply { load() }
        server.failing = IOException("offline")

        assertEquals(HeartChange.Failed, library.toggle("li_1", "ep_2"))
        assertEquals(setOf("ep_1"), library.episodesOf("li_1"))
    }
}
