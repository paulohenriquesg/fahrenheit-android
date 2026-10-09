package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The player's row of actions beside the transport (frame C of docs/mocks/player.html; #183). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayerActionsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val oneFile = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x")))

    private val episode = NowPlaying(
        itemId = "p1",
        title = "An Episode",
        timeline = oneFile,
        mediaDuration = null,
        chapters = null,
        episodeId = "e1",
        goToPodcast = true,
        description = null
    )

    private val twoChapters = listOf(Chapter(start = 0.0, end = 900.0, title = "Intro"), Chapter(start = 900.0, end = 1800.0, title = "Interview"))

    private val panels = PlayerPanels()

    private fun render(playing: NowPlaying) {
        val spans = ChapterClock.spans(playing.chapters, playing.trackTotal ?: 0.0)
        compose.setContent {
            FahrenheitTheme {
                Row {
                    PlayerActions(
                        panels = panels, speed = 1f, sleep = null, chapters = spans.isNotEmpty(),
                        onGoToPodcast = if (playing.goToPodcast) ({}) else null, favourite = null
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `an episode with chapters keeps Go to podcast and adds Chapters beside it`() {
        render(episode.copy(chapters = twoChapters))

        compose.onNodeWithTag(GO_TO_PODCAST_TAG).assertIsDisplayed()
        compose.onNodeWithText("Chapters").assertIsDisplayed()
    }

    @Test
    fun `an episode's Chapters opens the chapters panel`() {
        render(episode.copy(chapters = twoChapters))

        compose.onNodeWithText("Chapters").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(PlayerPanel.Chapters, panels.open)
    }

    @Test
    fun `an episode without chapters has Go to podcast and no Chapters`() {
        render(episode)

        compose.onNodeWithTag(GO_TO_PODCAST_TAG).assertIsDisplayed()
        compose.onNodeWithText("Chapters").assertDoesNotExist()
    }

    @Test
    fun `a book with chapters has Chapters and no Go to podcast`() {
        render(episode.copy(episodeId = null, goToPodcast = false, chapters = twoChapters))

        compose.onNodeWithText("Chapters").assertIsDisplayed()
        compose.onNodeWithTag(GO_TO_PODCAST_TAG).assertDoesNotExist()
    }
}
