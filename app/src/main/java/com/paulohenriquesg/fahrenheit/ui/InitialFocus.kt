package com.paulohenriquesg.fahrenheit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

private const val FRAMES_TO_KEEP_TRYING = 60

/**
 * Asks for focus, waiting for the target to be attached.
 *
 * [FocusRequester.requestFocus] throws when nothing is attached yet, which is
 * what killed the app when the drawer opened before its items had been
 * composed. Returns whether focus was taken.
 */
suspend fun FocusRequester.requestFocusWhenAttached(): Boolean {
    repeat(FRAMES_TO_KEEP_TRYING) {
        if (runCatching { requestFocus() }.isSuccess) return true
        withFrameNanos { }
    }
    return false
}

/**
 * A [FocusRequester] for the first thing a screen wants focused on arrival:
 * nothing on a TV responds to a D-pad until something holds focus.
 *
 * Attach the result to a focusable target with `Modifier.focusRequester(...)`.
 * Requesting focus on the content's focus group instead does not work: until a
 * child is focusable the group takes the focus itself, where it highlights
 * nothing, refuses `moveFocus(Enter)`, and sends the first press to the wrong
 * item.
 *
 * @param enabled false leaves focus alone, for when something above the content
 *   owns it (an open drawer, say).
 * @param keys the data whose arrival should re-run the request, as with
 *   [LaunchedEffect].
 */
@Composable
fun rememberInitialFocus(enabled: Boolean, vararg keys: Any?): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(enabled, *keys) {
        if (!enabled) return@LaunchedEffect
        // The target is attached a frame or two after the data it is built from.
        requester.requestFocusWhenAttached()
    }
    return requester
}
