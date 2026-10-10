package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PodcastEpisodesViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val gson = Gson()
    private val onServer: Episode = gson.fromJson(
        """{"libraryItemId":"li","id":"s1","index":1,"title":"Tabstack","publishedAt":3,"addedAt":0,"updatedAt":0,"guid":"g1"}""",
        Episode::class.java
    )
    private fun feed(guid: String, title: String, at: Long): JsonObject = gson.fromJson(
        """{"title":"$title","guid":"$guid","publishedAt":$at,"enclosure":{"url":"https://cdn/$guid.mp3"}}""",
        JsonObject::class.java
    )
    private var feedEpisodes = listOf(feed("g1", "Tabstack", 3), feed("g2", "Supergood", 2))

    private var played: Episode? = null
    private var downloaded: EpisodeRow? = null
    private var tabChosen: EpisodeTab? = null
    private var marked: Pair<String, Boolean>? = null

    private fun render(
        tab: EpisodeTab = EpisodeTab.All,
        feed: FeedLoad = FeedLoad.Loaded(feedEpisodes),
        downloads: Map<String, DownloadState> = emptyMap(),
        progress: Map<String, EpisodeProgress> = emptyMap(),
        date: (EpisodeRow) -> String = { "" }
    ) {
        val screen = PodcastScreenModel.of(listOf(onServer), feed, tab, null, false, now = 10)
        compose.setContent {
            FahrenheitTheme {
                PodcastEpisodesView(
                    screen = screen,
                    tab = tab,
                    onTab = { tabChosen = it },
                    downloads = downloads,
                    onPlay = { played = it },
                    onDownload = { downloaded = it },
                    progress = progress,
                    onMark = { episode, finished -> marked = episode.id to finished },
                    date = date,
                    top = {
                        // As tall as the real one: Now playing and the description card.
                        Text("TOP", modifier = Modifier.height(200.dp).testTag("podcast_top"))
                    }
                )
            }
        }
        compose.waitForIdle()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    @Test
    fun `the tabs carry their counts, and a press chooses one`() {
        render()

        compose.onNodeWithText("Not downloaded · 1").assertIsDisplayed()
        press("episode_tab_NotDownloaded")

        assertEquals(EpisodeTab.NotDownloaded, tabChosen)
    }

    @Test
    fun `an episode on the server plays`() {
        render()

        press("episode_row_server:s1")

        assertEquals("s1", played?.id)
        assertNull(downloaded)
    }

    @Test
    fun `an episode only in the feed downloads, straight away`() {
        render()

        press("episode_row_feed:g2")

        assertEquals("Supergood", downloaded?.title)
        assertNull(played)
    }

    @Test
    fun `the focused missing episode says what a press will do`() {
        render()

        compose.onNodeWithTag("episode_row_feed:g2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithText("Download to server").assertIsDisplayed()
    }

    @Test
    fun `a download in progress is not started twice`() {
        render(downloads = mapOf("feed:g2" to DownloadState.Downloading))

        compose.onNodeWithText("Downloading…").assertIsDisplayed()
        press("episode_row_feed:g2")

        assertNull(downloaded)
    }

    @Test
    fun `a failed download can be tried again`() {
        render(downloads = mapOf("feed:g2" to DownloadState.Failed))

        compose.onNodeWithText("Download failed").assertIsDisplayed()
        press("episode_row_feed:g2")

        assertEquals("Supergood", downloaded?.title)
    }

    @Test
    fun `the note is shown when the list is not the whole story`() {
        render(feed = FeedLoad.Failed)

        compose.onNodeWithText("Could not read the feed. Showing what the server has.").assertIsDisplayed()
    }

    @Test
    fun `without a primary action in the header, the first episode holds focus`() {
        render()

        compose.onNodeWithTag("episode_row_server:s1").assertIsFocused()
    }

    @Test
    fun `on arrival the top of the list is there`() {
        render()

        compose.onNodeWithTag("podcast_top").assertIsDisplayed()
    }

    // The show's name stays in the left column now (#205), so only the tabs pin.
    @Test
    fun `moving into the list scrolls the top away and keeps the tabs`() {
        feedEpisodes = listOf(feed("g1", "Tabstack", 30)) + (1..20).map { feed("x$it", "Episode $it", 29L - it) }
        render()

        compose.onNodeWithTag("podcast_list").performScrollToIndex(12)
        compose.waitForIdle()

        compose.onNodeWithTag("podcast_top").assertDoesNotExist()
        compose.onNodeWithTag("episode_tab_All").assertIsDisplayed()
    }

    // As the web app says it, in the row's line (#181).
    @Test
    fun `a finished episode says finished in its line`() {
        render(progress = mapOf("s1" to EpisodeProgress.Heard))

        compose.onNodeWithText("finished").assertIsDisplayed()
        compose.onNodeWithText("Heard").assertDoesNotExist()
    }

    @Test
    fun `a half-heard episode says how long is left, and resumes`() {
        render(progress = mapOf("s1" to EpisodeProgress.InProgress(fraction = 0.44, secondsLeft = 840.0)))

        compose.onNodeWithText("14m left", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Resume").assertIsDisplayed()
    }

    // An episode on the server shows a play icon, not the word "Play".
    @Test
    fun `an episode on the server offers a play icon rather than the word`() {
        render()

        compose.onNodeWithContentDescription("Play").assertIsDisplayed()
        compose.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun `an episode not on the server still says so in words`() {
        render()

        compose.onNodeWithText("Not downloaded").assertIsDisplayed()
    }

    // A tick read as "heard", so it means heard; being on the server is what
    // the play icon already says.
    @Test
    fun `a downloaded episode not yet finished carries no tick`() {
        render()

        compose.onNodeWithContentDescription("Finished").assertDoesNotExist()
    }

    @Test
    fun `a finished episode carries the tick`() {
        render(progress = mapOf("s1" to EpisodeProgress.Heard))

        compose.onNodeWithContentDescription("Finished").assertIsDisplayed()
    }

    @Test
    fun `an episode not on the server carries the download mark`() {
        render()

        compose.onNodeWithContentDescription("Not on the server").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Right from the focused row reaches Mark finished, and Left comes back`() {
        render()
        compose.onNodeWithTag("episode_row_server:s1").assertIsFocused()

        compose.onNodeWithTag("episode_row_server:s1").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        compose.onNodeWithTag("episode_mark_server:s1").assertIsFocused()
        compose.onNodeWithContentDescription("Mark finished").assertIsDisplayed()

        compose.onNodeWithTag("episode_mark_server:s1").performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()
        compose.onNodeWithTag("episode_row_server:s1").assertIsFocused()
    }

    @Test
    fun `the button is only on the focused row, and never on one the server lacks`() {
        render()
        compose.onNodeWithTag("episode_mark_server:s1").assertExists()

        compose.onNodeWithTag("episode_row_feed:g2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag("episode_mark_server:s1").assertDoesNotExist()
        compose.onNodeWithTag("episode_mark_feed:g2").assertDoesNotExist()
    }

    @Test
    fun `Mark finished asks for the episode to be finished, and the row still plays`() {
        render()

        compose.onNodeWithTag("episode_mark_server:s1").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals("s1" to true, marked)
        assertNull(played)
    }

    @Test
    fun `on a finished episode it reads Mark unfinished, and asks for that`() {
        render(progress = mapOf("s1" to EpisodeProgress.Heard))

        compose.onNodeWithContentDescription("Mark unfinished").assertExists()
        compose.onNodeWithTag("episode_mark_server:s1").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals("s1" to false, marked)
    }

    // Up and Down still walk the rows from the button (#181).
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Down from the button goes to the next row`() {
        render()
        compose.onNodeWithTag("episode_row_server:s1").performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()

        compose.onNodeWithTag("episode_mark_server:s1").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()

        compose.onNodeWithTag("episode_row_feed:g2").assertIsFocused()
    }

    // #205: the play mark says what Center does, so only where Center would do it.
    @Test
    fun `the play mark is only on the focused row`() {
        render()
        compose.onNodeWithContentDescription("Play").assertIsDisplayed()

        compose.onNodeWithTag("episode_row_feed:g2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Play").assertDoesNotExist()
    }

    @Test
    fun `a finished row is dimmed until it has focus`() {
        render(progress = mapOf("s1" to EpisodeProgress.Heard))
        compose.onNodeWithTag("episode_row_server:s1").assert(SemanticsMatcher.expectValue(EpisodeRowDimmed, false))

        compose.onNodeWithTag("episode_row_feed:g2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag("episode_row_server:s1").assert(SemanticsMatcher.expectValue(EpisodeRowDimmed, true))
        compose.onNodeWithTag("episode_row_feed:g2").assert(SemanticsMatcher.expectValue(EpisodeRowDimmed, false))
    }

    @Test
    fun `a row's line reads date, then the rest, apart by dots`() {
        render(progress = mapOf("s1" to EpisodeProgress.Heard), date = { "Yesterday" })

        compose.onNodeWithText("Yesterday · finished").assertIsDisplayed()
    }
}
