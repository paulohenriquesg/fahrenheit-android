package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction

/**
 * The "See all" tile at the end of a Home shelf (#123): there when the server
 * holds more than the row shows, and leading to the screen that already lists
 * the same things.
 *
 * Which shelves get one, and where it leads, is in [HomeShelves] (#147).
 */
object ShelfSeeAll {

    data class Tile(val total: Int, val opens: MenuAction, val query: LibraryQuery = LibraryQuery.Everything)

    fun of(shelf: Shelf): Tile? {
        val (opens, query) = destination(shelf) ?: return null
        val total = shelf.total ?: return null
        return if (total > shown(shelf)) Tile(total, opens, query) else null
    }

    private fun destination(shelf: Shelf): Pair<MenuAction, LibraryQuery>? = HomeShelves.of(shelf)?.seeAll

    private fun shown(shelf: Shelf): Int = when (shelf.type) {
        "series" -> shelf.seriesEntities?.size
        "authors" -> shelf.authorEntities?.size
        else -> shelf.bookEntities?.size
    } ?: 0
}
