package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Text
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
class PlayerScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var wentToPodcast = 0

    private fun playing(episode: Boolean) = NowPlaying(
        itemId = "p1",
        title = if (episode) "295 - The Book of Dale" else "Project Hail Mary",
        contentUrl = "/x",
        trackTotal = 1800.0,
        mediaDuration = null,
        chapters = null,
        episodeId = if (episode) "e295" else null,
        goToPodcast = episode,
        description = null,
        line = { if (episode) "Welcome to Night Vale · Yesterday" else "Chapter 2 · Andy Weir" }
    )

    private fun render(episode: Boolean) {
        compose.setContent {
            FahrenheitTheme {
                PlayerScreen(
                    nowPlaying = playing(episode),
                    currentTime = 0.0,
                    onGoToPodcast = { wentToPodcast++ },
                    transport = { Text("TRANSPORT") }
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `an episode shows its title, its show, and the transport`() {
        render(episode = true)

        compose.onNodeWithText("295 - The Book of Dale").assertIsDisplayed()
        compose.onNodeWithText("Welcome to Night Vale · Yesterday").assertIsDisplayed()
        compose.onNodeWithText("TRANSPORT").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `an episode offers its podcast`() {
        render(episode = true)

        compose.onNodeWithTag(GO_TO_PODCAST_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(GO_TO_PODCAST_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals(1, wentToPodcast)
    }

    @Test
    fun `a book does not`() {
        render(episode = false)

        compose.onNodeWithText("Chapter 2 · Andy Weir").assertIsDisplayed()
        compose.onNodeWithTag(GO_TO_PODCAST_TAG).assertDoesNotExist()
    }
}
