package com.paulohenriquesg.fahrenheit.author

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The author page while the author loads (#225; frame 3 of docs/mocks/
 * detail-loading.html): the photo and name from the Authors card, the bio
 * and books as still grey shapes, rather than a lone spinner.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class AuthorLoadingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(preview: PlayerPreview?, author: AuthorDetailResponse? = null, loading: Boolean = true, error: String? = null) {
        compose.setContent {
            FahrenheitTheme { AuthorStage("a1", preview, loading, error, author, onBookClick = {}) }
        }
        compose.waitForIdle()
    }

    private fun assertNoSpinnerAndNothingFocusable() {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).assertCountEquals(0)
    }

    @Test
    fun `loading shows the card's name and photo, the rest grey, and no spinner`() {
        show(PlayerPreview("An Invented Writer", coverUrl = "https://abs.test/api/authors/a1/image"))

        compose.onNodeWithTag(AuthorLoadingTags.FRAME).assertExists()
        compose.onNodeWithText("An Invented Writer").assertExists()
        compose.onNodeWithContentDescription("An Invented Writer").assertExists()
        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.SHAPE).fetchSemanticsNodes().isNotEmpty())
        assertNoSpinnerAndNothingFocusable()
    }

    @Test
    fun `an author with no photo gets the grey square, with the name`() {
        show(PlayerPreview("An Invented Writer"))

        compose.onNodeWithText("An Invented Writer").assertExists()
        compose.onNodeWithContentDescription("An Invented Writer").assertDoesNotExist()
    }

    @Test
    fun `opened with nothing handed over, grey shapes only`() {
        show(null)

        compose.onNodeWithTag(AuthorLoadingTags.FRAME).assertExists()
        // "Books", the grid's own heading, is the one word known before the author is.
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).assertCountEquals(1)
        assertNoSpinnerAndNothingFocusable()
    }

    @Test
    fun `ready, the author replaces the frame`() {
        show(
            PlayerPreview("An Invented Writer"),
            author = AuthorDetailResponse(id = "a1", name = "An Invented Writer", libraryId = "lib", addedAt = 0, updatedAt = 0, libraryItems = emptyList()),
            loading = false
        )

        compose.onNodeWithTag(AuthorLoadingTags.FRAME).assertDoesNotExist()
        compose.onNodeWithText("0 books").assertExists()
    }

    @Test
    fun `a failed load says why, not the frame`() {
        show(PlayerPreview("An Invented Writer"), loading = false, error = "Failed to load author")

        compose.onNodeWithTag(AuthorLoadingTags.FRAME).assertDoesNotExist()
        compose.onNodeWithText("Failed to load author").assertExists()
    }
}
