package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The player's own frame while the item loads (#223; frames 1 and 3 of
 * docs/mocks/player-loading.html), rather than "Loading…" on black. Drawn
 * still here: its sweep and spinner never end, and a test holding them would
 * never go idle.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayerLoadingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(preview: PlayerPreview?) {
        compose.setContent { FahrenheitTheme { PlayerLoading(itemId = "b1", preview = preview, animated = false) } }
        compose.waitForIdle()
    }

    @Test
    fun `the preview's title and kicker are drawn, and nothing takes focus`() {
        show(PlayerPreview("An Invented Book", kicker = "An Invented Writer"))

        compose.onNodeWithText("An Invented Book").assertExists()
        compose.onNodeWithText("An Invented Writer", ignoreCase = true).assertExists()
        compose.onNodeWithText("Starting…").assertExists()
        compose.onNodeWithContentDescription("Play").assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).assertCountEquals(0)
    }

    @Test
    fun `a book's author is drawn under its title`() {
        show(PlayerPreview("An Invented Book", byline = "An Invented Writer"))

        compose.onNodeWithText("An Invented Writer").assertExists()
    }

    @Test
    fun `with no preview, placeholders hold the layout instead of Loading on black`() {
        show(null)

        compose.onNodeWithTag(PlayerLoadingTags.FRAME).assertExists()
        compose.onNodeWithTag(PlayerLoadingTags.COVER_PLACEHOLDER).assertExists()
        compose.onAllNodesWithTag(PlayerLoadingTags.LINE_PLACEHOLDER).assertCountEquals(3)
        compose.onNodeWithText("Starting…").assertExists()
        compose.onNodeWithText("Loading…").assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).assertCountEquals(0)
    }

    @Test
    fun `with a preview, grey lines stand where the description goes`() {
        show(PlayerPreview("An Invented Book"))

        compose.onNodeWithTag(PlayerLoadingTags.COVER_PLACEHOLDER).assertDoesNotExist()
        compose.onAllNodesWithTag(PlayerLoadingTags.LINE_PLACEHOLDER).assertCountEquals(2)
    }
}
