package com.paulohenriquesg.fahrenheit.group

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.collection.CollectionDetailActivity
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.series.SeriesDetailActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What a series' screen and a collection's screen show and do (#73), pinned
 * before the two near-identical Activities become one. Each kind is opened
 * only through [seriesIntent] and [collectionIntent], so the merge changes
 * those two lines and nothing the assertions say.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class BookGroupScreenTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var scenario: ActivityScenario<Activity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    // --- the two ways in: the only lines the merge may change ---

    private fun seriesIntent(series: Series): Intent = SeriesDetailActivity.createIntent(context, series)

    private fun collectionIntent(collection: Collection): Intent = CollectionDetailActivity.createIntent(context, collection)

    private fun bareIntent(): Intent = Intent(context, SeriesDetailActivity::class.java)

    // --- fixtures ---

    private fun book(id: String, title: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"$title"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private val twoBooks = listOf(book("b1", "First Invented Book"), book("b2", "Second Invented Book"))

    private fun series(books: List<LibraryItem>?) =
        Series(id = "s1", name = "An Invented Series", description = "About the series", books = books)

    private fun collection(books: List<LibraryItem>?) = Collection(
        id = "c1", libraryId = "lib", name = "An Invented Collection", description = "About the collection",
        books = books, lastUpdate = 0L, createdAt = 0L
    )

    private fun open(intent: Intent) {
        scenario = ActivityScenario.launch<Activity>(intent)
        compose.waitForIdle()
    }

    private fun pressAndReadStarted(title: String): Intent {
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.OnClick)
        var started: Intent? = null
        scenario!!.onActivity { started = shadowOf(it).nextStartedActivity }
        assertNotNull("nothing was started", started)
        return started!!
    }

    // --- a series ---

    @Test
    fun `a series shows its name, count, description and books`() {
        open(seriesIntent(series(twoBooks)))

        compose.onNodeWithText("An Invented Series").assertIsDisplayed()
        compose.onNodeWithText("2 books").assertIsDisplayed()
        compose.onNodeWithText("About the series").assertIsDisplayed()
        compose.onNodeWithText("Books").assertIsDisplayed()
        compose.onNodeWithText("First Invented Book").assertIsDisplayed()
        compose.onNodeWithText("Second Invented Book").assertIsDisplayed()
    }

    @Test
    fun `a series with one book says book, not books`() {
        open(seriesIntent(series(twoBooks.take(1))))

        compose.onNodeWithText("1 book").assertIsDisplayed()
    }

    @Test
    fun `an empty series says so in its own words`() {
        open(seriesIntent(series(emptyList())))

        compose.onNodeWithText("0 books").assertIsDisplayed()
        compose.onNodeWithText("No books found in this series").assertIsDisplayed()
    }

    @Test
    fun `a series the server sent without a book list shows no message at all`() {
        open(seriesIntent(series(null)))

        compose.onNodeWithText("0 books").assertIsDisplayed()
        compose.onNodeWithText("No books found", substring = true).assertDoesNotExist()
    }

    @Test
    fun `pressing a book in a series opens that book`() {
        open(seriesIntent(series(twoBooks)))

        val started = pressAndReadStarted("Second Invented Book")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("b2", started.getStringExtra("item_id"))
    }

    // --- a collection ---

    @Test
    fun `a collection shows its name, count, description and books`() {
        open(collectionIntent(collection(twoBooks)))

        compose.onNodeWithText("An Invented Collection").assertIsDisplayed()
        compose.onNodeWithText("2 books").assertIsDisplayed()
        compose.onNodeWithText("About the collection").assertIsDisplayed()
        compose.onNodeWithText("Books").assertIsDisplayed()
        compose.onNodeWithText("First Invented Book").assertIsDisplayed()
    }

    @Test
    fun `an empty collection says so in its own words`() {
        open(collectionIntent(collection(emptyList())))

        compose.onNodeWithText("0 books").assertIsDisplayed()
        compose.onNodeWithText("No books found in this collection").assertIsDisplayed()
    }

    @Test
    fun `pressing a book in a collection opens that book`() {
        open(collectionIntent(collection(twoBooks)))

        val started = pressAndReadStarted("First Invented Book")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("b1", started.getStringExtra("item_id"))
    }

    // --- opened wrongly ---

    @Test
    fun `opened with nothing to show, the screen closes rather than showing a blank`() {
        open(bareIntent())

        assertEquals(Lifecycle.State.DESTROYED, scenario!!.state)
    }
}
