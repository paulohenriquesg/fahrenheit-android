package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The player waits briefly for its cover's colour before it shows, so it does
 * not open black and then change; a slow cover never holds it up (#107).
 */
class WashBeforeShowingTest {
    private val green = Color(0xFF24301A)

    @Test
    fun `a colour ready in time is there when the screen shows`() = runBlocking {
        val late = mutableListOf<Color?>()

        val early = washBeforeShowing(waitMs = 500, colour = async { delay(10); green }, late = { late += it })

        assertEquals(green, early)
        assertEquals(emptyList<Color?>(), late)
    }

    @Test
    fun `a slow colour does not hold the screen up, and arrives after`() = runBlocking {
        val late = mutableListOf<Color?>()

        val early = washBeforeShowing(waitMs = 50, colour = async { delay(300); green }, late = { late += it })

        assertNull(early)
        delay(500)
        assertEquals(listOf<Color?>(green), late)
    }

    @Test
    fun `a cover without colour shows at once with none`() = runBlocking {
        val late = mutableListOf<Color?>()

        val early = washBeforeShowing(waitMs = 500, colour = async { null }, late = { late += it })

        assertNull(early)
        assertEquals(emptyList<Color?>(), late)
    }

    // Review: a colour that finished just as the wait ran out was dropped.
    @Test
    fun `a colour that is ready when the wait runs out is used`() = runBlocking {
        val early = washBeforeShowing(waitMs = 0, colour = CompletableDeferred(green), late = {})

        assertEquals(green, early)
    }

    // Review: started before the item loads, the wait is what is left of the budget.
    @Test
    fun `the wait is only what is left of the budget`() = runBlocking {
        val started = System.currentTimeMillis()

        washBeforeShowing(waitMs = -10, colour = async { delay(1_000); green }, late = {})

        assertEquals(true, System.currentTimeMillis() - started < 500)
    }

    // Review: an unexpected failure while reading the colour would have
    // cancelled the player's loading and crashed it.
    @Test
    fun `a colour that fails is no colour`() = runBlocking {
        assertNull(washOrNothing { throw IllegalStateException("bad bitmap") })
    }
}
