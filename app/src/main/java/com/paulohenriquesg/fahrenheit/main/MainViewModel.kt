package com.paulohenriquesg.fahrenheit.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.LibraryStats
import com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.utils.Alphabetical
import com.paulohenriquesg.fahrenheit.utils.StartupTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the main screen's shell shows, as one value (#208); Home's shelves are
 * [HomeUiState]'s.
 *
 * @property libraries the server's libraries, in its display order.
 * @property libraryStats counts for the Switch Library tiles, by library id.
 * @property library the library open.
 * @property view what the screen shows.
 * @property highlighted the rail row highlighted and focused when the rail is
 *   reopened. Kept apart from [view] because rows like Settings open another
 *   screen and still take the highlight; changing that is a focus change for a
 *   TV, not a refactor.
 * @property listing the Library view's list.
 */
data class MainUiState(
    val libraries: List<Library> = emptyList(),
    val libraryStats: Map<String, LibraryStats> = emptyMap(),
    val library: Library? = null,
    val view: MainView = MainView.HOME,
    val highlighted: String = MainView.HOME.menuItemId,
    val listing: LibraryList.Listing = LibraryList.Listing(),
    val series: List<Series> = emptyList(),
    val seriesLoading: Boolean = false,
    val collections: List<Collection> = emptyList(),
    val collectionsLoading: Boolean = false,
    val listeningStats: ListeningStatsResponse? = null,
    val statsLoading: Boolean = false
)

/**
 * The main screen's shell, out of the composable (#208): which library is
 * open, which view shows and which rail row is highlighted, and the browse
 * views' lists. Home's own shelves are [HomeViewModel]'s; opening a library
 * opens them through [openHome].
 *
 * Every read returns null when there is no server to ask (signed out), and is
 * then skipped.
 *
 * @param openHome Home's shelves for a library, when one is opened.
 * @param savedLibrary the library chosen last, kept across launches.
 * @param scope where the work runs; the ViewModel's own unless a test gives one.
 */
class MainViewModel(
    private val libraries: suspend () -> Result<List<Library>>?,
    fetchLibraryItems: suspend (String, LibraryQuery) -> List<LibraryItem>,
    private val openHome: (String) -> Unit,
    private val savedLibrary: () -> String?,
    private val saveLibrary: (String) -> Unit,
    private val libraryStats: suspend (String) -> Result<LibraryStats>? = { null },
    private val series: suspend (String) -> Result<List<Series>>? = { null },
    private val collections: suspend (String) -> Result<List<Collection>>? = { null },
    private val listeningStats: suspend () -> Result<ListeningStatsResponse>? = { null },
    scope: CoroutineScope? = null
) : ViewModel() {

    private val scope = scope ?: viewModelScope
    private val state = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = state.asStateFlow()

    private val said = Channel<String>(Channel.BUFFERED)

    /** The libraries could not be read: why, to be said once. */
    val librariesFailed: Flow<String> = said.receiveAsFlow()

    // The whole library from the rail, or the view a Home shelf's "See all" stands for (#146).
    private val list = LibraryList(this.scope, changed = { listing -> state.update { it.copy(listing = listing) } }, fetch = fetchLibraryItems)

    // A library saved elsewhere (Switch Library, or another screen), to open once the libraries are known.
    private var switchOwed = false
    private var counts: Job? = null

    init {
        this.scope.launch { load() }
    }

    private suspend fun load() {
        val read = libraries() ?: return
        read.onSuccess { fetched ->
            StartupTimeline.app.mark("libraries loaded")
            state.update { it.copy(libraries = fetched) }
            LibraryChoice.pick(fetched, savedLibrary())?.let { chosen ->
                state.update { it.copy(library = chosen) }
                chosen.id?.let { libraryId ->
                    saveLibrary(libraryId)
                    open(libraryId)
                }
            }
            settleSwitch()
            if (state.value.view == MainView.SWITCH_LIBRARY) readCounts()
        }.onFailure { said.trySend(it.message ?: "network error") }
    }

    /**
     * Back on the screen: a library saved since it was opened is opened, once
     * the libraries are known. Whether one was, so the caller knows Home is
     * being loaded anyway.
     */
    fun followSavedLibrary(): Boolean {
        val savedId = savedLibrary()
        if (savedId == null || savedId == state.value.library?.id) return false
        switchOwed = true
        settleSwitch()
        return true
    }

    /**
     * Chosen in Switch Library: saved, then followed. Setting the current one
     * here first made them equal, and the shelves were never fetched again.
     */
    fun chooseLibrary(chosen: Library) {
        chosen.id?.let(saveLibrary)
        switchOwed = true
        settleSwitch()
    }

    private fun settleSwitch() {
        val known = state.value.libraries
        if (!switchOwed || known.isEmpty()) return
        switchOwed = false
        val chosen = known.find { it.id == savedLibrary() } ?: return
        if (chosen.id == state.value.library?.id) return
        state.update { it.copy(library = chosen, highlighted = MainView.HOME.menuItemId) }
        show(MainView.HOME)
        // Drop what belongs to the library being left: Home showed its
        // shelves for the second or two the fetch took.
        list.clear()
        chosen.id?.let(::open)
    }

    private fun open(libraryId: String) {
        openHome(libraryId)
        list.open(libraryId, LibraryQuery.Everything)
    }

    /** A rail row chosen: it takes the highlight, and its view shows. */
    fun select(itemId: String, action: MenuAction, query: LibraryQuery = LibraryQuery.Everything) {
        state.update { it.copy(highlighted = itemId) }
        act(action, query)
    }

    /** A Home shelf's "See all": as though the rail's row had been chosen, so the rail highlights the screen it opened. */
    fun seeAll(action: MenuAction, query: LibraryQuery) {
        MainView.forMenuAction(action)?.let { opened -> state.update { it.copy(highlighted = opened.menuItemId) } }
        act(action, query)
    }

    /** Back from a section's rail (#125). */
    fun goHome() {
        state.update { it.copy(highlighted = MainView.HOME.menuItemId) }
        show(MainView.HOME)
    }

    private fun act(action: MenuAction, query: LibraryQuery) {
        MainView.forMenuAction(action)?.let(::show)
        val libraryId = state.value.library?.id
        when (action) {
            MenuAction.LIBRARY -> libraryId?.let { list.open(it, query) }
            MenuAction.SERIES -> {
                state.update { it.copy(series = emptyList(), seriesLoading = true) }
                scope.launch {
                    libraryId?.let { series(it) }
                        ?.onSuccess { found -> state.update { it.copy(series = found.sortedWith(Alphabetical.byName { s -> s.name })) } }
                    state.update { it.copy(seriesLoading = false) }
                }
            }
            MenuAction.COLLECTIONS -> {
                state.update { it.copy(collections = emptyList(), collectionsLoading = true) }
                scope.launch {
                    libraryId?.let { collections(it) }
                        ?.onSuccess { found -> state.update { it.copy(collections = found.sortedWith(Alphabetical.byName { c -> c.name })) } }
                    state.update { it.copy(collectionsLoading = false) }
                }
            }
            MenuAction.STATS -> {
                state.update { it.copy(listeningStats = null, statsLoading = true) }
                scope.launch {
                    listeningStats()?.onSuccess { stats -> state.update { it.copy(listeningStats = stats) } }
                    state.update { it.copy(statsLoading = false) }
                }
            }
            else -> Unit
        }
    }

    private fun show(view: MainView) {
        val was = state.value.view
        state.update { it.copy(view = view) }
        if (view == was) return
        counts?.cancel()
        if (view == MainView.SWITCH_LIBRARY) readCounts()
    }

    // Counts for the Switch Library tiles, asked for each time that view opens
    // and never in its way: a tile without them says what kind of library it is.
    private fun readCounts() {
        counts?.cancel()
        counts = scope.launch {
            state.value.libraries.mapNotNull { it.id }.forEach { id ->
                libraryStats(id)?.onSuccess { read -> state.update { it.copy(libraryStats = it.libraryStats + (id to read)) } }
            }
        }
    }

    companion object {
        /** The app's own: the server through [ApiClient], the saved library in preferences. */
        fun of(
            context: Context,
            fetchLibraryItems: suspend (String, LibraryQuery) -> List<LibraryItem>,
            openHome: (String) -> Unit
        ): MainViewModel {
            val preferences = SharedPreferencesHandler(context.applicationContext)
            val library = { ApiClient.getLibraryApi()?.let(::LibraryRepository) }
            val browse = { ApiClient.getBrowseApi()?.let(::BrowseRepository) }
            return MainViewModel(
                libraries = { library()?.libraries() },
                fetchLibraryItems = fetchLibraryItems,
                openHome = openHome,
                savedLibrary = preferences::getSelectedLibraryId,
                saveLibrary = preferences::saveSelectedLibraryId,
                libraryStats = { library()?.stats(it) },
                series = { browse()?.series(it) },
                collections = { browse()?.collections(it) },
                listeningStats = { browse()?.listeningStats() }
            )
        }
    }
}
