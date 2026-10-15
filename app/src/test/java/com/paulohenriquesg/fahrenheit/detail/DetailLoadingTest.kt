package com.paulohenriquesg.fahrenheit.detail

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.ui.components.TITLE_TAG
import com.paulohenriquesg.fahrenheit.ui.elements.CoverTags
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The book and podcast pages while the item loads (#225; docs/mocks/
 * detail-loading.html): what the card showed, in its place, and still grey
 * shapes for the rest, rather than "Loading…" on black.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class DetailLoadingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // A test below changes Compose state from outside a composition; later
    // Compose tests in this JVM must not inherit work it left queued (#221).
    @After
    fun drain() = shadowOf(Looper.getMainLooper()).idle()

    private val margin = PaddingValues(horizontal = 24.dp, vertical = 16.dp)

    private fun show(preview: PlayerPreview?, podcast: Boolean = false) {
        compose.setContent { FahrenheitTheme { DetailLoading("b1", preview, podcast, margin) } }
        compose.waitForIdle()
    }

    private fun assertNothingFocusable() =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).assertCountEquals(0)

    private fun assertSomePlaceholders() =
        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.SHAPE).fetchSemanticsNodes().isNotEmpty())

    @Test
    fun `a book's frame shows the card's title, author and cover, and nothing takes focus`() {
        show(PlayerPreview("An Invented Book", coverUrl = "https://abs.test/api/items/b1/cover", byline = "An Invented Writer"))

        compose.onNodeWithText("An Invented Book").assertExists()
        compose.onNodeWithText("An Invented Writer").assertExists()
        compose.onNodeWithContentDescription("An Invented Book").assertExists()
        compose.onNodeWithText("Loading…").assertDoesNotExist()
        assertSomePlaceholders()
        assertNothingFocusable()
    }

    @Test
    fun `a card that has no cover hands over the title placeholder it shows`() {
        show(PlayerPreview("An Invented Book"))

        compose.onNodeWithTag(CoverTags.PLACEHOLDER).assertExists()
    }

    @Test
    fun `opened with nothing handed over, the book's frame is grey shapes only`() {
        show(null)

        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).assertCountEquals(0)
        compose.onNodeWithTag(CoverTags.PLACEHOLDER).assertDoesNotExist()
        assertSomePlaceholders()
        assertNothingFocusable()
    }

    @Test
    fun `a podcast's frame shows the card's name and cover, the episodes as grey rows`() {
        show(PlayerPreview("An Invented Show", coverUrl = "https://abs.test/api/items/b1/cover"), podcast = true)

        compose.onNodeWithText("An Invented Show").assertExists()
        compose.onNodeWithContentDescription("An Invented Show").assertExists()
        compose.onNodeWithTag(DetailLoadingTags.EPISODE_ROWS).assertExists()
        compose.onNodeWithText("Loading…").assertDoesNotExist()
        assertNothingFocusable()
    }

    @Test
    fun `opened with nothing handed over, the podcast's frame is grey shapes only`() {
        show(null, podcast = true)

        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).assertCountEquals(0)
        compose.onNodeWithTag(DetailLoadingTags.EPISODE_ROWS).assertExists()
        assertNothingFocusable()
    }

    private fun titleTop(): Dp = compose.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().top

    @Test
    fun `the book's title does not move when the page is ready`() {
        var ready by mutableStateOf(false)
        compose.setContent {
            FahrenheitTheme {
                if (ready) {
                    BookDetailView(
                        itemId = "b1",
                        content = DetailHeaderContent("An Invented Book", "An Invented Writer", emptyList(), "Play", "A blurb."),
                        onPrimary = {},
                        padding = margin
                    )
                } else {
                    DetailLoading("b1", PlayerPreview("An Invented Book", byline = "An Invented Writer"), podcast = false, margin = margin)
                }
            }
        }
        compose.waitForIdle()
        val loading = titleTop()

        compose.runOnIdle { ready = true }
        compose.waitForIdle()

        assertEquals(loading, titleTop())
    }

    private val book: LibraryItemResponse = Gson().fromJson(
        """{"id":"b1","mediaType":"book","media":{"metadata":{"title":"An Invented Book","explicit":false}}}""",
        LibraryItemResponse::class.java
    )

    @Test
    fun `the stage draws the frame while loading, then the page with focus on Play`() {
        var item by mutableStateOf<LibraryItemResponse?>(null)
        compose.setContent {
            FahrenheitTheme {
                DetailStage("b1", PlayerPreview("An Invented Book"), podcast = false, item = item, failed = false) { loaded, padding ->
                    BookDetailView(
                        itemId = loaded.id,
                        content = DetailHeaderContent(loaded.media.metadata.title, null, emptyList(), "Play", null),
                        onPrimary = {},
                        padding = padding
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(DetailLoadingTags.FRAME).assertExists()
        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()

        compose.runOnIdle { item = book }
        compose.waitForIdle()

        compose.onNodeWithTag(DetailLoadingTags.FRAME).assertDoesNotExist()
        compose.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsFocused()
    }

    @Test
    fun `a failed load says so, not the frame`() {
        compose.setContent {
            FahrenheitTheme { DetailStage("b1", null, podcast = false, item = null, failed = true) { _, _ -> } }
        }
        compose.waitForIdle()

        compose.onNodeWithTag(DetailLoadingTags.FRAME).assertDoesNotExist()
        compose.onNodeWithText("Could not load this item", substring = true).assertExists()
    }
}
