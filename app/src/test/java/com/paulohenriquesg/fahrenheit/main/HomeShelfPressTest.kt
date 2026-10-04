package com.paulohenriquesg.fahrenheit.main

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.group.BookGroupActivity
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What pressing a card on each of today's Home shelves does (#147), pinned
 * before the rules move into one table. Continue listening's press and long
 * press are pinned in ContinueListeningPressTest, and every See all in
 * ShelfSeeAllTest and HomeSeeAllTest.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class HomeShelfPressTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun item(id: String, title: String, extra: String = "", mediaType: String = "book"): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"$mediaType",
            "media":{"metadata":{"title":"$title"},"tags":[],"numTracks":1,"numAudioFiles":1,
            "numChapters":0,"duration":0.0,"size":0}$extra}""",
        LibraryItem::class.java
    )

    private fun books(id: String, type: String, entity: LibraryItem) =
        Shelf(id = id, label = "A Shelf", labelStringKey = "", type = type, bookEntities = listOf(entity))

    private fun press(shelf: Shelf, text: String): Intent {
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOf(shelf), "lib") } }
        compose.waitForIdle()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        val started: Intent? = shadowOf(compose.activity).nextStartedActivity
        assertNotNull("nothing was started", started)
        return started!!
    }

    @Test
    fun `an episode on an episode shelf plays that episode`() {
        val episode = item(
            "pod-1", "An Invented Show",
            ""","recentEpisode":{"id":"ep-1","libraryItemId":"pod-1","title":"An Invented Episode"}""",
            mediaType = "podcast"
        )

        val started = press(books("newest-episodes", "episode", episode), "An Invented Episode")

        assertEquals(PlayerActivity::class.java.name, started.component?.className)
        assertEquals("pod-1", started.getStringExtra("item_id"))
        assertEquals("ep-1", started.getStringExtra("episode_id"))
        assertTrue(started.getBooleanExtra("auto_play", false))
    }

    @Test
    fun `a card on an episode shelf without its episode opens the podcast`() {
        val started = press(books("newest-episodes", "episode", item("pod-1", "An Invented Show", mediaType = "podcast")), "An Invented Show")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("pod-1", started.getStringExtra("item_id"))
    }

    @Test
    fun `a book on Recently added opens its details`() {
        val started = press(books("recently-added", "book", item("b1", "An Invented Book")), "An Invented Book")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("b1", started.getStringExtra("item_id"))
    }

    @Test
    fun `a book on Listen again opens its details, not the player`() {
        val started = press(books("listen-again", "book", item("b1", "An Invented Book")), "An Invented Book")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
    }

    @Test
    fun `a book on Discover opens its details`() {
        val started = press(books("discover", "book", item("b1", "An Invented Book")), "An Invented Book")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
    }

    @Test
    fun `a podcast on Recently added opens the podcast`() {
        val started = press(books("recently-added", "podcast", item("pod-1", "An Invented Show", mediaType = "podcast")), "An Invented Show")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("pod-1", started.getStringExtra("item_id"))
    }

    @Test
    fun `an author on Newest authors opens the author`() {
        val shelf = Shelf(
            id = "newest-authors", label = "A Shelf", labelStringKey = "", type = "authors",
            authorEntities = listOf(Author(id = "a1", name = "An Invented Writer"))
        )

        val started = press(shelf, "An Invented Writer")

        assertEquals(AuthorDetailActivity::class.java.name, started.component?.className)
        assertEquals("a1", started.getStringExtra("author_id"))
    }

    @Test
    fun `a series on Recent series opens it as a series`() {
        val shelf = Shelf(
            id = "recent-series", label = "A Shelf", labelStringKey = "", type = "series",
            seriesEntities = listOf(Series(id = "s1", name = "An Invented Series", books = emptyList()))
        )

        val started = press(shelf, "An Invented Series")

        assertEquals(BookGroupActivity::class.java.name, started.component?.className)
        assertEquals("SERIES", started.getStringExtra("kind"))
    }

    @Test
    fun `a shelf of a type nothing draws today shows nothing`() {
        val shelf = books("continue-reading", "some-future-type", item("b1", "An Invented Book"))

        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOf(shelf), "lib") } }
        compose.waitForIdle()

        compose.onNodeWithText("An Invented Book").assertDoesNotExist()
        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }
}
