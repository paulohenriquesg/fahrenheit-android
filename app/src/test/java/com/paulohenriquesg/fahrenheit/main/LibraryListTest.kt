package com.paulohenriquesg.fahrenheit.main

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The library list Home's "See all" tiles and the rail open (#146): only the
 * latest request's answer is shown, under its own label. Each fetch here
 * waits on a gate, so a test decides which answer arrives first.
 */
class LibraryListTest {

    private fun item(id: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"T"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private val gates = LibraryQuery.entries.associateWith { CompletableDeferred<List<LibraryItem>>() }
    private val list = LibraryList(CoroutineScope(Dispatchers.Unconfined)) { _, query -> gates.getValue(query).await() }

    @Test
    fun `a slow earlier answer does not replace a later one`() {
        list.open("lib", LibraryQuery.InProgress)
        list.open("lib", LibraryQuery.Finished)

        gates.getValue(LibraryQuery.Finished).complete(listOf(item("finished")))
        gates.getValue(LibraryQuery.InProgress).complete(listOf(item("started")))

        assertEquals(LibraryQuery.Finished, list.query)
        assertEquals(listOf("finished"), list.items.map { it.id })
    }

    // Startup fetches the whole library, the slowest list; a tile pressed
    // meanwhile must win.
    @Test
    fun `the whole library arriving late does not land under a tile's view`() {
        list.open("lib", LibraryQuery.Everything)
        list.open("lib", LibraryQuery.InProgress)

        gates.getValue(LibraryQuery.InProgress).complete(listOf(item("started")))
        gates.getValue(LibraryQuery.Everything).complete(listOf(item("a"), item("b")))

        assertEquals(LibraryQuery.InProgress, list.query)
        assertEquals(listOf("started"), list.items.map { it.id })
    }

    @Test
    fun `while a new view loads it shows nothing of the old one and says it is loading`() {
        list.open("lib", LibraryQuery.Everything)
        gates.getValue(LibraryQuery.Everything).complete(listOf(item("a")))

        list.open("lib", LibraryQuery.Finished)

        assertTrue(list.loading)
        assertTrue(list.items.isEmpty())
        gates.getValue(LibraryQuery.Finished).complete(listOf(item("done")))
        assertFalse(list.loading)
    }

    @Test
    fun `reopening the same view keeps its list on screen while it refreshes`() {
        list.open("lib", LibraryQuery.Everything)
        gates.getValue(LibraryQuery.Everything).complete(listOf(item("a")))

        list.open("lib", LibraryQuery.Everything)

        assertEquals(listOf("a"), list.items.map { it.id })
    }

    @Test
    fun `switching library forgets the view and its list`() {
        list.open("lib", LibraryQuery.Finished)
        gates.getValue(LibraryQuery.Finished).complete(listOf(item("done")))

        list.clear()

        assertEquals(LibraryQuery.Everything, list.query)
        assertTrue(list.items.isEmpty())
        assertFalse(list.loading)
    }
}
