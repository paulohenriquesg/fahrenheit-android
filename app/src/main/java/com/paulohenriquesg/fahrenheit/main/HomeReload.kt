package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.Shelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fetches Home's shelves again, keeping what is shown until new ones arrive
 * (#197). The reasons come in bursts - back from the player, and the report
 * of the stop it made - so requests inside one [window] make one fetch; one
 * made after a fetch began may carry news it predates, and makes another,
 * whose shelves win however the two answer.
 *
 * While Home is not [showing], a request is owed rather than made, and made
 * when it shows again: Back to Home from another view fetches nothing else.
 *
 * A failure keeps what is shown, and is said ([failed]) only for a load the
 * viewer asked for that leaves Home [empty]: one in the background - back on
 * Home over a network blip - says nothing.
 *
 * @param fetch the shelves of a library, or null when they could not be read.
 * @param library the library Home shows now; shelves for another are dropped.
 */
class HomeReload(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> List<Shelf>?,
    private val library: () -> String?,
    private val show: (List<Shelf>) -> Unit,
    private val empty: () -> Boolean = { false },
    private val failed: () -> Unit = {},
    private val window: suspend () -> Unit = { delay(WINDOW_MS) }
) {
    private var waiting = false
    private var owed = false
    private var owedAsked = false
    /** Whether a request in the open window was the viewer's. */
    private var asked = false
    /** The latest fetch begun; only its shelves are shown. */
    private var latest = 0

    /** Whether Home is the view. */
    var showing: Boolean = true
        set(value) {
            field = value
            if (value && owed) request(asked = owedAsked)
        }

    /** @param asked the viewer asked for it, choosing Home: a failure is worth saying. */
    fun request(asked: Boolean = false) {
        if (!showing) {
            owed = true
            owedAsked = owedAsked || asked
            return
        }
        owed = false
        owedAsked = false
        this.asked = this.asked || asked
        if (waiting) return
        waiting = true
        scope.launch {
            var wasAsked = false
            try {
                window()
            } finally {
                waiting = false
                wasAsked = this@HomeReload.asked
                this@HomeReload.asked = false
            }
            val id = library() ?: return@launch
            val ticket = ++latest
            val shelves = fetch(id)
            if (shelves == null) {
                if (wasAsked && empty()) failed()
                return@launch
            }
            if (ticket == latest && library() == id) show(shelves)
        }
    }

    companion object {
        const val WINDOW_MS = 500L
    }
}
