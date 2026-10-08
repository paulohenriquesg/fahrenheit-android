package com.paulohenriquesg.fahrenheit.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.paulohenriquesg.fahrenheit.api.Shelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Fetches Home's shelves again, keeping what is shown until new ones arrive
 * (#197). The reasons come in bursts - back from the player, and the report
 * of the stop it made - so requests inside one [window] make one fetch; one
 * made after a fetch began may carry news it predates, and makes another.
 *
 * @param fetch the shelves of a library, or null when they could not be read.
 * @param library the library Home shows now; shelves for another are dropped.
 */
class HomeReload(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> List<Shelf>?,
    private val library: () -> String?,
    private val show: (List<Shelf>) -> Unit,
    private val window: suspend () -> Unit = { delay(WINDOW_MS) }
) {
    private var waiting = false

    fun request() {
        if (waiting) return
        waiting = true
        scope.launch {
            try {
                window()
            } finally {
                waiting = false
            }
            val id = library() ?: return@launch
            val shelves = fetch(id) ?: return@launch
            if (library() == id) show(shelves)
        }
    }

    companion object {
        const val WINDOW_MS = 500L
    }
}

/**
 * Asks for Home's shelves again on coming back to the screen - not on first
 * arriving, which the start-up load covers - and each time [news] says the
 * server's idea of what is in progress changed.
 */
@Composable
fun HomeReloadTriggers(news: StateFlow<Int>, onReload: () -> Unit) {
    val reload by rememberUpdatedState(onReload)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        var left = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> left = true
                Lifecycle.Event.ON_RESUME -> if (left) {
                    left = false
                    reload()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(news) {
        // The count as it stands is old news.
        news.drop(1).collect { reload() }
    }
}
