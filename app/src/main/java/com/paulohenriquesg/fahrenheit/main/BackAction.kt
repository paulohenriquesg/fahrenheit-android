package com.paulohenriquesg.fahrenheit.main

/**
 * What Back does on the main screen.
 *
 * It used to be handled only while the drawer was open, so from a content view
 * it fell through to the system and quit the app - and with the menu
 * unreachable by remote, quitting was the only way out (#53). Back now walks up
 * a level: menu first, then Home, then out.
 */
enum class BackAction {
    CloseDrawer,
    GoHome,
    Exit;

    companion object {
        fun decide(drawerOpen: Boolean, view: MainView): BackAction = when {
            drawerOpen -> CloseDrawer
            view != MainView.HOME -> GoHome
            else -> Exit
        }
    }
}
