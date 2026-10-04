package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction

object HomeShelves {
    enum class Style { Covers, Episodes, Authors, Series }
    enum class Action { Details, Play }

    data class Behaviour(
        val style: Style,
        val press: Action = Action.Details,
        val longPress: Action? = null,
        val seeAll: Pair<MenuAction, LibraryQuery>? = null
    )

    fun of(shelf: Shelf): Behaviour? = null
}
