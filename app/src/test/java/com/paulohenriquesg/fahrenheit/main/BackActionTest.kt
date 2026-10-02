package com.paulohenriquesg.fahrenheit.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What Back does on the main screen (#53).
 *
 * On the device, Back was handled only while the drawer was open, so from any
 * content view it fell through to the system and quit the app - with the menu
 * unreachable, that was the only way out.
 */
class BackActionTest {

    @Test
    fun `with the menu open, Back closes it`() =
        assertEquals(BackAction.CloseDrawer, BackAction.decide(drawerOpen = true, view = MainView.STATS))

    // Back should walk up a level before leaving, as it does elsewhere on TV.
    @Test
    fun `from a section, Back returns to Home`() {
        assertEquals(BackAction.GoHome, BackAction.decide(drawerOpen = false, view = MainView.STATS))
        assertEquals(BackAction.GoHome, BackAction.decide(drawerOpen = false, view = MainView.LIBRARY))
        assertEquals(BackAction.GoHome, BackAction.decide(drawerOpen = false, view = MainView.SERIES))
    }

    // Amazon expects Back from the app's root to leave the app, so Home is
    // where that happens - once, and predictably.
    @Test
    fun `from Home, Back leaves the app`() =
        assertEquals(BackAction.Exit, BackAction.decide(drawerOpen = false, view = MainView.HOME))

    @Test
    fun `the menu closing takes priority over the view`() =
        assertEquals(BackAction.CloseDrawer, BackAction.decide(drawerOpen = true, view = MainView.HOME))
}
