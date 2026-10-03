package com.paulohenriquesg.fahrenheit.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester

/**
 * What Back means on the main screen, as a table rather than a condition spread
 * through the handler (#53). It walks up a level at a time (#125): from the
 * content to the rail, from a section's rail to Home, and from Home's rail out.
 */
enum class BackAction {
    FocusRail,
    GoHome,
    Exit;

    companion object {
        fun decide(view: MainView, railHasFocus: Boolean): BackAction = when {
            !railHasFocus -> FocusRail
            view != MainView.HOME -> GoHome
            else -> Exit
        }
    }
}

/**
 * Applies [BackAction]. [rail] is attached to the rail's selected section, so
 * focusing it opens the rail on the section you are in. Exit is left to the
 * system: the handler stands aside rather than finishing the activity itself.
 */
@Composable
fun MainBackHandler(view: MainView, railHasFocus: Boolean, rail: FocusRequester, onGoHome: () -> Unit) {
    val action = BackAction.decide(view, railHasFocus)
    BackHandler(enabled = action != BackAction.Exit) {
        when (action) {
            // Throws when no section is attached to take it; Back then does nothing.
            BackAction.FocusRail -> runCatching { rail.requestFocus() }
            BackAction.GoHome -> onGoHome()
            BackAction.Exit -> Unit
        }
    }
}
