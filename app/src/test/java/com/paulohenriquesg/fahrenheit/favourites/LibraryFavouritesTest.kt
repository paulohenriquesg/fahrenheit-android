package com.paulohenriquesg.fahrenheit.favourites

import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.api.PlaylistApi
import com.paulohenriquesg.fahrenheit.api.PlaylistItems
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

    /** The server, holding a read or an add until let go. */
    private class Held(private val inner: FakePlaylists) : PlaylistApi by inner {
        var read: CompletableDeferred<Unit>? = null
        var add: CompletableDeferred<Unit>? = null
        override suspend fun playlist(id: String): Playlist {
            val answer = inner.playlist(id)
            read?.await()
            return answer
        }
        override suspend fun addItems(id: String, body: PlaylistItems): Playlist {
            add?.await()
            return inner.addItems(id, body)
        }
    }

    @Test
    fun `a read that fails keeps the playlist as it was`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val library = library().apply { load() }
        server.failing = IOException("offline")

        library.load()

        assertEquals(setOf("ep_1"), library.episodesOf("li_1"))
    }

    @Test
    fun `a press on another row while one is on its way does nothing`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val held = Held(server)
        val library = LibraryFavourites(Favourites(held, MemoryChoice("lib_1" to "pl_1")), "lib_1").apply { load() }
        held.add = CompletableDeferred()

        val first = async(start = CoroutineStart.UNDISPATCHED) { library.toggle("li_1", "ep_2") }
        assertNull(library.toggle("li_1", "ep_3"))
        held.add!!.complete(Unit)

        assertEquals(HeartChange.Added("Bedtime"), first.await())
        assertEquals(setOf("ep_1", "ep_2"), library.episodesOf("li_1"))
    }

    @Test
    fun `a read answered after a press does not undo it`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val held = Held(server)
        val library = LibraryFavourites(Favourites(held, MemoryChoice("lib_1" to "pl_1")), "lib_1").apply { load() }
        val gate = CompletableDeferred<Unit>()
        held.read = gate

        // The read has the playlist from before the press, and answers after it.
        val reading = launch(start = CoroutineStart.UNDISPATCHED) { library.load() }
        held.read = null
        library.toggle("li_1", "ep_2")
        gate.complete(Unit)
        reading.join()

        assertEquals(setOf("ep_1", "ep_2"), library.episodesOf("li_1"))
    }

    @Test
    fun `with the last episode taken out the playlist is empty, not gone`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("li_1", "ep_1"))
        val library = library().apply { load() }

        library.toggle("li_1", "ep_1")

        assertEquals(emptySet<String>(), library.episodesOf("li_1"))
    }
}
