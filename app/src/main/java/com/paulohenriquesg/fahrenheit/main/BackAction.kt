package com.paulohenriquesg.fahrenheit.main

/**
 * What Back means on the main screen, as a table rather than a condition spread
 * through the handler (#53).
 */
enum class BackAction {
    GoHome,
    Exit;

    companion object {
        fun decide(view: MainView): BackAction =
            if (view != MainView.HOME) GoHome else Exit
    }
}
