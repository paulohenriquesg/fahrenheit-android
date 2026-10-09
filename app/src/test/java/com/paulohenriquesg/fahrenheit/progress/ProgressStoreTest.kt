package com.paulohenriquesg.fahrenheit.progress

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** One place for listening progress, which every screen reads (#207). */
class ProgressStoreTest {

    private var clock = 1_000L
    private val store = ProgressStore(now = { clock })

    private fun server(itemId: String, episodeId: String? = null, at: Double = 0.0, finished: Boolean = false) =
        MediaProgressResponse(libraryItemId = itemId, episodeId = episodeId, currentTime = at, duration = 3600.0, isFinished = finished)

    @Test
    fun `an update from the player is seen by a reader`() {
        val reader = store.entries

        store.played("book", null, position = 900.0, duration = 3600.0)

        val seen = reader.value[ProgressKey("book", null)]!!
        assertEquals(900.0, seen.currentTime!!, 0.0)
        assertEquals(0.25, seen.progress!!, 0.0)
        assertEquals(false, seen.isFinished)
        assertEquals(1_000L, seen.lastUpdate)
    }

    @Test
    fun `Mark finished flips isFinished for that key only`() {
        store.replace(listOf(server("pod", "e1", at = 100.0), server("pod", "e2", at = 200.0), server("book", at = 300.0)), since = store.generation)

        store.marked("pod", "e1", ProgressMark(isFinished = true))

        assertEquals(true, store.of("pod", "e1")?.isFinished)
        assertEquals(false, store.of("pod", "e2")?.isFinished)
        assertEquals(false, store.of("book")?.isFinished)
        assertEquals(200.0, store.of("pod", "e2")?.currentTime!!, 0.0)
    }

    @Test
    fun `marking something never started makes an entry`() {
        store.marked("book", null, ProgressMark(isFinished = true))

        assertEquals(true, store.of("book")?.isFinished)
    }

    @Test
    fun `un-finishing puts the position at the start, as the server does, and a position mark puts it back`() {
        store.replace(listOf(server("book", at = 1200.0, finished = true)), since = store.generation)

        store.marked("book", null, ProgressMark(isFinished = false))
        assertEquals(false, store.of("book")?.isFinished)
        assertEquals(0.0, store.of("book")?.currentTime!!, 0.0)

        store.marked("book", null, ProgressMark(currentTime = 1200.0))
        assertEquals(1200.0, store.of("book")?.currentTime!!, 0.0)
    }

    @Test
    fun `a resume re-sync replaces stale entries, and drops ones the server no longer has`() {
        store.replace(listOf(server("book", at = 100.0), server("gone", at = 50.0)), since = store.generation)

        store.replace(listOf(server("book", at = 2000.0, finished = true)), since = store.generation)

        assertEquals(2000.0, store.of("book")?.currentTime!!, 0.0)
        assertEquals(true, store.of("book")?.isFinished)
        assertNull(store.of("gone"))
    }

    @Test
    fun `episode and book keys don't collide`() {
        store.played("item", null, position = 10.0, duration = 100.0)
        store.played("item", "ep", position = 70.0, duration = 100.0)

        assertEquals(10.0, store.of("item")?.currentTime!!, 0.0)
        assertEquals(70.0, store.of("item", "ep")?.currentTime!!, 0.0)
    }

    // A slow GET /api/me began before the player's report reached the server.
    @Test
    fun `a read answered late keeps what was written here after it began`() {
        store.replace(listOf(server("book", at = 100.0), server("other", at = 5.0)), since = store.generation)
        val began = store.generation

        store.played("book", null, position = 400.0, duration = 3600.0)
        store.replace(listOf(server("book", at = 100.0), server("other", at = 6.0)), since = began)

        assertEquals(400.0, store.of("book")?.currentTime!!, 0.0)
        assertEquals("the rest is the server's word", 6.0, store.of("other")?.currentTime!!, 0.0)
    }

    @Test
    fun `one item read from the server replaces its entry, unless written here since`() {
        store.played("book", null, position = 400.0, duration = 3600.0)

        store.read(server("book", at = 400.0, finished = true), since = store.generation)
        assertEquals(true, store.of("book")?.isFinished)

        val began = store.generation
        store.played("book", null, position = 500.0, duration = 3600.0)
        store.read(server("book", at = 400.0), since = began)
        assertEquals(500.0, store.of("book")?.currentTime!!, 0.0)
    }

    @Test
    fun `a report on a finished item un-finishes it only when the position moved`() {
        store.replace(listOf(server("book", at = 3600.0, finished = true)), since = store.generation)

        store.played("book", null, position = 3600.0, duration = 3600.0)
        assertEquals("the same place", true, store.of("book")?.isFinished)

        store.played("book", null, position = 30.0, duration = 3600.0)
        assertEquals("played again from the start", false, store.of("book")?.isFinished)
    }

    // The book page reads its item on open, progress and all.
    @Test
    fun `an item's own progress goes in under its id, and one never started adds nothing`() {
        val started = Gson().fromJson(
            """{"id":"book","mediaType":"book","media":{"metadata":{"title":"T","explicit":false}},
                "userMediaProgress":{"currentTime":120.0,"duration":3600.0,"isFinished":false}}""",
            LibraryItemResponse::class.java
        )
        val fresh = Gson().fromJson(
            """{"id":"new","mediaType":"book","media":{"metadata":{"title":"T","explicit":false}}}""",
            LibraryItemResponse::class.java
        )

        store.readItem(started, since = store.generation)
        store.readItem(fresh, since = store.generation)

        assertEquals(120.0, store.of("book")?.currentTime!!, 0.0)
        assertEquals(setOf(ProgressKey("book", null)), store.entries.value.keys)
    }

    // Review: a read still out at sign-out must not bring the last account's progress back.
    @Test
    fun `a read begun before sign-out adds nothing after it`() {
        val began = store.generation

        store.clear()
        store.replace(listOf(server("book", at = 100.0)), since = began)
        store.read(server("other", at = 5.0), since = began)

        assertEquals(emptyMap<ProgressKey, MediaProgressResponse>(), store.entries.value)
    }

    @Test
    fun `finishing puts progress at the whole, as the server does`() {
        store.played("book", null, position = 600.0, duration = 3600.0)

        store.marked("book", null, ProgressMark(isFinished = true))

        assertEquals(1.0, store.of("book")?.progress!!, 0.0)
    }

    // The server resets the position only when it un-finishes something finished.
    @Test
    fun `un-finishing something not finished leaves its place`() {
        store.played("book", null, position = 600.0, duration = 3600.0)

        store.marked("book", null, ProgressMark(isFinished = false))

        assertEquals(600.0, store.of("book")?.currentTime!!, 0.0)
    }

    // Review: Resume picks the latest lastUpdate, and the server's clock is not this device's.
    @Test
    fun `what the player wrote is the latest, even with this device's clock behind the server's`() {
        clock = 1_000L
        store.replace(listOf(server("pod", "e1", at = 10.0).copy(lastUpdate = 9_000_000L)), since = store.generation)

        store.played("pod", "e2", position = 20.0, duration = 3600.0)

        assertEquals(true, store.of("pod", "e2")?.lastUpdate!! > 9_000_000L)
    }

    // The podcast page reads GET /api/me anyway, for who the user is.
    @Test
    fun `a reply from GET api me goes in whole, and one without the list changes nothing`() {
        store.played("book", null, position = 10.0, duration = 100.0)

        store.readMe(Me(type = "user", mediaProgress = null), since = store.generation)
        assertEquals(10.0, store.of("book")?.currentTime!!, 0.0)

        store.readMe(Me(type = "user", mediaProgress = listOf(server("other", at = 5.0))), since = store.generation)
        assertEquals(setOf(ProgressKey("other", null)), store.entries.value.keys)
    }

    @Test
    fun `sign-out forgets everything`() {
        store.played("book", null, position = 10.0, duration = 100.0)

        store.clear()

        assertEquals(emptyMap<ProgressKey, MediaProgressResponse>(), store.entries.value)
    }
}
