package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcast
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcastMetadata
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.podcast.LatestEpisodesView
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * docs/mocks/list-placeholders.html (#224): while a list loads, its screen draws
 * still grey cards in the shape of what is coming, under its real title, where
 * it used to say "Loading…" on an empty screen. Nothing there takes focus, so
 * the rail keeps it. Empty and failed loads read as they did.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ListPlaceholdersTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(content: @Composable () -> Unit) {
        compose.setContent { FahrenheitTheme { content() } }
        compose.waitForIdle()
    }

    private fun placeholders() =
        compose.onAllNodesWithTag(PlaceholderTags.SHAPE).fetchSemanticsNodes().size +
            compose.onAllNodesWithTag(PlaceholderTags.ROUND).fetchSemanticsNodes().size

    /** Placeholders under the real title, nothing focusable, no loading line. */
    private fun assertLoadingLooksRight(title: String) {
        compose.onNodeWithText(title).assertIsDisplayed()
        assertTrue("no placeholders drawn", placeholders() > 0)
        compose.onAllNodes(isFocusable()).assertCountEquals(0)
        compose.onAllNodes(hasText("Loading", substring = true)).assertCountEquals(0)
    }

    private fun assertNoPlaceholders() = assertTrue("placeholders left behind", placeholders() == 0)

    private val series = Series(id = "s1", name = "An Invented Series")
    private val collection = Collection(id = "c1", libraryId = "lib", name = "An Invented Collection", lastUpdate = 0, createdAt = 0)
    private val author = Author(id = "a1", name = "Someone Invented")
    private val episode = RecentPodcastEpisode(
        id = "ep-1",
        libraryItemId = "pod-1",
        title = "An Invented Episode",
        description = null,
        podcast = RecentEpisodePodcast(metadata = RecentEpisodePodcastMetadata(title = "An Invented Show"))
    )

    // Home

    @Test
    fun `Home draws shelves of placeholders while it loads`() {
        render { PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = true) }
        assertLoadingLooksRight("Home")
    }

    @Test
    fun `Home's shelves replace the placeholders`() {
        render { PersonalizedHomeView(shelves = listOf(TestFixtures.createMockShelf()), libraryId = "lib", isLoading = true) }
        assertNoPlaceholders()
        compose.onNodeWithText("Continue Listening").assertIsDisplayed()
    }

    @Test
    fun `an empty Home that has loaded draws no placeholders`() {
        render { PersonalizedHomeView(shelves = emptyList(), libraryId = "lib", isLoading = false) }
        assertNoPlaceholders()
    }

    // Library

    @Test
    fun `Library draws a grid of placeholders while it loads, and counts nothing`() {
        render {
            LibraryBrowseView(
                name = "An Invented Library", itemLabel = "books", items = emptyList(),
                rowLayout = false, listState = rememberLazyListState(), loading = true
            )
        }
        assertLoadingLooksRight("An Invented Library")
        compose.onNodeWithText("0 books", substring = true).assertDoesNotExist()
    }

    @Test
    fun `Library in rows draws placeholders while it loads`() {
        render {
            LibraryBrowseView(
                name = "An Invented Library", itemLabel = "books", items = emptyList(),
                rowLayout = true, listState = rememberLazyListState(), loading = true
            )
        }
        assertLoadingLooksRight("An Invented Library")
    }

    @Test
    fun `Library's items replace the placeholders`() {
        render {
            LibraryBrowseView(
                name = "An Invented Library", itemLabel = "books",
                items = listOf(TestFixtures.createMockLibraryItemEntry()),
                rowLayout = false, listState = rememberLazyListState(), loading = false
            )
        }
        assertNoPlaceholders()
        compose.onNodeWithText("(1 books)", substring = true).assertIsDisplayed()
    }

    @Test
    fun `Library refreshing what it already shows keeps its items`() {
        render {
            LibraryBrowseView(
                name = "An Invented Library", itemLabel = "books",
                items = listOf(TestFixtures.createMockLibraryItemEntry()),
                rowLayout = false, listState = rememberLazyListState(), loading = true
            )
        }
        assertNoPlaceholders()
        compose.onNodeWithText("(1 books)", substring = true).assertIsDisplayed()
    }

    // Series

    @Test
    fun `Series draws placeholders while it loads`() {
        render { SeriesBrowseView(seriesList = emptyList(), isLoading = true) }
        assertLoadingLooksRight("Series")
    }

    @Test
    fun `Series replace the placeholders`() {
        render { SeriesBrowseView(seriesList = listOf(series), isLoading = false) }
        assertNoPlaceholders()
        compose.onNodeWithText("An Invented Series").assertIsDisplayed()
    }

    @Test
    fun `no series still says so`() {
        render { SeriesBrowseView(seriesList = emptyList(), isLoading = false) }
        assertNoPlaceholders()
        compose.onNodeWithText("No series found", substring = true).assertIsDisplayed()
    }

    // Collections

    @Test
    fun `Collections draws placeholders while it loads`() {
        render { CollectionsBrowseView(collectionsList = emptyList(), isLoading = true) }
        assertLoadingLooksRight("Collections")
    }

    @Test
    fun `Collections replace the placeholders`() {
        render { CollectionsBrowseView(collectionsList = listOf(collection), isLoading = false) }
        assertNoPlaceholders()
        compose.onNodeWithText("An Invented Collection").assertIsDisplayed()
    }

    @Test
    fun `no collections still says so`() {
        render { CollectionsBrowseView(collectionsList = emptyList(), isLoading = false) }
        assertNoPlaceholders()
        compose.onNodeWithText("No collections found", substring = true).assertIsDisplayed()
    }

    // Authors

    @Test
    fun `Authors draws round placeholders while it loads`() {
        render { AuthorsBrowseView(libraryId = "lib", load = { awaitCancellation() }) }
        assertLoadingLooksRight("Authors")
        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.ROUND).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `Authors replace the placeholders when they arrive`() {
        val authors = CompletableDeferred<Result<List<Author>>>()
        render { AuthorsBrowseView(libraryId = "lib", load = { authors.await() }) }
        assertTrue(placeholders() > 0)

        authors.complete(Result.success(listOf(author)))
        compose.waitForIdle()

        assertNoPlaceholders()
        compose.onNodeWithText("Someone Invented").assertIsDisplayed()
    }

    @Test
    fun `Authors that could not load still read as none`() {
        render { AuthorsBrowseView(libraryId = "lib", load = { Result.failure(IllegalStateException("offline")) }) }
        assertNoPlaceholders()
        compose.onNodeWithText("No authors found", substring = true).assertIsDisplayed()
    }

    // Latest Episodes

    @Test
    fun `Latest Episodes draws placeholder rows while it loads`() {
        render { LatestEpisodesView("lib", load = { awaitCancellation() }) }
        assertLoadingLooksRight("Latest Episodes")
    }

    @Test
    fun `Latest Episodes replace the placeholders when they arrive`() {
        val episodes = CompletableDeferred<Result<List<RecentPodcastEpisode>>>()
        render { LatestEpisodesView("lib", load = { episodes.await() }) }
        assertTrue(placeholders() > 0)

        episodes.complete(Result.success(listOf(episode)))
        compose.waitForIdle()

        assertNoPlaceholders()
        compose.onNodeWithText("An Invented Episode").assertIsDisplayed()
    }

    @Test
    fun `Latest Episodes that could not load still say so`() {
        render { LatestEpisodesView("lib", load = { Result.failure(IllegalStateException("offline")) }) }
        assertNoPlaceholders()
        compose.onNodeWithText("Could not load recent episodes", substring = true).assertIsDisplayed()
    }
}
