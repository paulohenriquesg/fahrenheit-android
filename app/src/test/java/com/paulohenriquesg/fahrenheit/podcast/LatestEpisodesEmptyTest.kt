package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Frame 2 of docs/mocks/latest-episodes.html (#109). The server lists only
 * unfinished episodes, so an empty list usually means a listener who is up to
 * date: not an error, and it should not read like one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class LatestEpisodesEmptyTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `an empty list says the listener is caught up, and why`() {
        compose.setContent { FahrenheitTheme { LatestEpisodesView("lib", load = { Result.success(emptyList()) }) } }
        compose.waitForIdle()

        compose.onNodeWithText("You're all caught up").assertIsDisplayed()
        compose.onNodeWithText("Finished episodes are not listed here", substring = true).assertIsDisplayed()
        compose.onNodeWithText("No recent episodes found").assertDoesNotExist()
    }

    @Test
    fun `a list that could not be read still says so, not caught up`() {
        compose.setContent {
            FahrenheitTheme { LatestEpisodesView("lib", load = { Result.failure(IllegalStateException("offline")) }) }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Could not load recent episodes", substring = true).assertIsDisplayed()
        compose.onNodeWithText("caught up", substring = true).assertDoesNotExist()
    }
}
