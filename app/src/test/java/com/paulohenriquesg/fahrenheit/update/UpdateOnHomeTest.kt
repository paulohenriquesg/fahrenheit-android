package com.paulohenriquesg.fahrenheit.update

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The launch check answers whenever the network does. By then a player may
 * be in front of Home (#229): the question waits for Home to be seen again.
 */
@RunWith(RobolectricTestRunner::class)
class UpdateOnHomeTest {

    private val update = AvailableUpdate(11, "v0.1.1", emptyList(), "https://example.invalid/app.apk", "abc", 10)

    private val home = object : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun `a newer release is offered over Home`() = runBlocking {
        home.registry.currentState = Lifecycle.State.RESUMED
        var shown: AvailableUpdate? = null

        offerOnHome(home.lifecycle, check = { update }) { shown = it }

        assertEquals(update, shown)
    }

    @Test
    fun `nothing newer offers nothing`() = runBlocking {
        home.registry.currentState = Lifecycle.State.RESUMED
        var shown: AvailableUpdate? = null

        offerOnHome(home.lifecycle, check = { null }) { shown = it }

        assertNull(shown)
    }

    @Test
    fun `the question never appears over the player, and waits for Home`() = runBlocking {
        // A player opened from Home stops it.
        home.registry.currentState = Lifecycle.State.CREATED
        var shown: AvailableUpdate? = null

        val job = launch { offerOnHome(home.lifecycle, check = { update }) { shown = it } }
        repeat(5) { yield() }
        assertNull("asked over the player", shown)

        home.registry.currentState = Lifecycle.State.STARTED
        job.join()
        assertEquals(update, shown)
    }
}
