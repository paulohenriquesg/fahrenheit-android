package com.paulohenriquesg.fahrenheit.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.LibraryStats
import com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The main screen's shell, away from Compose (#208): which library is open,
 * which view shows, the rail's highlight and the browse views' lists. The
 * fakes answer at once unless gated, and the scope is unconfined, so each
 * event has settled by the time it returns.
 */
@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun stop() = scope.cancel()

    private val books = TestFixtures.createMockLibrary(id = "books", name = "Books")
    private val podcasts = TestFixtures.createMockLibrary(id = "podcasts", name = "Podcasts")

    private var saved: String? = null
    private val homeOpened = mutableListOf<String>()
    private val listFetched = mutableListOf<Pair<String, LibraryQuery>>()
    private var librariesAnswer: CompletableDeferred<Result<List<Library>>?> =
        CompletableDeferred(Result.success(listOf(books, podcasts)))
    private var seriesAnswer: Result<List<Series>>? = Result.success(emptyList())
    private var collectionsAnswer: Result<List<Collection>>? = Result.success(emptyList())
    private var statsAnswer: Result<ListeningStatsResponse>? = Result.success(listeningStats())
    private val libraryStatsAnswers = mutableMapOf<String, Result<LibraryStats>>()
    private val libraryStatsAsked = mutableListOf<String>()

    private fun model() = MainViewModel(
        libraries = { librariesAnswer.await() },
        fetchLibraryItems = { id, query -> listFetched += id to query; emptyList<LibraryItem>() },
        openHome = { homeOpened += it },
        savedLibrary = { saved },
        saveLibrary = { saved = it },
        libraryStats = { id -> libraryStatsAsked += id; libraryStatsAnswers[id] },
        series = { seriesAnswer },
        collections = { collectionsAnswer },
        listeningStats = { statsAnswer },
        scope = scope
    )

    private fun listeningStats() = ListeningStatsResponse(
        totalTime = 60.0, items = emptyMap(), days = emptyMap(), dayOfWeek = emptyMap(), today = 0.0
    )

    private fun series(name: String) = Series(id = name, name = name)

    private fun collection(name: String) = Collection(id = name, libraryId = "books", name = name, lastUpdate = 0, createdAt = 0)

    // Initial load

    @Test
    fun `the library chosen last time opens on start, Home and the Library list both`() {
        saved = "podcasts"

        val state = model().uiState.value

        assertEquals(listOf(books, podcasts), state.libraries)
        assertEquals(podcasts, state.library)
        assertEquals(listOf("podcasts"), homeOpened)
        assertEquals(listOf("podcasts" to LibraryQuery.Everything), listFetched)
        assertEquals(MainView.HOME, state.view)
        assertEquals(MainView.HOME.menuItemId, state.highlighted)
    }

    @Test
    fun `a saved library no longer on the server falls back to the first, which is saved`() {
        saved = "deleted"

        assertEquals(books, model().uiState.value.library)
        assertEquals("books", saved)
        assertEquals(listOf("books"), homeOpened)
    }

    @Test
    fun `nothing opens until the libraries arrive`() {
        librariesAnswer = CompletableDeferred()
        val model = model()

        assertNull(model.uiState.value.library)
        assertTrue(homeOpened.isEmpty())

        librariesAnswer.complete(Result.success(listOf(books)))
        assertEquals(books, model.uiState.value.library)
    }

    @Test
    fun `a failed libraries load opens nothing and says why, once`() {
        librariesAnswer = CompletableDeferred(Result.failure(RuntimeException("timeout")))
        val model = model()
        val said = mutableListOf<String>()
        scope.launch { model.librariesFailed.collect { said += it } }

        assertNull(model.uiState.value.library)
        assertTrue(homeOpened.isEmpty())
        assertEquals(listOf("timeout"), said)
    }

    @Test
    fun `with no server to ask, nothing opens and nothing is said`() {
        librariesAnswer = CompletableDeferred(null)
        val model = model()
        val said = mutableListOf<String>()
        scope.launch { model.librariesFailed.collect { said += it } }

        assertNull(model.uiState.value.library)
        assertTrue(said.isEmpty())
    }

    // Library switch

    @Test
    fun `following a newly saved library opens it on Home and drops the old list`() {
        val model = model()
        model.select(MainView.LIBRARY.menuItemId, MenuAction.LIBRARY, LibraryQuery.Finished)
        homeOpened.clear()
        listFetched.clear()

        saved = "podcasts"
        val switched = model.followSavedLibrary()

        val state = model.uiState.value
        assertTrue(switched)
        assertEquals(podcasts, state.library)
        assertEquals(MainView.HOME, state.view)
        assertEquals(MainView.HOME.menuItemId, state.highlighted)
        assertEquals(LibraryQuery.Everything, state.listing.query)
        assertEquals(listOf("podcasts"), homeOpened)
        assertEquals(listOf("podcasts" to LibraryQuery.Everything), listFetched)
    }

    @Test
    fun `following the library already open does nothing`() {
        val model = model()
        model.select(MainView.STATS.menuItemId, MenuAction.STATS)
        homeOpened.clear()

        assertFalse(model.followSavedLibrary())
        assertEquals(MainView.STATS, model.uiState.value.view)
        assertTrue(homeOpened.isEmpty())
    }

    @Test
    fun `a library chosen in Switch Library is saved and opened`() {
        val model = model()
        model.select(MainView.SWITCH_LIBRARY.menuItemId, MenuAction.SELECT_LIBRARY)

        model.chooseLibrary(podcasts)

        assertEquals("podcasts", saved)
        assertEquals(podcasts, model.uiState.value.library)
        assertEquals(MainView.HOME, model.uiState.value.view)
    }

    @Test
    fun `a switch saved before the libraries arrive is applied when they do`() {
        librariesAnswer = CompletableDeferred()
        saved = "books"
        val model = model()

        saved = "podcasts"
        assertTrue(model.followSavedLibrary())
        librariesAnswer.complete(Result.success(listOf(books, podcasts)))

        assertEquals(podcasts, model.uiState.value.library)
        assertEquals(listOf("podcasts"), homeOpened)
    }

    // The browse views

    @Test
    fun `Series loads sorted, after showing nothing and loading`() {
        val gate = CompletableDeferred<Result<List<Series>>?>()
        val model = MainViewModel(
            libraries = { Result.success(listOf(books)) },
            fetchLibraryItems = { _, _ -> emptyList() },
            openHome = {},
            savedLibrary = { null },
            saveLibrary = {},
            series = { gate.await() },
            scope = scope
        )

        model.select(MainView.SERIES.menuItemId, MenuAction.SERIES)
        assertEquals(MainView.SERIES, model.uiState.value.view)
        assertTrue(model.uiState.value.seriesLoading)
        assertTrue(model.uiState.value.series.isEmpty())

        gate.complete(Result.success(listOf(series("Zebra"), series("Apple"))))
        assertFalse(model.uiState.value.seriesLoading)
        assertEquals(listOf("Apple", "Zebra"), model.uiState.value.series.map { it.name })
    }

    @Test
    fun `a failed Series load stops loading and shows none, and a second visit clears the first`() {
        val model = model()
        seriesAnswer = Result.success(listOf(series("Apple")))
        model.select(MainView.SERIES.menuItemId, MenuAction.SERIES)

        seriesAnswer = Result.failure(RuntimeException())
        model.select(MainView.SERIES.menuItemId, MenuAction.SERIES)

        assertFalse(model.uiState.value.seriesLoading)
        assertTrue(model.uiState.value.series.isEmpty())
    }

    @Test
    fun `Collections loads sorted, and a failure stops loading`() {
        val model = model()
        collectionsAnswer = Result.success(listOf(collection("b"), collection("A")))
        model.select(MainView.COLLECTIONS.menuItemId, MenuAction.COLLECTIONS)
        assertEquals(listOf("A", "b"), model.uiState.value.collections.map { it.name })
        assertFalse(model.uiState.value.collectionsLoading)

        collectionsAnswer = Result.failure(RuntimeException())
        model.select(MainView.COLLECTIONS.menuItemId, MenuAction.COLLECTIONS)
        assertTrue(model.uiState.value.collections.isEmpty())
        assertFalse(model.uiState.value.collectionsLoading)
    }

    @Test
    fun `Stats loads the listening stats, and a failure leaves none`() {
        val model = model()
        model.select(MainView.STATS.menuItemId, MenuAction.STATS)
        assertEquals(listeningStats(), model.uiState.value.listeningStats)
        assertFalse(model.uiState.value.statsLoading)

        statsAnswer = Result.failure(RuntimeException())
        model.select(MainView.STATS.menuItemId, MenuAction.STATS)
        assertNull(model.uiState.value.listeningStats)
        assertFalse(model.uiState.value.statsLoading)
    }

    @Test
    fun `with no library open, Series and Collections load nothing`() {
        librariesAnswer = CompletableDeferred(Result.success(emptyList()))
        val model = model()
        seriesAnswer = Result.success(listOf(series("Apple")))

        model.select(MainView.SERIES.menuItemId, MenuAction.SERIES)

        assertTrue(model.uiState.value.series.isEmpty())
        assertFalse(model.uiState.value.seriesLoading)
    }

    @Test
    fun `Switch Library reads each library's counts as it opens, leaving out a failed one`() {
        libraryStatsAnswers["books"] = Result.success(LibraryStats(totalItems = 3, totalDuration = 1.0, numAudioTracks = 3))
        libraryStatsAnswers["podcasts"] = Result.failure(RuntimeException())
        val model = model()
        assertTrue("not before it opens", libraryStatsAsked.isEmpty())

        model.select(MainView.SWITCH_LIBRARY.menuItemId, MenuAction.SELECT_LIBRARY)

        assertEquals(listOf("books", "podcasts"), libraryStatsAsked)
        assertEquals(setOf("books"), model.uiState.value.libraryStats.keys)
    }

    @Test
    fun `the Library view opens the list for the query asked`() {
        val model = model()
        listFetched.clear()

        model.select(MainView.LIBRARY.menuItemId, MenuAction.LIBRARY, LibraryQuery.InProgress)

        assertEquals(MainView.LIBRARY, model.uiState.value.view)
        assertEquals(LibraryQuery.InProgress, model.uiState.value.listing.query)
        assertEquals(listOf("books" to LibraryQuery.InProgress), listFetched)
    }

    // The rail's highlight

    @Test
    fun `a row that opens no view takes the highlight and leaves the view`() {
        val model = model()

        model.select("narrators", MenuAction.NARRATORS)

        assertEquals("narrators", model.uiState.value.highlighted)
        assertEquals(MainView.HOME, model.uiState.value.view)
    }

    @Test
    fun `See all highlights the screen its tile opens`() {
        val model = model()

        model.seeAll(MenuAction.LIBRARY, LibraryQuery.Finished)

        assertEquals(MainView.LIBRARY, model.uiState.value.view)
        assertEquals(MainView.LIBRARY.menuItemId, model.uiState.value.highlighted)
        assertEquals(LibraryQuery.Finished, model.uiState.value.listing.query)
    }

    @Test
    fun `going Home from a section highlights Home`() {
        val model = model()
        model.select(MainView.STATS.menuItemId, MenuAction.STATS)

        model.goHome()

        assertEquals(MainView.HOME, model.uiState.value.view)
        assertEquals(MainView.HOME.menuItemId, model.uiState.value.highlighted)
    }
}
