package com.paulohenriquesg.fahrenheit.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.podcast.Fact
import com.paulohenriquesg.fahrenheit.ui.components.DESCRIPTION_BOX_TAG
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

    private val longBlurb = (1..30).joinToString("<br /><br />") { "Paragraph $it of a book's very long blurb, long enough to need a second line." }

    private fun renderBook(description: String) {
        compose.setContent {
            FahrenheitTheme {
                BookDetailView(
                    itemId = "b1",
                    content = DetailHeaderContent("A Book", null, emptyList(), "Play", description),
                    onPrimary = { pressed++ }
                )
            }
        }
        compose.waitForIdle()
    }

    // #134: the description scrolls in its own box, beside the actions.
    private fun scrollPosition(): Float =
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test
    fun `a book shows its whole description, paragraphs and all`() {
        renderBook("<p>First paragraph.</p><p>Second paragraph.</p>")

        compose.onNodeWithText("Second paragraph.", substring = true).assertExists()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `a description longer than its box is reached with Right and scrolled with Down`() {
        renderBook(longBlurb)

        compose.onNodeWithTag(PRIMARY_ACTION_TAG).performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()

        val before = scrollPosition()
        compose.onNodeWithTag(DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()

        assertTrue("Down scrolls the text", scrollPosition() > before)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Left from the description goes back to the button`() {
        renderBook(longBlurb)

        compose.onNodeWithTag(PRIMARY_ACTION_TAG).performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()

        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsFocused()
    }
}
