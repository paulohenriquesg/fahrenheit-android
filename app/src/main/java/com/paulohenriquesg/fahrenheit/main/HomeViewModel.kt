package com.paulohenriquesg.fahrenheit.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.FavouritesShelf
import com.paulohenriquesg.fahrenheit.podcast.EpisodeProgress
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import com.paulohenriquesg.fahrenheit.ui.elements.CoverProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What Home shows, as one value (#208).
 *
 * @property shelves the library's personalized shelves, Favourites among them.
 * @property loading a library is being opened, with nothing of it to show yet.
 * @property covers how far into each book or episode on a shelf (#104).
 * @property episodes what has been heard, by episode id, for Latest Episodes (#78).
 */
data class HomeUiState(
    val shelves: List<Shelf> = emptyList(),
    val loading: Boolean = false,
    val covers: CoverProgress = CoverProgress.None,
    val episodes: Map<String, EpisodeProgress> = emptyMap()
)

/**
 * Home's state, out of the composables (#208): its shelves, and the progress
 * drawn on them from the shared [ProgressStore] (#207).
 *
 * The shelves are loaded when a library is [open]ed, and again (#197) when
 * Home is [returned] to and when the store says listening started, stopped or
 * finished - what moves an item on or off them. Those reloads go through
 * [HomeReload]: coalesced, owed while Home is not [visible], and never for
 * the worse. A failure worth saying - a load the viewer asked for that leaves
 * Home empty - comes out of [failures], for the screen to say.
 *
 * @param fetchShelves a library's shelves; null when they could not be read.
 * @param window the coalescing window of a reload.
 */
class HomeViewModel(
    store: ProgressStore,
    private val fetchShelves: suspend (String) -> List<Shelf>? = { null },
    window: suspend () -> Unit = { delay(HomeReload.WINDOW_MS) }
) : ViewModel() {

    private data class Shelves(val list: List<Shelf> = emptyList(), val loading: Boolean = false)

    private val shelves = MutableStateFlow(Shelves())
    private var library: String? = null
    private var opening: Job? = null
    private val said = Channel<Unit>(Channel.BUFFERED)

    /** A load the viewer asked for failed and left Home empty. */
    val failures: Flow<Unit> = said.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(store.entries, shelves) { all, home -> stateOf(home, all.values.toList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateOf(shelves.value, store.entries.value.values.toList()))

    private val reload = HomeReload(
        scope = viewModelScope,
        fetch = fetchShelves,
        library = { library },
        show = { shelves.value = Shelves(it) },
        empty = { shelves.value.list.isEmpty() },
        failed = { said.trySend(Unit) },
        window = window
    )

    init {
        // The count as it stands is old news.
        viewModelScope.launch { store.news.drop(1).collect { reload.request() } }
    }

    /** The first load, or a library switch: the last library's shelves go at once. */
    fun open(libraryId: String) {
        library = libraryId
        opening?.cancel()
        shelves.value = Shelves(loading = true)
        opening = viewModelScope.launch {
            val read = fetchShelves(libraryId)
            if (library != libraryId) return@launch
            shelves.value = Shelves(read.orEmpty())
            if (read == null) said.trySend(Unit)
        }
    }

    /** Home chosen in the menu: asked for, so a failure that leaves it empty is said. */
    fun choseHome() = reload.request(asked = true)

    /** Back on the screen after it was stopped - from the player, say. */
    fun returned() = reload.request()

    /** Whether Home is the view, with the screen up. */
    fun visible(showing: Boolean) {
        reload.showing = showing
    }

    /** The Favourites choice changed (#180): that shelf alone is read again. */
    fun refreshFavourites(favourites: Favourites?) {
        val libraryId = library ?: return
        viewModelScope.launch {
            val refreshed = FavouritesShelf.refreshed({ shelves.value.list }, libraryId, favourites)
            if (library == libraryId) shelves.value = shelves.value.copy(list = refreshed)
        }
    }

    private fun stateOf(home: Shelves, progress: List<MediaProgressResponse>) = HomeUiState(
        shelves = home.list,
        loading = home.loading,
        covers = CoverProgress.index(progress),
        episodes = EpisodeProgress.byEpisode(progress)
    )
}
