package com.paulohenriquesg.fahrenheit.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What Back does on the main screen (#53, #125).
 *
 * It used to be handled only while the drawer was open, so from any content
 * view it fell through to the system and quit the app - and with the menu
 * unreachable, that was the only way out.
 *
 * Since #58 the rail is always on screen, so Back walks up a level at a time:
 * from the content to the rail, from the rail to Home, and from Home's rail out.
 */
class BackActionTest {

    // #125: on Home, Back from the shelves closed the app at once.
    @Test
    fun `from the content, Back goes to the rail, whatever the section`() {
        MainView.entries.forEach { view ->
            assertEquals(view.name, BackAction.FocusRail, BackAction.decide(view, railHasFocus = false))
        }
    }

    @Test
    fun `from the rail on a section, Back returns to Home`() {
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.STATS, railHasFocus = true))
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.LIBRARY, railHasFocus = true))
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.SERIES, railHasFocus = true))
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.SETTINGS, railHasFocus = true))
    }

    // Amazon expects Back from the app's root to leave the app, so Home's rail
    // is where that happens - once, and predictably.
    @Test
    fun `from the rail on Home, Back leaves the app`() =
        assertEquals(BackAction.Exit, BackAction.decide(MainView.HOME, railHasFocus = true))
}
