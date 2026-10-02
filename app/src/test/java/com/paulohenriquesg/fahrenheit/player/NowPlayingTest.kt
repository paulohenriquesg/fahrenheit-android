package com.paulohenriquesg.fahrenheit.player

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One player for books and episodes (#73). What differs is three things: the
 * line under the title, whether chapter marks are drawn, and which file plays.
 * Frames 4 and 4b of docs/mocks/screens.html.
 */
class NowPlayingTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_790_942_400_000L

    private val book: LibraryItemResponse = Gson().fromJson(
        """{"id":"b1","mediaType":"book","media":{"duration":58200.0,
            "metadata":{"title":"Project Hail Mary","authorName":"Andy Weir","explicit":false},
            "chapters":[{"id":0,"start":0.0,"end":600.0,"title":"Chapter 1"},{"id":1,"start":600.0,"end":1200.0,"title":"Chapter 2"}],
            "tracks":[{"index":1,"startOffset":0.0,"duration":58000.0,"title":"t1","contentUrl":"/api/items/b1/file/1","mimeType":"audio/mpeg",
                       "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}]}}""",
        LibraryItemResponse::class.java
    )

    private val bookInTwoFiles: LibraryItemResponse = Gson().fromJson(
        """{"id":"b2","mediaType":"book","media":{"duration":5400.0,
            "metadata":{"title":"A Book in Parts","authorName":"An Author","explicit":false},
            "tracks":[
              {"index":1,"startOffset":0.0,"duration":3600.0,"title":"t1","contentUrl":"/api/items/b2/file/1","mimeType":"audio/mpeg",
               "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}},
              {"index":2,"startOffset":3600.0,"duration":1800.0,"title":"t2","contentUrl":"/api/items/b2/file/2","mimeType":"audio/mpeg",
               "metadata":{"filename":"b","ext":"mp3","path":"/b","relPath":"b","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}]}}""",
        LibraryItemResponse::class.java
    )

    private val podcast: LibraryItemResponse = Gson().fromJson(
        """{"id":"p1","mediaType":"podcast","media":{"metadata":{"title":"Welcome to Night Vale","explicit":false},
            "episodes":[{"libraryItemId":"p1","id":"e295","index":1,"title":"295 - The Book of Dale","publishedAt":${now - day - 1000},
              "addedAt":0,"updatedAt":0,"description":"<p>Dale</p>",
              "audioTrack":{"index":1,"startOffset":0.0,"duration":1800.0,"title":"t","contentUrl":"/api/items/p1/file/9","mimeType":"audio/mpeg","codec":"mp3",
                            "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}}]}}""",
        LibraryItemResponse::class.java
    )

    @Test
    fun `a book says which chapter is playing, and who wrote it`() {
        val playing = NowPlaying.of(book, episodeId = null, now = now)!!

        assertEquals("Project Hail Mary", playing.title)
        assertEquals("Chapter 2 · Andy Weir", playing.subtitle(currentTime = 700.0))
        assertEquals("Chapter 1 · Andy Weir", playing.subtitle(currentTime = 0.0))
    }

    @Test
    fun `a book plays its tracks, with chapter marks, and trusts the tracks' length`() {
        val playing = NowPlaying.of(book, episodeId = null, now = now)!!

        assertEquals("/api/items/b1/file/1", playing.timeline!!.track(0).contentUrl)
        assertEquals(2, playing.chapters?.size)
        assertEquals(58000.0, playing.trackTotal!!, 0.0)
        assertEquals(58200.0, playing.mediaDuration!!, 0.0)
        assertNull(playing.episodeId)
    }

    @Test
    fun `a book in several files plays all of them, not just the first`() {
        val playing = NowPlaying.of(bookInTwoFiles, episodeId = null, now = now)!!

        val timeline = playing.timeline!!
        assertEquals(2, timeline.size)
        assertEquals("/api/items/b2/file/2", timeline.track(1).contentUrl)
        assertEquals(5400.0, playing.trackTotal!!, 0.0)
    }

    @Test
    fun `a book has no way to its podcast, having none`() {
        assertFalse(NowPlaying.of(book, episodeId = null, now = now)!!.goToPodcast)
    }

    @Test
    fun `an episode shows its own title, and the show with when it came out`() {
        val playing = NowPlaying.of(podcast, episodeId = "e295", now = now)!!

        assertEquals("295 - The Book of Dale", playing.title)
        assertEquals("Welcome to Night Vale · Yesterday", playing.subtitle(currentTime = 100.0))
    }

    @Test
    fun `an episode plays its one file, without chapter marks, and offers its podcast`() {
        val playing = NowPlaying.of(podcast, episodeId = "e295", now = now)!!

        val timeline = playing.timeline!!
        assertEquals(1, timeline.size)
        assertEquals("/api/items/p1/file/9", timeline.track(0).contentUrl)
        assertEquals(0.0, timeline.track(0).startOffset, 0.0)
        assertNull(playing.chapters)
        assertEquals(1800.0, playing.trackTotal!!, 0.0)
        assertEquals("e295", playing.episodeId)
        assertTrue(playing.goToPodcast)
    }

    @Test
    fun `an episode the podcast does not have is nothing to play`() {
        assertNull(NowPlaying.of(podcast, episodeId = "missing", now = now))
    }
}
