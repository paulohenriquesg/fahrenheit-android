package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Whether any of the app's screens is showing: the service may open one only then (#144). */
class AppVisibilityTest {

    @Test
    fun `visible while any screen is started`() {
        val visibility = AppVisibility()
        assertFalse(visibility.visible)

        visibility.started()
        visibility.started()
        visibility.stopped()
        assertTrue(visibility.visible)

        visibility.stopped()
        assertFalse(visibility.visible)
    }
}
