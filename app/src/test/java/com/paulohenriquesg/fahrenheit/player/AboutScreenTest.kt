package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.input.key.Key
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG
import com.paulohenriquesg.fahrenheit.ui.components.DESCRIPTION_BOX_TAG
import com.paulohenriquesg.fahrenheit.ui.components.FACTS_TAG
import com.paulohenriquesg.fahrenheit.ui.components.TITLE_TAG
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** About from the player: the book layout in place of the player, over its wash (#134). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class AboutScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val marked = mutableListOf<Boolean>()

    private val book = NowPlaying(
        itemId = "b1", title = "The Long Drift", timeline = null, mediaDuration = null, chapters = null,
        episodeId = null, goToPodcast = false, description = "<p>A survey ship drifts into a quiet sector.</p>",
        byline = "An Author · read by A Reader",
        facts = listOf(AboutFact(AboutFact.Kind.ReadBy, "A Reader")),
        finished = false
    )

    /** As the player does it: the screen or the player, and the panels' host for focus. */
    // #178: an episode, as NowPlaying.of makes one.
    private val episode = NowPlaying(
        itemId = "p1", title = "Episode 14: The Quiet Hour", timeline = null, mediaDuration = null, chapters = null,
        episodeId = "e14", goToPodcast = true, description = "<p>Two hosts talk about a quiet hour.</p>",
        show = "A Made-up Show",
        facts = listOf(
            AboutFact(AboutFact.Kind.Show, "A Made-up Show"),
            AboutFact(AboutFact.Kind.Published, "Yesterday"),
            AboutFact(AboutFact.Kind.Length, "30 min 0 s")
        )
    )

    private fun show(playing: NowPlaying = book, finished: Boolean? = false) {
        compose.setContent {
            FahrenheitTheme {
                val panels = rememberPlayerPanels()
                // As in the player: the chip stays, under the screen drawn over it.
                AboutChip(panels)
                if (panels.open == PlayerPanel.About) {
                    AboutScreen(
                        nowPlaying = playing,
                        wash = Color(0xFF24301A),
                        series = null,
                        finished = finished,
                        onPlayInstead = {},
                        onMarkFinished = { marked += it },
                        onClose = panels::close
                    )
                }
                PlayerPanelHost(panels) {}
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("About").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `about takes the screen, with the description focused`() {
        show()
        compose.onNodeWithText("The Long Drift", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Read by").assertIsDisplayed()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    @Test fun `mark finished is its action`() {
        show()
        compose.onNodeWithText("Mark finished").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(true), marked)
    }

    // Review Focus 3.
    @Test fun `back returns to the player, on the About chip`() {
        show()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertFalse(compose.activity.isFinishing)
        compose.onNodeWithText("About").assertIsFocused()
    }

    // Review (#134): focus must not wander to the player hidden under About.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `focus cannot leave About for the player underneath`() {
        show()
        compose.onNodeWithTag(DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    // #178: an episode's About is the same full-screen layout as a book's.
    @Test fun `an episode opens the full-screen About, its show under the title`() {
        show(episode, finished = null)
        compose.onNodeWithText("Episode 14: The Quiet Hour", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).assertIsDisplayed()
        // The byline, and again among the facts.
        assertEquals(2, compose.onAllNodesWithText("A Made-up Show").fetchSemanticsNodes().size)
    }

    @Test fun `an episode's facts include its show and length`() {
        show(episode, finished = null)
        compose.onNodeWithText("Show").assertIsDisplayed()
        compose.onNodeWithText("Length").assertIsDisplayed()
        compose.onNodeWithText("30 min 0 s").assertIsDisplayed()
    }

    @Test fun `an episode has no Mark finished`() {
        show(episode, finished = null)
        compose.onNodeWithText("Mark finished").assertDoesNotExist()
        compose.onNodeWithText("Mark unfinished").assertDoesNotExist()
    }

    @Test fun `an episode's About lands on its description`() {
        show(episode, finished = null)
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()
    }

    @Test fun `an episode with no description lands on its facts`() {
        show(episode.copy(description = " "), finished = null)
        compose.onNodeWithTag(FACTS_TAG).assertIsFocused()
    }

    @Test fun `back from an episode's About returns to the player, on the About chip`() {
        show(episode.copy(description = null), finished = null)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertFalse(compose.activity.isFinishing)
        compose.onNodeWithText("About").assertIsFocused()
    }

    // Review (#178): with nothing else to focus, focus stayed on the chip under About.
    @Test fun `an episode with no description and no facts lands on its title`() {
        show(episode.copy(description = null, facts = emptyList()), finished = null)
        compose.onNodeWithTag(TITLE_TAG).assertIsFocused()
    }
}
