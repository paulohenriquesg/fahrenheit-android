package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.paulohenriquesg.fahrenheit.player.ListeningNews
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What makes Home fetch its shelves again (#197). */
@RunWith(RobolectricTestRunner::class)
class HomeReloadTriggersTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val news = ListeningNews()
    private var asked = 0

    private fun show() {
        compose.setContent { HomeReloadTriggers(news.count) { asked++ } }
        compose.waitForIdle()
    }

    @Test
    fun `arriving asks nothing, the first load being the start-up one`() {
        show()

        assertEquals(0, asked)
    }

    @Test
    fun `coming back to the screen, say from the player, asks once`() {
        show()

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()

        assertEquals(1, asked)
    }

    @Test
    fun `losing focus without leaving asks nothing`() {
        show()

        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()

        assertEquals(0, asked)
    }

    @Test
    fun `each report of a start or stop reaching the server asks`() {
        show()

        news.reported()
        compose.waitForIdle()
        news.reported()
        compose.waitForIdle()

        assertEquals(2, asked)
    }
}
