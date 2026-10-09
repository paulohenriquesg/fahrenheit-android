package com.paulohenriquesg.fahrenheit.player

/**
 * Whether any of the app's screens is showing, counted from their starts and
 * stops: the playback service may open the player screen only then (#144).
 * Main thread.
 */
class AppVisibility {
    private var started = 0
    private var players = 0

    val visible: Boolean get() = started > 0

    /** The player screen is up: it asks for itself (#142). */
    val playerVisible: Boolean get() = players > 0

    /** @return whether this start brought the app to the front: no screen of it was showing. */
    fun started(player: Boolean = false): Boolean {
        started++
        if (player) players++
        return started == 1
    }

    fun stopped(player: Boolean = false) {
        started = (started - 1).coerceAtLeast(0)
        if (player) players = (players - 1).coerceAtLeast(0)
    }

    companion object {
        /** Kept by the Application for every activity. */
        val process = AppVisibility()
    }
}
