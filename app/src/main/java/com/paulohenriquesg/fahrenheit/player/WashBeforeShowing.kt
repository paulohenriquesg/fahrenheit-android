package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The cover's colour, waited for briefly before the player shows, so the
 * screen does not open black and then change (#107).
 *
 * Ready within [waitMs]: it is returned, and the screen opens in it. Later: the
 * screen opens without it, and [late] receives it when it arrives, for the
 * screen to fade in. A cover with no usable colour opens at once, with none.
 * The work belongs to this scope, so leaving the screen cancels it.
 */
suspend fun CoroutineScope.washBeforeShowing(
    waitMs: Long,
    colour: Deferred<Color?>,
    late: (Color?) -> Unit
): Color? {
    val early = withTimeoutOrNull(waitMs) { colour.await() }
    // Finished just as the wait ran out: still in time.
    if (early != null || colour.isCompleted) return early ?: colour.await()
    launch { late(colour.await()) }
    return null
}

/**
 * The colour, or none if working it out failed: a cover is decoration, and a
 * bad one must not take the player down with it. Leaving the screen still
 * cancels it.
 */
suspend fun washOrNothing(compute: suspend () -> Color?): Color? = try {
    compute()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
