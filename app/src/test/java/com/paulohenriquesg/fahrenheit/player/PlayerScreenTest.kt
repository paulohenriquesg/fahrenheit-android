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
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The player in layout C (#107; frame "C…" of docs/mocks/player.html): the
 * cover with what you read beside it, and the transport under them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayerScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val oneFile = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x")))

    private val book = NowPlaying(
        itemId = "b1",
        title = "A Long Way Home",
        timeline = oneFile,
        mediaDuration = null,
        chapters = listOf(Chapter(start = 0.0, end = 600.0, title = "Chapter 1"), Chapter(start = 600.0, end = 1800.0, title = "Chapter 2")),
        episodeId = null,
        goToPodcast = false,
        description = null,
        kicker = "The Long Way · Book 2",
        byline = "An Author · read by A Reader"
    )

    private val episode = NowPlaying(
        itemId = "p1",
        title = "295 - The Book of Dale",
        timeline = oneFile,
        mediaDuration = null,
        chapters = null,
        episodeId = "e295",
        goToPodcast = true,
        description = null,
        kicker = "Welcome to Night Vale · Yesterday"
    )

    private fun render(nowPlaying: NowPlaying, currentTime: Double) {
        compose.setContent {
            FahrenheitTheme {
                PlayerScreen(nowPlaying = nowPlaying, currentTime = currentTime, transport = { Text("TRANSPORT") })
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `a book shows its series, title, byline and the chapter playing`() {
        render(book, currentTime = 700.0)

        compose.onNodeWithText("THE LONG WAY · BOOK 2").assertIsDisplayed()
        compose.onNodeWithText("A Long Way Home").assertIsDisplayed()
        compose.onNodeWithText("An Author · read by A Reader").assertIsDisplayed()
        compose.onNodeWithText("Chapter 2").assertIsDisplayed()
        compose.onNodeWithText("TRANSPORT").assertIsDisplayed()
    }

    @Test
    fun `the chapter follows the position`() {
        render(book, currentTime = 100.0)

        compose.onNodeWithText("Chapter 1").assertIsDisplayed()
    }

    @Test
    fun `an episode shows its show and date above its title`() {
        render(episode, currentTime = 0.0)

        compose.onNodeWithText("WELCOME TO NIGHT VALE · YESTERDAY").assertIsDisplayed()
        compose.onNodeWithText("295 - The Book of Dale").assertIsDisplayed()
    }

    // The description moves to About (step 3 of #107).
    @Test
    fun `the description is not on the player screen`() {
        render(book.copy(description = "<p>A long description</p>"), currentTime = 0.0)

        compose.onNodeWithText("A long description", substring = true).assertDoesNotExist()
    }

    // Review Focus 5: Go to podcast now sits beside the transport.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `an episode offers its podcast beside the transport`() {
        var went = 0
        compose.setContent { FahrenheitTheme { GoToPodcastButton { went++ } } }

        compose.onNodeWithTag(GO_TO_PODCAST_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(GO_TO_PODCAST_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals(1, went)
    }

    // Review: upper-casing in the device's language turns "i" into "İ" in Turkish.
    @Test
    fun `the line above the title is upper-cased the same in every language`() {
        val before = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr"))
        try {
            render(book.copy(kicker = "Science fiction"), currentTime = 0.0)
            compose.onNodeWithText("SCIENCE FICTION").assertIsDisplayed()
        } finally {
            java.util.Locale.setDefault(before)
        }
    }

    @Test
    fun `with a wash the screen still shows everything`() {
        compose.setContent {
            FahrenheitTheme {
                PlayerScreen(nowPlaying = book, currentTime = 700.0, wash = Color(0xFF24301A), transport = { Text("TRANSPORT") })
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Chapter 2").assertIsDisplayed()
        compose.onNodeWithText("TRANSPORT").assertIsDisplayed()
    }

    @Test
    fun `an open panel is drawn over the screen`() {
        compose.setContent {
            FahrenheitTheme {
                PlayerScreen(nowPlaying = book, currentTime = 700.0, transport = { Text("TRANSPORT") }, overlay = { Text("PANEL") })
            }
        }
        compose.onNodeWithText("PANEL").assertIsDisplayed()
        compose.onNodeWithText("TRANSPORT").assertIsDisplayed()
    }

    private val middleEpisode = episode.copy(
        badge = "Bonus",
        details = "Season 2 · Episode 295 · 30 min",
        notes = "Dale has written a book, and the whole town is reading it.",
        next = EpisodeRef("e296", "296 - A Sad Song", 1680.0)
    )

    @Test
    fun `an episode shows its badge, details and notes under the title`() {
        render(middleEpisode, currentTime = 0.0)
        compose.onNodeWithText("BONUS").assertIsDisplayed()
        compose.onNodeWithText("Season 2 · Episode 295 · 30 min").assertIsDisplayed()
        compose.onNodeWithText("Dale has written a book, and the whole town is reading it.").assertIsDisplayed()
    }

    @Test
    fun `up next names what Next plays`() {
        render(middleEpisode, currentTime = 0.0)
        compose.onNodeWithText("Up next · 296 - A Sad Song · 28 min").assertIsDisplayed()
    }

    // A short one keeps its seconds, as every length does under ten minutes (#170).
    @Test
    fun `up next under ten minutes keeps its seconds`() {
        render(middleEpisode.copy(next = EpisodeRef("e296", "296 - A Sad Song", 545.0)), currentTime = 0.0)
        compose.onNodeWithText("Up next · 296 - A Sad Song · 9 min 5 s").assertIsDisplayed()
    }

    @Test
    fun `the newest episode has no up next`() {
        render(middleEpisode.copy(next = null), currentTime = 0.0)
        compose.onNodeWithText("Up next", substring = true).assertDoesNotExist()
    }
}
