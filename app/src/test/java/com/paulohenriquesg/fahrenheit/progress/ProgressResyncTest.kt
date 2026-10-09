package com.paulohenriquesg.fahrenheit.progress

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

/** Coming back to the app reads the server's progress into the store (#207). */
class ProgressResyncTest {

    private val store = ProgressStore(now = { 0L })
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun server(itemId: String, at: Double) = MediaProgressResponse(libraryItemId = itemId, currentTime = at, duration = 100.0)

    @Test
    fun `a resync fills the store with what the server holds`() {
        ProgressResync(store, scope) { listOf(server("book", 40.0)) }.request()

        assertEquals(40.0, store.of("book")?.currentTime!!, 0.0)
    }

    @Test
    fun `one read at a time - asking again while one is out reads nothing more`() {
        var reads = 0
        val answer = CompletableDeferred<List<MediaProgressResponse>>()
        val resync = ProgressResync(store, scope) { reads++; answer.await() }

        resync.request()
        resync.request()
        answer.complete(listOf(server("book", 40.0)))

        assertEquals(1, reads)
        assertEquals(40.0, store.of("book")?.currentTime!!, 0.0)
    }

    @Test
    fun `asking after the read came back reads again`() {
        var reads = 0
        val resync = ProgressResync(store, scope) { reads++; emptyList() }

        resync.request()
        resync.request()

        assertEquals(2, reads)
    }

    @Test
    fun `a failed read keeps what the store holds`() {
        store.played("book", null, position = 30.0, duration = 100.0)

        ProgressResync(store, scope) { error("offline") }.request()

        assertEquals(30.0, store.of("book")?.currentTime!!, 0.0)
    }

    @Test
    fun `signed out, there is nothing to read and nothing changes`() {
        store.played("book", null, position = 30.0, duration = 100.0)

        ProgressResync(store, scope) { null }.request()

        assertEquals(30.0, store.of("book")?.currentTime!!, 0.0)
    }
}
