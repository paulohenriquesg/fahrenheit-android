package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG
import com.paulohenriquesg.fahrenheit.ui.components.DESCRIPTION_BOX_TAG
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The podcast page in the book page's layout (#205). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PodcastPageTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val gson = Gson()
    private val newest: Episode = gson.fromJson(
        """{"libraryItemId":"p1","id":"s1","title":"First Steps","publishedAt":40,"guid":"g0"}""",
        Episode::class.java
    )
    private fun feed(guid: String, title: String, at: Long): JsonObject = gson.fromJson(
        """{"title":"$title","guid":"$guid","publishedAt":$at,"enclosure":{"url":"https://cdn.example/$guid.mp3"}}""",
        JsonObject::class.java
    )

    private val margin = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    private var checks = 0
    private var primaries = 0
    private val changes = mutableListOf<DownloadChange>()
    private var closed = 0

    private fun state(
        title: String = "The Show",
        episodes: Int = 20,
        admin: Boolean = true,
        settings: DownloadSettings? = DownloadSettings(false, "0 0 * * *", 0, 3),
        server: List<Episode> = listOf(newest),
        playlist: String? = null
    ): PodcastUiState {
        val feed = FeedLoad.Loaded((1..episodes).map { feed("g$it", "Episode $it", 39L - it) })
        return PodcastUiState(
            itemId = "p1",
            title = title,
            byline = "Podcast · Talk",
            description = "<p>${"A show about many things, told slowly. ".repeat(12)}</p>",
            primary = "Resume First Steps",
            primaryEpisode = newest,
            screen = PodcastScreenModel.of(
                server, if (admin) feed else FeedLoad.Unavailable, EpisodeTab.All, 0, null, now = 10,
                favourites = playlist?.let { emptySet() }
            ),
            tab = EpisodeTab.All,
            feedCheck = if (admin) FeedCheckState.Idle else null,
            autoDownloads = settings,
            favouritesPlaylist = playlist
        )
    }

    private fun show(state: PodcastUiState = state(), nowPlaying: @Composable () -> Unit = {}) {
        compose.setContent {
            FahrenheitTheme {
                PodcastPage(
                    state = state,
                    margin = margin,
                    onPrimary = { primaries++ },
                    onTab = {},
                    onPlay = {},
                    onDownload = {},
                    onMark = { _, _ -> },
                    onCheckFeed = { checks++ },
                    onChangeDownloads = { changes += it },
                    onDownloadsClosed = { closed++ },
                    nowPlaying = nowPlaying
                )
            }
        }
        compose.waitForIdle()
    }

    private fun press(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun `the primary holds focus on arrival, and plays`() {
        show()

        compose.onNodeWithTag(PODCAST_PRIMARY_TAG).assertIsFocused()
        press(PODCAST_PRIMARY_TAG)
        assertEquals(1, primaries)
    }

    @Test
    fun `the left column stays put while the episodes scroll`() {
        show()
        val before = compose.onNodeWithTag(PODCAST_LEFT_TAG).getUnclippedBoundsInRoot()

        compose.onNodeWithTag("podcast_list").performScrollToIndex(15)
        compose.waitForIdle()

        compose.onNodeWithTag(PODCAST_DESCRIPTION_TAG).assertDoesNotExist()
        assertEquals(before, compose.onNodeWithTag(PODCAST_LEFT_TAG).getUnclippedBoundsInRoot())
    }

    @Test
    fun `the facts sit at the foot of the left column, under its buttons`() {
        show()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val facts = compose.onNodeWithTag(PODCAST_FACTS_TAG).getUnclippedBoundsInRoot()
        val downloads = compose.onNodeWithTag(PODCAST_DOWNLOADS_TAG).getUnclippedBoundsInRoot()

        compose.onNodeWithText("1 of 21 on the server").assertIsDisplayed()
        assertTrue("facts $facts, screen $screen", screen.bottom - 16.dp - facts.bottom < 2.dp)
        assertTrue("facts $facts, downloads $downloads", facts.top >= downloads.bottom)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Left from any row lands on the primary`() {
        show()
        val row = "episode_row_feed:g3"
        compose.onNodeWithTag(row).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag(row).performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()

        compose.onNodeWithTag(PODCAST_PRIMARY_TAG).assertIsFocused()
    }

    @Config(qualifiers = "w960dp-h540dp", fontScale = 1.3f)
    @Test
    fun `under a long title at a large font every button and the facts stay on screen`() {
        show(state(title = "A Very Long Name for a Show About Everything That Happens, Told at Length by Two Hosts"))
        val inside = compose.onRoot().getUnclippedBoundsInRoot().bottom - 16.dp

        listOf(PODCAST_PRIMARY_TAG, FEED_CHECK_TAG, PODCAST_DOWNLOADS_TAG, PODCAST_FACTS_TAG).forEach { tag ->
            val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag ends at ${bounds.bottom}, the margin at $inside", bounds.bottom <= inside)
        }
    }

    @Test
    fun `the feed check says what it will fetch, under its name`() {
        show()

        compose.onNodeWithText("Up to 3 to the server").assertIsDisplayed()
        press(FEED_CHECK_TAG)
        assertEquals(1, checks)
    }

    @Test
    fun `automatic downloads says its state, opens the panel, and focus comes back to it`() {
        show()
        compose.onNodeWithText("Off").assertIsDisplayed()

        compose.onNodeWithTag(PODCAST_DOWNLOADS_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        press(PODCAST_DOWNLOADS_TAG)
        compose.onNodeWithTag("downloads_enabled").assertIsFocused()
        press("downloads_enabled")
        assertEquals(listOf<DownloadChange>(DownloadChange.Enabled(true)), changes)

        back()
        compose.onNodeWithTag("downloads_enabled").assertDoesNotExist()
        compose.onNodeWithTag(PODCAST_DOWNLOADS_TAG).assertIsFocused()
        assertEquals(1, closed)
    }

    @Test
    fun `without the right or the role, neither button is there`() {
        show(state(admin = false, settings = null))

        compose.onNodeWithTag(FEED_CHECK_TAG).assertDoesNotExist()
        compose.onNodeWithTag(PODCAST_DOWNLOADS_TAG).assertDoesNotExist()
        compose.onNodeWithTag(PODCAST_PRIMARY_TAG).assertIsFocused()
    }

    @Test
    fun `the description opens full screen, and Back returns to it`() {
        show()
        compose.onNodeWithTag(PODCAST_DESCRIPTION_TAG).performSemanticsAction(SemanticsActions.RequestFocus)

        press(PODCAST_DESCRIPTION_TAG)
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).assertIsDisplayed()
        compose.onNodeWithTag(DESCRIPTION_TAG).assertIsFocused()

        back()
        compose.onNodeWithTag(DESCRIPTION_BOX_TAG).assertDoesNotExist()
        compose.onNodeWithTag(PODCAST_DESCRIPTION_TAG).assertIsFocused()
    }

    // #159, moved from the header: at the top of the right column, scrolling
    // away with the description rather than fixed over the episodes.
    @Test
    fun `Now playing sits beside the cover, above the description`() {
        show(nowPlaying = { BasicText("bar", Modifier.testTag("bar").width(450.dp).height(56.dp)) })
        val bar = compose.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val cover = compose.onNodeWithContentDescription("The Show").getUnclippedBoundsInRoot()
        val description = compose.onNodeWithTag(PODCAST_DESCRIPTION_TAG).getUnclippedBoundsInRoot()

        assertTrue("bar $bar, cover $cover", bar.left >= cover.right)
        assertTrue("bar $bar, description $description", bar.bottom <= description.top)
    }

    private fun episode(id: String, title: String, at: Long): Episode = gson.fromJson(
        """{"libraryItemId":"p1","id":"$id","title":"$title","publishedAt":$at,"guid":"g$id"}""",
        Episode::class.java
    )

    private fun assertFocusedTab() = compose.onNode(isFocused()).assert(
        SemanticsMatcher("is a tab") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("episode_tab_") == true }
    )

    // Device check (#211): from the heart at the far right nothing sits right
    // above, and Up skipped the tabs for the button beyond them. Here the
    // description, full width, is what sits above it: an admin's page, with
    // the feed read and so no note between it and the tabs.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Up from a row's buttons goes where Up from the row goes`() {
        show(state(server = listOf(newest, episode("s2", "Second Wind", 39)), playlist = "Bedtime"))

        listOf("episode_mark_server:s1", "episode_favourite_server:s1").forEach { button ->
            compose.onNodeWithTag("episode_row_server:s1").performSemanticsAction(SemanticsActions.RequestFocus)
            compose.waitForIdle()
            compose.onNodeWithTag(button).performSemanticsAction(SemanticsActions.RequestFocus)
            compose.waitForIdle()
            compose.onNodeWithTag(button).performKeyInput { pressKey(Key.DirectionUp) }
            compose.waitForIdle()
            assertFocusedTab()
        }

        compose.onNodeWithTag("episode_row_server:s2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onNodeWithTag("episode_favourite_server:s2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onNodeWithTag("episode_favourite_server:s2").performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithTag("episode_row_server:s1").assertIsFocused()
    }

    // Review: Left out of the list from what is not a row, through the column's exit.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Left from the description lands on the primary too`() {
        show()
        compose.onNodeWithTag(PODCAST_DESCRIPTION_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag(PODCAST_DESCRIPTION_TAG).performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()

        compose.onNodeWithTag(PODCAST_PRIMARY_TAG).assertIsFocused()
    }

    private fun lines(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    // Device check of #213: at 180dp "Resume <episode>" was cut short and
    // the two-line buttons wrapped to three. Each is its label on one line,
    // then its state (#205: about 240dp, as the mock draws it).
    @Test
    fun `each button's label fits on one line, and Resume shows the episode`() {
        show(state().copy(primary = "Resume Episode Eight"))

        listOf("Check for new episodes", "Automatic downloads").forEach { label ->
            assertEquals(label, 1, lines(label).lineCount)
        }
        assertFalse("Resume is cut", lines("Resume Episode Eight").hasVisualOverflow)
    }
}
