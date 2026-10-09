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

    // Review (#144): with the player screen up, a Play it let through must not
    // open a second one on top.
    @Test
    fun `the player screen is counted apart`() {
        val visibility = AppVisibility()

        visibility.started(player = false)
        assertFalse(visibility.playerVisible)
        visibility.started(player = true)
        assertTrue(visibility.playerVisible)
        visibility.stopped(player = true)

        assertFalse(visibility.playerVisible)
        assertTrue(visibility.visible)
    }

    // #207: coming back to the app reads the server's progress again; going
    // from one screen to the next is not coming back - the next starts first.
    @Test
    fun `a start says whether it brought the app to the front`() {
        val visibility = AppVisibility()

        assertTrue("launched", visibility.started())
        assertFalse("the next screen", visibility.started())
        visibility.stopped()
        visibility.stopped()

        assertTrue("back from the TV's home screen", visibility.started())
    }
}
