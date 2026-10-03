package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction

/**
 * The "See all" tile at the end of a Home shelf (#123): there when the server
 * holds more than the row shows, and leading to the screen that already lists
 * the same things.
 *
 * Only shelves with such a screen get one. The library screen has no sort or
 * filter yet, so Recently added (books), Continue listening and Listen again
 * would open a title-sorted list that does not match them; Discover is random
 * picks that nothing on the server reproduces.
 */
object ShelfSeeAll {

    data class Tile(val total: Int, val opens: MenuAction)

    fun of(shelf: Shelf): Tile? {
        val opens = destination(shelf) ?: return null
        val total = shelf.total ?: return null
        return if (total > shown(shelf)) Tile(total, opens) else null
    }

    private fun destination(shelf: Shelf): MenuAction? = when {
        shelf.id == "recent-series" -> MenuAction.SERIES
        shelf.id == "newest-authors" -> MenuAction.AUTHORS
        shelf.id == "newest-episodes" -> MenuAction.LATEST
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
