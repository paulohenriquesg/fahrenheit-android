package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
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

        val early = washBeforeShowing(waitMs = 500, compute = { delay(10); green }, late = { late += it })

        assertEquals(green, early)
        assertEquals(emptyList<Color?>(), late)
    }

    @Test
    fun `a slow colour does not hold the screen up, and arrives after`() = runBlocking {
        val late = mutableListOf<Color?>()

        val early = washBeforeShowing(waitMs = 50, compute = { delay(300); green }, late = { late += it })

        assertNull(early)
        delay(500)
        assertEquals(listOf<Color?>(green), late)
    }

    @Test
    fun `a cover without colour shows at once with none`() = runBlocking {
        val late = mutableListOf<Color?>()

        val early = washBeforeShowing(waitMs = 500, compute = { null }, late = { late += it })

        assertNull(early)
        assertEquals(emptyList<Color?>(), late)
    }
}
