package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG
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
    private fun show() {
        compose.setContent {
            FahrenheitTheme {
                val panels = rememberPlayerPanels()
                if (panels.open == PlayerPanel.About) {
                    AboutScreen(
                        nowPlaying = book,
                        wash = Color(0xFF24301A),
                        series = null,
                        finished = false,
                        onPlayInstead = {},
                        onMarkFinished = { marked += it },
                        onClose = panels::close
                    )
                } else {
                    AboutChip(panels)
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
}
