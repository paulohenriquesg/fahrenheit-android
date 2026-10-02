package com.paulohenriquesg.fahrenheit.player

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * A screen's connection to the playback service, held while it is visible.
 *
 * Only the connection opened last counts. One that completes after [close] -
 * Home pressed while still connecting - is released on arrival rather than
 * handed to a screen that has moved on (#94).
 *
 * @param onChange the connected controller, or null when connecting failed.
 */
class ControllerSlot<C : Any>(
    private val connect: () -> ListenableFuture<C>,
    private val release: (C) -> Unit,
    private val executor: Executor,
    private val onChange: (C?) -> Unit
) {
    var current: C? = null
        private set

    private var pending: ListenableFuture<C>? = null

    fun open() {
        val future = connect()
        pending = future
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            if (pending !== future) {
                controller?.let(release)
                return@addListener
            }
            pending = null
            current = controller
            onChange(controller)
        }, executor)
    }

    /** @param beforeRelease runs on the connected controller first, e.g. to stop playback on Back. */
    fun close(beforeRelease: (C) -> Unit) {
        pending = null
        current?.let {
            beforeRelease(it)
            release(it)
        }
        current = null
    }
}
