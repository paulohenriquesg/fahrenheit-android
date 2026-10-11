package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The library list and which view of it is showing (#146): the whole library
 * from the rail, or the view a Home shelf's "See all" stands for.
 *
 * Only the latest request's answer is shown. Each fetch used to write the list
 * whenever it finished, so the whole library, the slowest fetch and the one
 * made at startup, could land after a tile's filtered list, under its label.
 *
 * @param changed each new [Listing], for the screen's state (#208).
 */
class LibraryList(
    private val scope: CoroutineScope,
    private val changed: (Listing) -> Unit = {},
    private val fetch: suspend (String, LibraryQuery) -> List<LibraryItem>
) {
    /** What the Library view shows: which view, its items, and whether they are coming. */
    data class Listing(
        val query: LibraryQuery = LibraryQuery.Everything,
        val items: List<LibraryItem> = emptyList(),
        val loading: Boolean = false
    )

    private var listing = Listing()
        set(value) {
            field = value
            changed(value)
        }

    val query get() = listing.query
    val items get() = listing.items
    val loading get() = listing.loading

    private var request: Job? = null

    fun open(libraryId: String, query: LibraryQuery) {
        request?.cancel()
        // Another view's list must not show under this one's label while it
        // loads; the same view's stays up while it refreshes.
        listing = Listing(query, if (query != this.query) emptyList() else items, loading = true)
        request = scope.launch {
            val fetched = fetch(libraryId, query)
            if (this@LibraryList.query == query) listing = Listing(query, fetched, loading = false)
        }
    }

    /** A different library: none of this one's view or list carries over. */
    fun clear() {
        request?.cancel()
        listing = Listing()
    }
}
