package com.paulohenriquesg.fahrenheit.player

import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The player screen's connection to the playback service, while it is visible.
 *
 * A connection can complete after the screen has already stopped - Home
 * pressed while it was still connecting. That late result used to be handed
 * to the screen anyway: a controller nobody would release, or a "could not
 * connect" error on a screen that had moved on (#94).
 */
class ControllerSlotTest {

    private class Fake(val name: String)

    private val released = mutableListOf<String>()
    private val left = mutableListOf<String>()
    private val handed = mutableListOf<String?>()
    private val pending = ArrayDeque<SettableFuture<Fake>>()

    private val slot = ControllerSlot(
        connect = { SettableFuture.create<Fake>().also { pending.addLast(it) } },
        release = { released += it.name },
        executor = MoreExecutors.directExecutor(),
        onChange = { handed += it?.name }
    )

    @Test
    fun `a connection is handed to the screen`() {
        slot.open()

        pending.removeFirst().set(Fake("a"))

        assertEquals(listOf<String?>("a"), handed)
        assertEquals("a", slot.current?.name)
    }

    @Test
    fun `a connection that arrives after the screen stopped is released, not handed over`() {
        slot.open()
        val late = pending.removeFirst()
        slot.close { left += it.name }

        late.set(Fake("late"))

        assertEquals(emptyList<String?>(), handed)
        assertEquals(listOf("late"), released)
        assertEquals(emptyList<String>(), left)
    }

    @Test
    fun `a failure that arrives after the screen stopped is not reported`() {
        slot.open()
        val late = pending.removeFirst()
        slot.close { left += it.name }

        late.setException(IllegalStateException("no service"))

        assertEquals(emptyList<String?>(), handed)
    }

    @Test
    fun `a failure while visible is reported as no controller`() {
        slot.open()

        pending.removeFirst().setException(IllegalStateException("no service"))

        assertEquals(listOf<String?>(null), handed)
    }

    @Test
    fun `stopping lets the screen leave first, then releases`() {
        slot.open()
        pending.removeFirst().set(Fake("a"))

        slot.close { left += it.name; assertEquals(emptyList<String>(), released) }

        assertEquals(listOf("a"), left)
        assertEquals(listOf("a"), released)
        assertNull(slot.current)
    }

    @Test
    fun `coming back connects again, and only the new connection counts`() {
        slot.open()
        val first = pending.removeFirst()
        slot.close { }
        slot.open()

        first.set(Fake("old"))
        pending.removeFirst().set(Fake("new"))

        assertEquals(listOf<String?>("new"), handed)
        assertEquals(listOf("old"), released)
    }
}
