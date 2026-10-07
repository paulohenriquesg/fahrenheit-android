package com.paulohenriquesg.fahrenheit.favourites

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/** Home asks for the chosen playlist with its shelves (#180). */
class FavouritesOnHomeTest {

    private val server = FakePlaylists()
    private val home = listOf(Shelf("continue-listening", "Continue Listening", "", "book"))
    private val book = Gson().fromJson("""{"id":"b1","media":{"metadata":{"title":"A Book"}}}""", LibraryItem::class.java)

    @Test
    fun `the chosen playlist joins Home's shelves`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("b1", libraryItem = book))

        val shelves = FavouritesShelf.onto(home, "lib_1", Favourites(server, MemoryChoice("lib_1" to "pl_1")))

        assertEquals(listOf("continue-listening", FavouritesShelf.ID), shelves.map { it.id })
    }

    @Test
    fun `with None, or offline, Home is as the server sent it`() = runBlocking {
        server.add("pl_1", "Bedtime", "lib_1", PlaylistItem("b1", libraryItem = book))

        assertEquals(home, FavouritesShelf.onto(home, "lib_1", Favourites(server, MemoryChoice())))
        server.failing = IOException("offline")
        assertEquals(home, FavouritesShelf.onto(home, "lib_1", Favourites(server, MemoryChoice("lib_1" to "pl_1"))))
        assertEquals(home, FavouritesShelf.onto(home, "lib_1", null))
    }
}
