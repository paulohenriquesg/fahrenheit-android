package com.paulohenriquesg.fahrenheit.player

/**
 * Whether any of the app's screens is showing, counted from their starts and
 * stops: the playback service may open the player screen only then (#144).
 * Main thread.
 */
class AppVisibility {
    private var started = 0

    val visible: Boolean get() = started > 0

    fun started() {
        started++
    }

    fun stopped() {
        started = (started - 1).coerceAtLeast(0)
    }

    companion object {
        /** Kept by the Application for every activity. */
        val process = AppVisibility()
    }
}
