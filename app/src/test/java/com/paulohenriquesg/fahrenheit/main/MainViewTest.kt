package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which part of the main screen a menu choice shows.
 *
 * The screen kept this as two parallel strings, `viewMode` and
 * `currentSection`, written at different places.
 */
class MainViewTest {

    @Test
    fun `menu choices that change the view`() {
        assertEquals(MainView.HOME, MainView.forMenuAction(MenuAction.HOME))
        assertEquals(MainView.LIBRARY, MainView.forMenuAction(MenuAction.LIBRARY))
        assertEquals(MainView.SERIES, MainView.forMenuAction(MenuAction.SERIES))
        assertEquals(MainView.COLLECTIONS, MainView.forMenuAction(MenuAction.COLLECTIONS))
        assertEquals(MainView.AUTHORS, MainView.forMenuAction(MenuAction.AUTHORS))
        assertEquals(MainView.STATS, MainView.forMenuAction(MenuAction.STATS))
    }

    // These open another screen or do something in place; the view behind them
    // must stay as it was.
    @Test
    fun `menu choices that leave the view alone`() {
        assertNull(MainView.forMenuAction(MenuAction.LATEST))
        assertNull(MainView.forMenuAction(MenuAction.SELECT_LIBRARY))
        assertNull(MainView.forMenuAction(MenuAction.SETTINGS))
        assertNull(MainView.forMenuAction(MenuAction.LOGOUT))
        assertNull(MainView.forMenuAction(MenuAction.NARRATORS))
    }

    @Test
    fun `every view has the menu id the drawer highlights`() {
        assertEquals("home", MainView.HOME.menuItemId)
        assertEquals("library", MainView.LIBRARY.menuItemId)
        assertEquals("series", MainView.SERIES.menuItemId)
        assertEquals("collections", MainView.COLLECTIONS.menuItemId)
        assertEquals("authors", MainView.AUTHORS.menuItemId)
        assertEquals("stats", MainView.STATS.menuItemId)
    }
}
