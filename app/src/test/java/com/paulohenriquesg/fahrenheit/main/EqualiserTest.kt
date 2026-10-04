package com.paulohenriquesg.fahrenheit.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The heights of Now playing's equaliser bars, through one run (#170). */
class EqualiserTest {

    private val phases = (0..100).map { it / 100f }

    @Test fun `three bars`() {
        assertEquals(3, Equaliser.levels(0.3f).size)
    }

    // Never flat to nothing, never past the top: always visibly a bar.
    @Test fun `every level is between a floor and full height`() {
        phases.flatMap { Equaliser.levels(it) }.forEach {
            assertTrue("level $it", it in Equaliser.FLOOR..1f)
        }
    }

    @Test fun `the bars move through a run`() {
        assertNotEquals(Equaliser.levels(0.1f), Equaliser.levels(0.2f))
    }

    // Bars in lockstep read as one pulsing block, not an equaliser.
    @Test fun `the bars do not move in lockstep`() {
        assertTrue(phases.any { phase -> Equaliser.levels(phase).toSet().size == 3 })
    }

    // A run ends where the next begins, so a poll arriving does not make the bars jump.
    @Test fun `a run ends where it starts`() {
        Equaliser.levels(0f).zip(Equaliser.levels(1f)).forEach { (start, end) ->
            assertEquals(start, end, 0.001f)
        }
    }
}
