package com.paulohenriquesg.fahrenheit.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.podcast.Fact
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class DetailHeaderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var pressed = 0

    private fun render(primary: String? = "Resume at 5h 28m") {
        compose.setContent {
            FahrenheitTheme {
                DetailHeader(
                    itemId = "b1",
                    content = DetailHeaderContent(
                        title = "Project Hail Mary",
                        byline = "Andy Weir · narrated by Ray Porter",
                        chips = listOf(Fact("16h 10m"), Fact("34% in")),
                        primary = primary,
                        description = "<p>Ryland Grace is the <b>sole</b> survivor</p>"
                    ),
                    onPrimary = { pressed++ },
                    actions = { Text("EXTRA") }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `it shows the title, by-line, chips and description`() {
        render()

        compose.onNodeWithText("Project Hail Mary").assertIsDisplayed()
        compose.onNodeWithText("Andy Weir · narrated by Ray Porter").assertIsDisplayed()
        compose.onNodeWithText("34% in").assertIsDisplayed()
        // Rendered, not printed with its tags (#57).
        compose.onNodeWithText("Ryland Grace is the sole survivor").assertIsDisplayed()
    }

    @Test
    fun `the primary action holds focus on arrival, so one press continues`() {
        render()

        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `pressing it runs the primary action`() {
        render()

        compose.onNodeWithTag(PRIMARY_ACTION_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals(1, pressed)
    }

    @Test
    fun `other actions sit beside it`() {
        render()

        compose.onNodeWithText("EXTRA").assertIsDisplayed()
    }

    @Test
    fun `with nothing to play there is no primary button`() {
        render(primary = null)

        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
    }
}
