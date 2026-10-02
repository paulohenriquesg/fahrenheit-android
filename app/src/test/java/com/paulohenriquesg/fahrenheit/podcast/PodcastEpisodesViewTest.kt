package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
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

    private fun render(
        tab: EpisodeTab = EpisodeTab.All,
        feed: FeedLoad = FeedLoad.Loaded(feedEpisodes),
        downloads: Map<String, DownloadState> = emptyMap()
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
                    title = "The Show",
                    header = {
                        // As tall as the real one: a cover and a description.
                        Text("HEADER", modifier = Modifier.height(260.dp).testTag("podcast_header"))
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
    fun `on arrival the header is there and the pinned title is not`() {
        render()

        compose.onNodeWithTag("podcast_header").assertIsDisplayed()
        compose.onNodeWithTag("pinned_title").assertDoesNotExist()
    }

    @Test
    fun `moving into the list scrolls the header away and keeps the tabs and the name`() {
        feedEpisodes = listOf(feed("g1", "Tabstack", 30)) + (1..20).map { feed("x$it", "Episode $it", 29L - it) }
        render()

        compose.onNodeWithTag("podcast_list").performScrollToIndex(12)
        compose.waitForIdle()

        compose.onNodeWithTag("podcast_header").assertDoesNotExist()
        compose.onNodeWithTag("episode_tab_All").assertIsDisplayed()
        compose.onNodeWithTag("pinned_title").assertIsDisplayed()
        compose.onNodeWithText("The Show").assertIsDisplayed()
    }
}
