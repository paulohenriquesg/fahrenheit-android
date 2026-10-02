package com.paulohenriquesg.fahrenheit.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What Back does on the main screen (#53).
 *
 * It used to be handled only while the drawer was open, so from any content
 * view it fell through to the system and quit the app - and with the menu
 * unreachable, that was the only way out.
 *
 * Since #58 the rail is always on screen, so there is no hidden drawer for Back
 * to close: it walks up a level, or leaves.
 */
class BackActionTest {

    // Back should walk up a level before leaving, as it does elsewhere on TV.
    @Test
    fun `from a section, Back returns to Home`() {
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.STATS))
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.LIBRARY))
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.SERIES))
        assertEquals(BackAction.GoHome, BackAction.decide(MainView.COLLECTIONS))
    }

    // Amazon expects Back from the app's root to leave the app, so Home is
    // where that happens - once, and predictably.
    @Test
    fun `from Home, Back leaves the app`() =
        assertEquals(BackAction.Exit, BackAction.decide(MainView.HOME))
}
