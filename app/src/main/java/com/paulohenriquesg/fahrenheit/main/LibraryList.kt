package com.paulohenriquesg.fahrenheit.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 */
class LibraryList(
    private val scope: CoroutineScope,
    private val fetch: suspend (String, LibraryQuery) -> List<LibraryItem>
) {
    var query by mutableStateOf(LibraryQuery.Everything)
        private set
    var items by mutableStateOf(emptyList<LibraryItem>())
        private set
    var loading by mutableStateOf(false)
        private set

    private var request: Job? = null

    fun open(libraryId: String, query: LibraryQuery) {
        request?.cancel()
        // Another view's list must not show under this one's label while it
        // loads; the same view's stays up while it refreshes.
        if (query != this.query) items = emptyList()
        this.query = query
        loading = true
        request = scope.launch {
            val fetched = fetch(libraryId, query)
            if (this@LibraryList.query == query) {
                items = fetched
                loading = false
            }
        }
    }

    /** A different library: none of this one's view or list carries over. */
    fun clear() {
        request?.cancel()
        query = LibraryQuery.Everything
        items = emptyList()
        loading = false
    }
}
