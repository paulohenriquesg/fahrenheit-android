package com.paulohenriquesg.fahrenheit.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Counts the moments the server's idea of what is being listened to has just
 * changed: a start's first report, or a closing one, reached it (#197).
 * Screens showing what is in progress read it again on each.
 */
class ListeningNews {
    private val reports = MutableStateFlow(0)

    val count: StateFlow<Int> = reports.asStateFlow()

    fun reported() = reports.update { it + 1 }

    companion object {
        /** The playback service tells; Home listens. */
        val process = ListeningNews()
    }
}
