package com.paulohenriquesg.fahrenheit.favourites

import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** The rules of Favourites, an Audiobookshelf playlist chosen per library (#180). */
class FavouritesTest {

    private val server = FakePlaylists()

    private fun episode(id: String, show: String = "li_1") = PlaylistItem(show, id)
    private fun book(id: String) = PlaylistItem(id)

    // --- the choice ---

    @Test
    fun `with None chosen there is no playlist, and nothing is asked of the server`() = runBlocking {
        val favourites = Favourites(server, MemoryChoice())

        assertNull(favourites.current("lib_1").getOrThrow())
        assertEquals(emptyList<String>(), server.calls)
    }

    @Test
    fun `the chosen playlist is read from the server`() = runBlocking {
        server.add("pl_1", "Favourites", "lib_1", episode("ep_1"))
        val favourites = Favourites(server, MemoryChoice("lib_1" to "pl_1"))

        assertEquals("pl_1", favourites.current("lib_1").getOrThrow()?.id)
    }

    @Test
    fun `a chosen playlist gone from the server is None again`() = runBlocking {
        val choice = MemoryChoice("lib_1" to "pl_gone")
        val favourites = Favourites(server, choice)

        assertNull(favourites.current("lib_1").getOrThrow())
        assertNull(choice.playlistFor("lib_1"))
    }

    @Test
    fun `offline, the choice is kept and the read fails`() = runBlocking {
        server.add("pl_1", "Favourites")
        server.failing = IOException("offline")
        val choice = MemoryChoice("lib_1" to "pl_1")

        assertTrue(Favourites(server, choice).current("lib_1").isFailure)
        assertEquals("pl_1", choice.playlistFor("lib_1"))
    }

    @Test
    fun `choosing a playlist keeps it for that library only`() = runBlocking {
        val playlist = server.add("pl_1", "Bedtime")
        val choice = MemoryChoice("lib_2" to "pl_9")

        Favourites(server, choice).choose("lib_1", FavouritesPick.Existing(playlist)).getOrThrow()

        assertEquals("pl_1", choice.playlistFor("lib_1"))
        assertEquals("pl_9", choice.playlistFor("lib_2"))
    }

    @Test
    fun `choosing None forgets the library's playlist`() = runBlocking {
        val choice = MemoryChoice("lib_1" to "pl_1")

        assertNull(Favourites(server, choice).choose("lib_1", FavouritesPick.None).getOrThrow())
        assertNull(choice.playlistFor("lib_1"))
    }

    @Test
    fun `Create makes an empty Favourites playlist in the library and chooses it`() = runBlocking {
        val choice = MemoryChoice()

        val made = Favourites(server, choice).choose("lib_1", FavouritesPick.Create).getOrThrow()!!

        assertEquals("Favourites", made.name)
        assertEquals("lib_1", made.libraryId)
        assertEquals(made.id, choice.playlistFor("lib_1"))
    }

    @Test
    fun `a Create that fails changes nothing`() = runBlocking {
        server.failing = IOException("offline")
        val choice = MemoryChoice("lib_1" to "pl_1")

        assertTrue(Favourites(server, choice).choose("lib_1", FavouritesPick.Create).isFailure)
        assertEquals("pl_1", choice.playlistFor("lib_1"))
    }

    // --- the panel's list ---

    @Test
    fun `the library's playlists come by name`() = runBlocking {
        server.add("pl_1", "kids")
        server.add("pl_2", "Bedtime")
        server.add("pl_3", "Elsewhere", "lib_2")

        assertEquals(listOf("Bedtime", "kids"), Favourites(server, MemoryChoice()).playlists("lib_1").getOrThrow().map { it.name })
    }

    @Test
    fun `Create is offered unless a playlist is already called Favourites`() {
        val bedtime = server.add("pl_1", "Bedtime")
        val favourites = server.add("pl_2", "favourites")

        assertTrue(Favourites.offersCreate(listOf(bedtime)))
        assertFalse(Favourites.offersCreate(listOf(bedtime, favourites)))
    }

    // --- the heart ---

    @Test
    fun `an episode is in the playlist by its episode, a book by its item`() {
        val playlist = server.add("pl_1", "Favourites", "lib_1", episode("ep_1"), book("b_1"))

        assertTrue(playlist.holds("li_1", "ep_1"))
        assertFalse(playlist.holds("li_1", "ep_2"))
        assertTrue(playlist.holds("b_1", null))
        assertFalse(playlist.holds("li_1", null))
    }

    @Test
    fun `the heart adds what is not in`() = runBlocking {
        val playlist = server.add("pl_1", "Favourites", "lib_1", episode("ep_1"))

        val after = Favourites(server, MemoryChoice("lib_1" to "pl_1")).toggle(playlist, "li_1", "ep_2").getOrThrow()

        assertTrue(after.holds("li_1", "ep_2"))
        assertTrue(after.holds("li_1", "ep_1"))
    }

    @Test
    fun `the heart takes out what is in`() = runBlocking {
        val playlist = server.add("pl_1", "Favourites", "lib_1", episode("ep_1"), episode("ep_2"))

        val after = Favourites(server, MemoryChoice("lib_1" to "pl_1")).toggle(playlist, "li_1", "ep_2").getOrThrow()

        assertFalse(after.holds("li_1", "ep_2"))
        assertTrue(after.holds("li_1", "ep_1"))
    }

    @Test
    fun `a heart that fails reports it`() = runBlocking {
        val playlist = server.add("pl_1", "Favourites", "lib_1", episode("ep_1"))
        server.failing = IOException("offline")

        assertTrue(Favourites(server, MemoryChoice("lib_1" to "pl_1")).toggle(playlist, "li_1", "ep_1").isFailure)
    }

    // The server deletes a playlist when its last item goes; taking out the
    // last favourite must not quietly turn Favourites off.
    @Test
    fun `taking out the last one keeps an empty playlist of that name chosen`() = runBlocking {
        val playlist = server.add("pl_1", "Bedtime", "lib_1", book("b_1"))
        val choice = MemoryChoice("lib_1" to "pl_1")

        val after = Favourites(server, choice).toggle(playlist, "b_1", null).getOrThrow()

        assertEquals("Bedtime", after.name)
        assertEquals(emptyList<PlaylistItem>(), after.items.orEmpty())
        assertEquals(after.id, choice.playlistFor("lib_1"))
        assertEquals(listOf(after.id), server.playlists.map { it.id })
    }
}
