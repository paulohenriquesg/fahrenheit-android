package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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
    compute: suspend () -> Color?,
    late: (Color?) -> Unit
): Color? {
    val colour = async { compute() }
    val early = withTimeoutOrNull(waitMs) { colour.await() }
    if (early != null || colour.isCompleted) return early
    launch { late(colour.await()) }
    return null
}
