package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction

/**
 * What Home does with each shelf the server sends (#147): how its row is
 * drawn, what a press and a long press on a card do, and where its "See all"
 * leads. Looked up by the shelf's id, then its type; a shelf of a type this
 * version does not know is drawn as plain covers if it holds library items.
 *
 * Adding a server shelf is one row here.
 */
object HomeShelves {

    enum class Style { Covers, Episodes, Authors, Series }

    enum class Action { Details, Play }

    data class Behaviour(
        val style: Style,
        val press: Action = Action.Details,
        val longPress: Action? = null,
        /** The screen and library view "See all" opens, or null for no tile. */
        val seeAll: Pair<MenuAction, LibraryQuery>? = null
    )

    private class Row(val id: String, val types: Set<String>, val behaviour: Behaviour)

    // By id, for the shelves that behave differently from others of their type.
    private val byId = listOf(
        // Plays from where it was, as an episode there does; details a long press away (#124).
        Row("continue-listening", setOf("book"), Behaviour(Style.Covers, Action.Play, Action.Details, MenuAction.LIBRARY to LibraryQuery.InProgress)),
        Row("listen-again", setOf("book"), Behaviour(Style.Covers, seeAll = MenuAction.LIBRARY to LibraryQuery.Finished)),
        Row("recently-added", setOf("book", "podcast"), Behaviour(Style.Covers, seeAll = MenuAction.LIBRARY to LibraryQuery.RecentlyAdded)),
        Row("recent-series", setOf("series"), Behaviour(Style.Series, seeAll = MenuAction.SERIES to LibraryQuery.Everything)),
        Row("newest-authors", setOf("authors"), Behaviour(Style.Authors, seeAll = MenuAction.AUTHORS to LibraryQuery.Everything))
    )

    // By type, for everything else. Discover gets no "See all": random picks
    // that nothing on the server reproduces. Newest episodes neither: Latest
    // Episodes loads at most 50 and does not page.
    private val byType = mapOf(
        "book" to Behaviour(Style.Covers),
        "podcast" to Behaviour(Style.Covers),
        "episode" to Behaviour(Style.Episodes),
        "authors" to Behaviour(Style.Authors),
        "series" to Behaviour(Style.Series)
    )

    /** How to draw [shelf], or null when Home cannot draw it at all. */
    fun of(shelf: Shelf): Behaviour? {
        byId.firstOrNull { it.id == shelf.id && shelf.type in it.types }?.let { return it.behaviour }
        byType[shelf.type]?.let { return it }
        return if (!shelf.bookEntities.isNullOrEmpty()) Behaviour(Style.Covers) else null
    }
}
