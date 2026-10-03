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

    @Test
    fun `the podcast header keeps a short preview`() {
        render()

        compose.onNodeWithTag(DESCRIPTION_TAG).assertDoesNotExist()
    }

    // Seen on the stick with NerdCast: "Resume <a long episode title>" took the
    // whole row, the feed check beside it was squeezed to zero width, and its
    // text wrapped one letter per line - pushing everything below off screen.
    @Test
    fun `a long primary action leaves room for the others and keeps the header on screen`() {
        compose.setContent {
            FahrenheitTheme {
                DetailHeader(
                    itemId = "p1",
                    content = DetailHeaderContent(
                        title = "NerdCast",
                        byline = "Podcast · Society & Culture",
                        chips = listOf(Fact("1087 of 1736 on the server")),
                        primary = "Resume NerdCast 1048 - O Segredo de Widow's Bay: A Melhor Série do Ano! Azaghal Cravou!",
                        description = "Lambda lambda lambda, nerds!"
                    ),
                    onPrimary = {},
                    actions = {
                        com.paulohenriquesg.fahrenheit.podcast.FeedCheckRow(
                            state = com.paulohenriquesg.fahrenheit.podcast.FeedCheckState.Idle,
                            onCheck = {}
                        )
                    }
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Check for new episodes").assertIsDisplayed()
        compose.onNodeWithText("Lambda lambda lambda, nerds!").assertIsDisplayed()
        val height = compose.onRoot().fetchSemanticsNode().size.height / compose.density.density
        assertTrue("header is ${height}dp tall", height < 540f)
    }
}
