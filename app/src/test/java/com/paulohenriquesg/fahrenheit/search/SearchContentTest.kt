package com.paulohenriquesg.fahrenheit.search

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.SearchResults
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Frame 5 of docs/mocks/screens.html (#106). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class SearchContentTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(
        query: String = "",
        results: SearchResults = SearchResults.None,
        failed: Boolean = false,
        mediaType: String? = "book",
        onAuthorClick: (Author) -> Unit = {}
    ) {
        compose.setContent {
            FahrenheitTheme {
                Content(query, results, failed, mediaType, onAuthorClick)
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun Content(query: String, results: SearchResults, failed: Boolean, mediaType: String?, onAuthorClick: (Author) -> Unit) {
        SearchContent(
            query = TextFieldValue(query),
            onQueryChange = {},
            results = results,
            failed = failed,
            mediaType = mediaType,
            onItemClick = {},
            onAuthorClick = onAuthorClick
        )
    }

    @Test
    fun `the field has focus on arrival, so typing can start at once`() {
        render()

        compose.onNodeWithTag(SearchTags.FIELD).assertIsFocused()
    }

    @Test
    fun `results are grouped by kind, each group headed with its count`() {
        render(
            query = "writer",
            results = SearchResults(
                items = listOf(book("b1", "First Invented Book"), book("b2", "Second Invented Book")),
                authors = listOf(Author(id = "a1", name = "An Invented Writer", numBooks = 2))
            )
        )

        compose.onNodeWithText("Books · 2").assertIsDisplayed()
        compose.onNodeWithText("First Invented Book").assertIsDisplayed()
        compose.onNodeWithText("Authors · 1").assertExists()
        compose.onNodeWithText("An Invented Writer").assertExists()
    }

    @Test
    fun `a kind with no matches has no group`() {
        render(query = "book", results = SearchResults(items = listOf(book("b1", "First Invented Book")), authors = emptyList()))

        compose.onNodeWithText("Books · 1").assertIsDisplayed()
        compose.onNodeWithText("Authors", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a podcast library's matches are headed as podcasts`() {
        render(query = "show", results = SearchResults(items = listOf(book("p1", "An Invented Show")), authors = emptyList()), mediaType = "podcast")

        compose.onNodeWithText("Podcasts · 1").assertIsDisplayed()
    }

    @Test
    fun `pressing an author opens that author`() {
        var opened: String? = null
        render(
            query = "writer",
            results = SearchResults(items = emptyList(), authors = listOf(Author(id = "a1", name = "An Invented Writer"))),
            onAuthorClick = { opened = it.id }
        )

        compose.onNodeWithText("An Invented Writer").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals("a1", opened)
    }

    // The server has no offset for search (#193): a kind that came back at the
    // limit says it was cut, under its own row.
    @Test
    fun `a kind that was cut says so`() {
        render(
            query = "a",
            results = SearchResults(items = listOf(book("b1", "First Invented Book")), authors = emptyList(), itemsCut = true)
        )

        compose.onNodeWithText(CUT_LINE).assertIsDisplayed()
    }

    // Secondary text is 14sp and up (docs/ui-style-guide.md): smaller is
    // unreadable at 3 metres.
    @Test
    fun `the cut line is readable from the sofa`() {
        render(
            query = "a",
            results = SearchResults(items = listOf(book("b1", "First Invented Book")), authors = emptyList(), itemsCut = true)
        )

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(CUT_LINE).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)

        assertTrue(layouts.single().layoutInput.style.fontSize.value >= 14f)
    }

    @Test
    fun `a kind that was not cut says nothing about it`() {
        render(
            query = "a",
            results = SearchResults(items = listOf(book("b1", "First Invented Book")), authors = emptyList())
        )

        compose.onNodeWithText(CUT_LINE).assertDoesNotExist()
    }

    @Test
    fun `only the kind that was cut says so`() {
        render(
            query = "a",
            results = SearchResults(
                items = listOf(book("b1", "First Invented Book")),
                authors = listOf(Author(id = "a1", name = "An Invented Writer")),
                authorsCut = true
            )
        )

        compose.onAllNodesWithText(CUT_LINE).assertCountEquals(1)
        compose.onNodeWithTag(SearchTags.cutLine("authors")).assertExists()
        compose.onNodeWithTag(SearchTags.cutLine("items")).assertDoesNotExist()
    }

    @Test
    fun `nothing matched says so`() {
        render(query = "zzz")

        compose.onNodeWithText("Nothing found for \"zzz\"").assertIsDisplayed()
    }

    @Test
    fun `a failed search says so, rather than nothing found`() {
        render(query = "zzz", failed = true)

        compose.onNodeWithText("Search failed. Check the connection to your server.").assertIsDisplayed()
        compose.onNodeWithText("Nothing found", substring = true).assertDoesNotExist()
    }

    private companion object {
        const val CUT_LINE = "Showing the first 50. Add a word to narrow it."
    }

    private fun book(id: String, title: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"$title"},"tags":[],
            "numTracks":0,"numAudioFiles":0,"numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )
}
