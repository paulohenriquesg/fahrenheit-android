package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.Shelf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

/** Home fetches its shelves again, once per burst of reasons, and never for the worse (#197). */
class HomeReloadTest {

    private val old = listOf(shelf("continue-listening"))
    private val fresh = listOf(shelf("continue-listening"), shelf("recently-added"))

    /** Each send closes one window. */
    private val windows = Channel<Unit>(Channel.UNLIMITED)

    private fun closeWindow() = windows.trySend(Unit)

    private fun reload(
        fetched: MutableList<String>,
        shown: MutableList<List<Shelf>>,
        library: () -> String? = { "lib" },
        answer: suspend (String) -> List<Shelf>? = { fresh }
    ) = HomeReload(
        scope = CoroutineScope(Dispatchers.Unconfined),
        fetch = { id -> fetched += id; answer(id) },
        library = library,
        show = { shown += it },
        window = { windows.receive() }
    )

    @Test
    fun `a request fetches the shelves after the window and shows them`() {
        val fetched = mutableListOf<String>()
        val shown = mutableListOf<List<Shelf>>()
        val reload = reload(fetched, shown)

        reload.request()
        assertEquals("not before the window closes", emptyList<String>(), fetched)

        closeWindow()
        assertEquals(listOf("lib"), fetched)
        assertEquals(listOf(fresh), shown)
    }

    @Test
    fun `two requests inside the window make one fetch`() {
        val fetched = mutableListOf<String>()
        val reload = reload(fetched, mutableListOf())

        reload.request()
        reload.request()
        closeWindow()
        closeWindow()

        assertEquals(1, fetched.size)
    }

    @Test
    fun `a request after the fetch began makes another`() {
        val fetched = mutableListOf<String>()
        val network = CompletableDeferred<Unit>()
        val reload = reload(fetched, mutableListOf(), answer = { network.await(); fresh })

        reload.request()
        closeWindow()
        reload.request()
        closeWindow()
        network.complete(Unit)

        assertEquals(2, fetched.size)
    }

    @Test
    fun `a failed fetch shows nothing new`() {
        val shown = mutableListOf<List<Shelf>>()
        val reload = reload(mutableListOf(), shown, answer = { null })

        reload.request()
        closeWindow()

        assertEquals(emptyList<List<Shelf>>(), shown)
    }

    @Test
    fun `shelves for a library no longer current are dropped`() {
        val shown = mutableListOf<List<Shelf>>()
        var current = "lib"
        val reload = reload(mutableListOf(), shown, library = { current }, answer = { current = "other"; old })

        reload.request()
        closeWindow()

        assertEquals(emptyList<List<Shelf>>(), shown)
    }

    @Test
    fun `with no library there is nothing to fetch`() {
        val fetched = mutableListOf<String>()
        val reload = reload(fetched, mutableListOf(), library = { null })

        reload.request()
        closeWindow()

        assertEquals(emptyList<String>(), fetched)
    }

    // Review: Library, the player, Back to Home - the news came while Home was hidden.
    @Test
    fun `a request while Home is hidden is made when Home shows again`() {
        val fetched = mutableListOf<String>()
        val reload = reload(fetched, mutableListOf())
        reload.showing = false

        reload.request()
        closeWindow()
        assertEquals("not while hidden", emptyList<String>(), fetched)

        reload.showing = true
        closeWindow()
        assertEquals(listOf("lib"), fetched)
    }

    @Test
    fun `showing Home again with nothing owed fetches nothing`() {
        val fetched = mutableListOf<String>()
        val reload = reload(fetched, mutableListOf())

        reload.showing = false
        reload.showing = true
        closeWindow()

        assertEquals(emptyList<String>(), fetched)
    }

    // Review: a slow older fetch must not put back shelves from before the news.
    @Test
    fun `an older fetch answering last does not replace a newer one`() {
        val shown = mutableListOf<List<Shelf>>()
        val first = CompletableDeferred<Unit>()
        var calls = 0
        val reload = reload(mutableListOf(), shown, answer = {
            if (++calls == 1) { first.await(); old } else fresh
        })

        reload.request()
        closeWindow()
        reload.request()
        closeWindow()
        first.complete(Unit)

        assertEquals(listOf(fresh), shown)
    }

    private fun shelf(id: String) = Shelf(id = id, label = id, labelStringKey = id, type = "book", bookEntities = emptyList())
}
