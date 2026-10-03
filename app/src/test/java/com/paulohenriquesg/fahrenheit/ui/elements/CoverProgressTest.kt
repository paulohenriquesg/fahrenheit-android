package com.paulohenriquesg.fahrenheit.ui.elements

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverProgressTest {

    @Test
    fun `a started book shows how far in and how long is left`() {
        val progress = CoverProgress.index(
            listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = 3000.0, duration = 12000.0))
        )

        val cover = progress.of(book("book-1"))

        assertEquals(0.25f, cover!!.fraction, 0.0001f)
        assertEquals(9000.0, cover.secondsLeft, 0.0)
    }

    @Test
    fun `a book nobody has started has no progress`() {
        val progress = CoverProgress.index(
            listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = 3000.0, duration = 12000.0))
        )

        assertNull(progress.of(book("book-2")))
    }

    @Test
    fun `a book opened but not played has no progress`() {
        val progress = CoverProgress.index(
            listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = 0.0, duration = 12000.0))
        )

        assertNull(progress.of(book("book-1")))
    }

    @Test
    fun `a finished book draws no bar`() {
        val progress = CoverProgress.index(
            listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = 12000.0, duration = 12000.0, isFinished = true))
        )

        assertNull(progress.of(book("book-1")))
    }

    @Test
    fun `progress without a duration is not guessed at`() {
        val progress = CoverProgress.index(
            listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = 3000.0))
        )

        assertNull(progress.of(book("book-1")))
    }

    @Test
    fun `an episode card reads the episode's progress, not the podcast's`() {
        val progress = CoverProgress.index(
            listOf(
                MediaProgressResponse(libraryItemId = "pod-1", episodeId = "ep-old", currentTime = 100.0, duration = 200.0),
                MediaProgressResponse(libraryItemId = "pod-1", episodeId = "ep-new", currentTime = 600.0, duration = 2400.0)
            )
        )

        val cover = progress.of(episodeCard(podcastId = "pod-1", episodeId = "ep-new"))

        assertEquals(0.25f, cover!!.fraction, 0.0001f)
        assertEquals(1800.0, cover.secondsLeft, 0.0)
    }

    @Test
    fun `a podcast card on the grid takes no episode's progress`() {
        val progress = CoverProgress.index(
            listOf(MediaProgressResponse(libraryItemId = "pod-1", episodeId = "ep-1", currentTime = 100.0, duration = 200.0))
        )

        assertNull(progress.of(book("pod-1")))
    }

    @Test
    fun `an empty index knows nothing`() {
        assertNull(CoverProgress.None.of(book("book-1")))
    }

    private fun book(id: String): LibraryItem = item(id, "book", recentEpisode = "")

    private fun episodeCard(podcastId: String, episodeId: String): LibraryItem =
        item(podcastId, "podcast", recentEpisode = ""","recentEpisode":{"id":"$episodeId","libraryItemId":"$podcastId","title":"An Episode"}""")

    private fun item(id: String, mediaType: String, recentEpisode: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"$mediaType",
            "media":{"metadata":{"title":"A Title"},"tags":[],"numTracks":0,
            "numAudioFiles":0,"numChapters":0,"duration":0.0,"size":0}$recentEpisode}""",
        LibraryItem::class.java
    )
}
