package com.paulohenriquesg.fahrenheit.detail

import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.player.SeriesBook
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * What the opening screen hands the book, podcast and author pages for their
 * loading frame (#225): the cover and name the card already shows.
 */
@RunWith(RobolectricTestRunner::class)
class DetailPreviewTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun signIn() {
        SharedPreferencesHandler(context).saveUserPreferences(UserPreferences("https://abs.test", "listener", "token", false))
        ApiClient.initialize(context)
    }

    @After
    fun signOut() = ApiClient.clearSession()

    private fun item(json: String): LibraryItem = Gson().fromJson(
        """{"id":"i1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,$json}""",
        LibraryItem::class.java
    )

    private val book = item(
        """"mediaType":"book","media":{"metadata":{"title":"An Invented Book","authorName":"An Invented Writer"},"coverPath":"/c.jpg","tags":[],
        "numTracks":1,"numAudioFiles":1,"numChapters":0,"duration":0.0,"size":0}"""
    )

    private val show = item(
        """"mediaType":"podcast","media":{"metadata":{"title":"An Invented Show","author":"A Maker"},"coverPath":"/c.jpg","tags":[],
        "numTracks":0,"numAudioFiles":0,"numChapters":0,"duration":0.0,"size":0},
        "recentEpisode":{"id":"ep-1","libraryItemId":"i1","title":"An Invented Episode"}"""
    )

    @Test
    fun `the preview and the kind of page travel with the intent`() {
        val preview = PlayerPreview("An Invented Book", coverUrl = "https://abs.test/api/items/b1/cover", byline = "A. Writer")
        val intent = DetailActivity.createIntent(context, "b1", preview, podcast = true)

        assertEquals("b1", intent.getStringExtra("item_id"))
        assertEquals(preview, DetailActivity.previewOf(intent))
        assertTrue(DetailActivity.podcastOf(intent))
    }

    @Test
    fun `opened with nothing handed over, there is no preview and it is a book`() {
        val intent = DetailActivity.createIntent(context, "b1")

        assertNull(DetailActivity.previewOf(intent))
        assertFalse(DetailActivity.podcastOf(intent))
    }

    @Test
    fun `a book card hands over its title, author and cover`() {
        val intent = DetailActivity.intentFor(context, book)

        assertEquals("i1", intent.getStringExtra("item_id"))
        assertEquals(
            PlayerPreview("An Invented Book", coverUrl = "https://abs.test/api/items/i1/cover", byline = "An Invented Writer"),
            DetailActivity.previewOf(intent)
        )
        assertFalse(DetailActivity.podcastOf(intent))
    }

    @Test
    fun `a podcast card hands over the show's name, not its newest episode, and opens the podcast frame`() {
        val intent = DetailActivity.intentFor(context, show)

        assertEquals(
            PlayerPreview("An Invented Show", coverUrl = "https://abs.test/api/items/i1/cover"),
            DetailActivity.previewOf(intent)
        )
        assertTrue(DetailActivity.podcastOf(intent))
    }

    @Test
    fun `a card with no cover asks for none`() {
        val bare = book.copy(media = book.media.copy(coverPath = null))

        assertNull(PlayerPreview.page(bare)?.coverUrl)
    }

    @Test
    fun `a book of the series hands over its title and cover`() {
        assertEquals(
            PlayerPreview("Book Two", coverUrl = "https://abs.test/api/items/b2/cover"),
            PlayerPreview.of(SeriesBook("b2", "Book Two", "2"))
        )
    }

    @Test
    fun `Go to podcast hands over the show's name and cover`() {
        val intent = PlayerActivity.podcastIntent(context, "p1", "An Invented Show")

        assertEquals(PlayerPreview("An Invented Show", coverUrl = "https://abs.test/api/items/p1/cover"), DetailActivity.previewOf(intent))
        assertTrue(DetailActivity.podcastOf(intent))
    }

    @Test
    fun `leaving the player for the podcast hands over the show it was playing`() {
        // Any Activity will do as the player here: the real one is not built under Robolectric.
        val player = Robolectric.buildActivity(ComponentActivity::class.java).create().get()

        PlayerActivity.leaveForPodcast(player, "p1", "An Invented Show")

        val started = shadowOf(player).nextStartedActivity
        assertEquals("An Invented Show", DetailActivity.previewOf(started)?.title)
        assertTrue(DetailActivity.podcastOf(started))
    }

    @Test
    fun `an author card hands over the name, and the photo only when there is one`() {
        val withPhoto = Author(id = "a1", name = "An Invented Writer", imagePath = "/a.jpg")

        assertEquals(
            PlayerPreview("An Invented Writer", coverUrl = "https://abs.test/api/authors/a1/image"),
            PlayerPreview.author(withPhoto)
        )
        assertEquals(PlayerPreview("An Invented Writer"), PlayerPreview.author(withPhoto.copy(imagePath = null)))
    }

    @Test
    fun `the author page's intent carries the preview`() {
        val preview = PlayerPreview("An Invented Writer", coverUrl = "https://abs.test/api/authors/a1/image")

        assertEquals(preview, AuthorDetailActivity.previewOf(AuthorDetailActivity.createIntent(context, "a1", preview)))
        assertNull(AuthorDetailActivity.previewOf(AuthorDetailActivity.createIntent(context, "a1")))
    }
}
