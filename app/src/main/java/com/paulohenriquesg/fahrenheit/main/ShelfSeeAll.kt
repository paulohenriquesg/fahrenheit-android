package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction

/**
 * The "See all" tile at the end of a Home shelf (#123): there when the server
 * holds more than the row shows, and leading to the screen that already lists
 * the same things.
 *
 * Only shelves whose screen exists today get one. The issue asks for Recently
 * added (books) sorted by date added, and Continue listening and Listen again
 * filtered to in progress and finished; the library screen has no sort or
 * filter yet, so those wait for it. Newest episodes waits too: Latest Episodes
 * loads at most 50 and does not page, so "See all 800" would stop at 50.
 * Discover is random picks that nothing on the server reproduces.
 */
object ShelfSeeAll {

    data class Tile(val total: Int, val opens: MenuAction, val query: LibraryQuery = LibraryQuery.Everything)

    fun of(shelf: Shelf): Tile? {
        val opens = destination(shelf) ?: return null
        val total = shelf.total ?: return null
        return if (total > shown(shelf)) Tile(total, opens) else null
    }

    private fun destination(shelf: Shelf): MenuAction? = when {
        shelf.id == "recent-series" -> MenuAction.SERIES
        shelf.id == "newest-authors" -> MenuAction.AUTHORS
        // A podcast library's own list is every podcast, which is what this
        // shelf is the start of; a book library's would need a date sort.
        shelf.id == "recently-added" && shelf.type == "podcast" -> MenuAction.LIBRARY
        else -> null
    }

    private fun shown(shelf: Shelf): Int = when (shelf.type) {
        "series" -> shelf.seriesEntities?.size
        "authors" -> shelf.authorEntities?.size
        else -> shelf.bookEntities?.size
    } ?: 0
}
