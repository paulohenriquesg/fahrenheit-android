package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** The speeds on offer, and how long is left at one (#107). */
class ListeningSpeedTest {

    @Test fun `seven steps from three quarters to double`() =
        assertEquals(listOf(0.75f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f), ListeningSpeed.STEPS)

    @Test fun `labels drop needless zeros`() =
        assertEquals(
            listOf("0.75×", "1×", "1.1×", "1.25×", "1.5×", "1.75×", "2×"),
            ListeningSpeed.STEPS.map(ListeningSpeed::label)
        )

    @Test fun `what is left counts at the speed`() =
        assertEquals(800.0, ListeningSpeed.left(position = 1000.0, total = 2000.0, speed = 1.25f), 0.001)

    @Test fun `past the end nothing is left`() =
        assertEquals(0.0, ListeningSpeed.left(position = 2100.0, total = 2000.0, speed = 1.5f), 0.0)
}
