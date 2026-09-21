package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.navigation.MenuAction

/**
 * What the main screen is showing.
 *
 * Held as two strings before, `viewMode` and `currentSection`, set a few lines
 * apart: the drawer highlighted one and the body rendered the other.
 */
enum class MainView(val menuItemId: String) {
    HOME("home"),
    LIBRARY("library"),
    SERIES("series"),
    COLLECTIONS("collections"),
    AUTHORS("authors"),
    STATS("stats");

    companion object {
        /**
         * The view a menu choice switches to, or null when it opens another
         * screen or acts in place and the view stays as it was.
         */
        fun forMenuAction(action: MenuAction): MainView? = when (action) {
            MenuAction.HOME -> HOME
            MenuAction.LIBRARY -> LIBRARY
            MenuAction.SERIES -> SERIES
            MenuAction.COLLECTIONS -> COLLECTIONS
            MenuAction.AUTHORS -> AUTHORS
            MenuAction.STATS -> STATS
            MenuAction.NARRATORS,
            MenuAction.LATEST,
            MenuAction.SELECT_LIBRARY,
            MenuAction.SETTINGS,
            MenuAction.LOGOUT -> null
        }
    }
}
