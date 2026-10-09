package com.paulohenriquesg.fahrenheit.main

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.favourites.FavouritesShelf
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Home's shelves, held by its ViewModel (#208): loaded on open, and again on
 * a return and when the store says listening started, stopped or finished
 * (#197) - without ever making Home worse.
 */
@RunWith(AndroidJUnit4::class)
class HomeShelvesModelTest {

    private val store = ProgressStore(now = { 0L })
    private val fresh = listOf(shelf("continue-listening"), shelf("recently-added"))
    private val older = listOf(shelf("recently-added"))

    /** Each send closes one coalescing window. */
    private val windows = Channel<Unit>(Channel.UNLIMITED)
    private val fetched = mutableListOf<String>()
    private var answer: suspend (String) -> List<Shelf>? = { fresh }
    private var said = 0

    private val model = HomeViewModel(
        store,
        fetchShelves = { id -> fetched += id; answer(id) },
        window = { windows.receive() }
    )
    private val listening = CoroutineScope(Dispatchers.Unconfined).also { scope ->
        scope.launch { model.failures.collect { said++ } }
    }

    @After
    fun tearDown() = listening.cancel()

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    private fun closeWindow() {
        windows.trySend(Unit)
        settle()
    }

    private fun opened(shelves: List<Shelf> = fresh) {
        answer = { shelves }
        model.open("lib")
        settle()
        fetched.clear()
    }

    @Test
    fun `opening a library loads its shelves, saying it is loading meanwhile`() {
        val network = CompletableDeferred<Unit>()
        answer = { network.await(); fresh }

        model.open("lib")
        settle()
        assertEquals(true, model.uiState.value.loading)

        network.complete(Unit)
        settle()
        assertEquals(fresh, model.uiState.value.shelves)
        assertEquals(false, model.uiState.value.loading)
    }

    @Test
    fun `an open that fails says so, with nothing to show`() {
        answer = { null }

        model.open("lib")
        settle()

        assertEquals(emptyList<Shelf>(), model.uiState.value.shelves)
        assertEquals(1, said)
    }

    @Test
    fun `switching library drops the last one's shelves at once`() {
        opened()
        answer = { CompletableDeferred<List<Shelf>?>().await() }

        model.open("other")
        settle()

        assertEquals(emptyList<Shelf>(), model.uiState.value.shelves)
        assertEquals(true, model.uiState.value.loading)
    }

    @Test
    fun `listening that starts reloads the shelves, after the window`() {
        opened(older)
        answer = { fresh }

        store.played("book", null, position = 10.0, duration = 100.0)
        settle()
        assertEquals("not before the window closes", emptyList<String>(), fetched)

        closeWindow()
        assertEquals(listOf("lib"), fetched)
        assertEquals(fresh, model.uiState.value.shelves)
    }

    @Test
    fun `a stop and a finish reload too`() {
        opened()

        store.stopped("book", null, progress = null, since = store.generation)
        closeWindow()
        store.marked("book", null, ProgressMark(isFinished = true))
        closeWindow()

        assertEquals(listOf("lib", "lib"), fetched)
    }

    @Test
    fun `news while Home is hidden is owed until it shows`() {
        opened()
        model.visible(false)

        store.played("book", null, position = 10.0, duration = 100.0)
        closeWindow()
        assertEquals("not while hidden", emptyList<String>(), fetched)

        model.visible(true)
        closeWindow()
        assertEquals(listOf("lib"), fetched)
    }

    @Test
    fun `coming back to Home reloads, once with the news that came with it`() {
        opened()

        model.returned()
        store.stopped("book", null, progress = null, since = store.generation)
        closeWindow()
        closeWindow()

        assertEquals(listOf("lib"), fetched)
    }

    @Test
    fun `a reload in the background that fails keeps the shelves and says nothing`() {
        opened()
        answer = { null }

        model.returned()
        closeWindow()

        assertEquals(fresh, model.uiState.value.shelves)
        assertEquals(0, said)
    }

    @Test
    fun `choosing Home with nothing shown says a failure`() {
        answer = { null }
        model.open("lib")
        settle()

        model.choseHome()
        closeWindow()

        assertEquals(2, said)
    }

    @Test
    fun `the Favourites choice changed refreshes that shelf only`() {
        opened(listOf(shelf("continue-listening"), shelf(FavouritesShelf.ID), shelf("recently-added")))

        model.refreshFavourites(null)
        settle()

        assertEquals(listOf("continue-listening", "recently-added"), model.uiState.value.shelves.map { it.id })
        assertEquals(emptyList<String>(), fetched)
    }

    private fun shelf(id: String) = Shelf(id = id, label = id, labelStringKey = id, type = "book", bookEntities = emptyList())
}
