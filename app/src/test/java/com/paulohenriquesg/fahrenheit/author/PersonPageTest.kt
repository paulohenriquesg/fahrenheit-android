package com.paulohenriquesg.fahrenheit.author

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The author's and a narrator's page, ready (#230; docs/mocks/author-page.html):
 * the photo, name and counts on the left with Continue, the bio box, and the
 * books one row per series.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PersonPageTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @After
    fun drain() = shadowOf(Looper.getMainLooper()).idle()

    private val opened = mutableListOf<String>()
    private val continued = mutableListOf<String>()

    private var resumeState by mutableStateOf<LibraryItem?>(null)

    private fun show(page: PersonPage?, resume: LibraryItem? = null, error: String? = null) {
        resumeState = resume
        compose.setContent {
            FahrenheitTheme {
                val rail = remember { FocusRequester() }
                Row {
                    // Stands in for the side menu, which takes focus when the page has nothing to.
                    Box(Modifier.size(10.dp).focusRequester(rail).focusable().testTag("rail"))
                    PersonStage(
                        preview = null,
                        narrator = page?.narrator ?: false,
                        loading = false,
                        error = error,
                        page = page,
                        resume = resumeState,
                        onBook = { opened += it.id },
                        onContinue = { continued += it.id },
                        rail = rail
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun `the author's books are one row per series, then Other books, with the counts on the left`() {
        show(PersonPage.author(author()))

        compose.onNodeWithText("An Author").assertExists()
        compose.onNodeWithText("6 books · 2 series").assertExists()
        compose.onNodeWithText("ANOTHER ROAD · 1 BOOK").assertExists()
        compose.onNodeWithText("THE LONG WAY · 3 BOOKS").assertExists()
        compose.onNodeWithText("OTHER BOOKS · 2").assertExists()
        assertEquals(
            listOf("Second Light", "Book One", "Book Two", "Book Two", "A Quiet Signal", "Far Harbour"),
            compose.onAllNodesWithTag(PersonTags.BOOK_TITLE, useUnmergedTree = true).fetchSemanticsNodes().map {
                it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
            }
        )
    }

    @Test
    fun `two editions of one title are told apart by the subtitle tag and the length`() {
        show(PersonPage.author(author()))

        compose.onAllNodesWithText("Book Two", useUnmergedTree = true).fetchSemanticsNodes().let { assertEquals(2, it.size) }
        compose.onNodeWithText("Unabridged", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("#2 · 14 h 40 min", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("#2 · 3 h 1 min", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `with nothing in progress there is no Continue, and focus lands on the first book, which opens it`() {
        show(PersonPage.author(author()))

        compose.onNodeWithTag(PersonTags.CONTINUE).assertDoesNotExist()
        compose.onAllNodesWithTag(PersonTags.BOOK)[0].assertIsFocused()
        compose.onAllNodesWithTag(PersonTags.BOOK)[0].performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("x2"), opened)
    }

    @Test
    fun `a book in progress gets Continue, which holds focus and plays it`() {
        val page = PersonPage.author(author())
        show(page, resume = page.items.first { it.id == "long" })

        compose.onNodeWithText("Continue Book Two", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(PersonTags.CONTINUE).assertIsFocused()
        compose.onNodeWithTag(PersonTags.CONTINUE).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("long"), continued)
    }

    @Test
    fun `OK on the bio box opens it in full, and Back closes it onto the box`() {
        show(PersonPage.author(author()))

        compose.onNodeWithText("About".uppercase()).assertExists()
        compose.onNodeWithTag(PersonTags.FULL_BIO).assertDoesNotExist()
        compose.onNodeWithTag(PersonTags.BIO_BOX).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        compose.onNodeWithTag(PersonTags.FULL_BIO).assertExists()
        compose.onNodeWithTag(com.paulohenriquesg.fahrenheit.detail.DESCRIPTION_TAG).assertIsFocused()

        back()

        compose.onNodeWithTag(PersonTags.FULL_BIO).assertDoesNotExist()
        compose.onNodeWithTag(PersonTags.BIO_BOX).assertIsFocused()
    }

    @Test
    fun `a failed page leaves focus on the side menu`() {
        show(null, error = "Failed to load")

        compose.onNodeWithTag("rail").assertIsFocused()
    }

    @Test
    fun `an author with a bio and no books lands on the bio`() {
        show(PersonPage.author(author().copy(libraryItems = emptyList(), series = emptyList())))

        compose.onNodeWithTag(PersonTags.BIO_BOX).assertIsFocused()
    }

    @Test
    fun `an author with no bio and no books leaves focus on the side menu`() {
        show(PersonPage.author(author(bio = null).copy(libraryItems = emptyList(), series = emptyList())))

        compose.onNodeWithTag("rail").assertIsFocused()
    }

    @Test
    fun `Continue arriving later does not take focus from the book that has it`() {
        val page = PersonPage.author(author())
        show(page)
        compose.onAllNodesWithTag(PersonTags.BOOK)[0].assertIsFocused()

        resumeState = page.items.first { it.id == "long" }
        compose.waitForIdle()

        compose.onNodeWithTag(PersonTags.CONTINUE).assertExists()
        compose.onAllNodesWithTag(PersonTags.BOOK)[0].assertIsFocused()
    }

    @Test
    fun `a series named like the bio's row does not clash with it`() {
        val author = author()
        show(PersonPage.author(author.copy(series = author.series!!.map { it.copy(name = "about") })))

        compose.onNodeWithTag(PersonTags.BIO_BOX).assertExists()
    }

    @Test
    fun `a bio too long for the box says More`() {
        show(PersonPage.author(author(bio = List(12) { "An invented line of the bio." }.joinToString(" "))))

        compose.onNodeWithText("More ›", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a bio that fits says no More`() {
        show(PersonPage.author(author(bio = "Short.")))

        compose.onNodeWithText("More ›", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `no bio, no box`() {
        show(PersonPage.author(author(bio = null)))

        compose.onNodeWithTag(PersonTags.BIO_BOX).assertDoesNotExist()
        compose.onNodeWithText("About".uppercase()).assertDoesNotExist()
    }

    @Test
    fun `a narrator's page reads Reads N books, has no bio, and its cards name the author`() {
        val items = listOf(
            item("b1", "Book One", series = """[{"id":"s1","name":"The Long Way","sequence":"1"}]""", author = "An Author"),
            item("q", "A Quiet Signal", author = "Another Author"),
            item("f", "Far Harbour", author = "Another Author")
        )

        show(PersonPage.narrator("A Reader", items))

        compose.onNodeWithText("A Reader").assertExists()
        compose.onNodeWithText("Reads 3 books").assertExists()
        compose.onNodeWithTag(PersonTags.BIO_BOX).assertDoesNotExist()
        compose.onNodeWithText("#1 · An Author", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("Another Author", useUnmergedTree = true).fetchSemanticsNodes().let { assertEquals(2, it.size) }
        compose.onNodeWithText("OTHER BOOKS · 2").assertExists()
    }

    private fun author(bio: String? = "An author of invented stories, written for this test and long enough to need more than a few lines of the box to read in full, which is the point of the box and its More.") =
        Gson().fromJson(
            """{"id":"a1","name":"An Author","libraryId":"lib","addedAt":0,"updatedAt":0
               ${bio?.let { ",\"description\":\"$it\"" } ?: ""},
               "libraryItems":[${json("long", "Book Two", subtitle = "Unabridged", duration = 52_800.0)},
                 ${json("short", "Book Two", duration = 10_860.0)}, ${json("b1", "Book One")},
                 ${json("x2", "Second Light")}, ${json("q", "A Quiet Signal")}, ${json("f", "Far Harbour")}],
               "series":[
                 {"id":"s1","name":"The Long Way","items":[
                   ${json("b1", "Book One", series = """{"id":"s1","name":"The Long Way","sequence":"1"}""")},
                   ${json("long", "Book Two", series = """{"id":"s1","name":"The Long Way","sequence":"2"}""", subtitle = "Unabridged", duration = 52_800.0)},
                   ${json("short", "Book Two", series = """{"id":"s1","name":"The Long Way","sequence":"2"}""", duration = 10_860.0)}]},
                 {"id":"s2","name":"Another Road","items":[
                   ${json("x2", "Second Light", series = """{"id":"s2","name":"Another Road","sequence":"2"}""")}]}]}""",
            AuthorDetailResponse::class.java
        )

    private fun item(id: String, title: String, series: String? = null, author: String? = null): LibraryItem =
        Gson().fromJson(json(id, title, series, author = author), LibraryItem::class.java)

    private fun json(
        id: String,
        title: String,
        series: String? = null,
        subtitle: String? = null,
        duration: Double = 3600.0,
        author: String? = null
    ): String = """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
        "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
        "isMissing":false,"isInvalid":false,"mediaType":"book","numFiles":1,"size":0,
        "media":{"metadata":{"title":"$title"${subtitle?.let { ",\"subtitle\":\"$it\"" } ?: ""}
        ${author?.let { ",\"authorName\":\"$it\"" } ?: ""}${series?.let { ",\"series\":$it" } ?: ""}},
        "tags":[],"numTracks":1,"numAudioFiles":1,"numChapters":0,"duration":$duration,"size":0}}"""
}
