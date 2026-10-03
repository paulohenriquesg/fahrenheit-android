package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction

/**
 * The "See all" tile at the end of a Home shelf (#123): there when the server
 * holds more than the row shows, and leading to the screen that already lists
 * the same things.
 *
 * Recently added opens the library newest first, and Continue listening and
 * Listen again open it filtered to in progress and finished (#146). Newest
 * episodes waits: Latest Episodes loads at most 50 and does not page, so "See
 * all 800" would stop at 50. Discover is random picks that nothing on the
 * server reproduces.
 */
object ShelfSeeAll {

    data class Tile(val total: Int, val opens: MenuAction, val query: LibraryQuery = LibraryQuery.Everything)

    fun of(shelf: Shelf): Tile? {
        val (opens, query) = destination(shelf) ?: return null
        val total = shelf.total ?: return null
        return if (total > shown(shelf)) Tile(total, opens, query) else null
    }

    private fun destination(shelf: Shelf): Pair<MenuAction, LibraryQuery>? = when {
        shelf.id == "recent-series" -> MenuAction.SERIES to LibraryQuery.Everything
        shelf.id == "newest-authors" -> MenuAction.AUTHORS to LibraryQuery.Everything
        // Books or podcasts, newest first: the library sorted by date added (#146).
        shelf.id == "recently-added" && shelf.type in setOf("book", "podcast") -> MenuAction.LIBRARY to LibraryQuery.RecentlyAdded
        // Books only: in a podcast library these shelves are episodes, and a
        // list of podcasts filtered by progress is not the same list.
        shelf.id == "continue-listening" && shelf.type == "book" -> MenuAction.LIBRARY to LibraryQuery.InProgress
        shelf.id == "listen-again" && shelf.type == "book" -> MenuAction.LIBRARY to LibraryQuery.Finished
        else -> null
    }

    private fun shown(shelf: Shelf): Int = when (shelf.type) {
        "series" -> shelf.seriesEntities?.size
        "authors" -> shelf.authorEntities?.size
        else -> shelf.bookEntities?.size
    } ?: 0
}
