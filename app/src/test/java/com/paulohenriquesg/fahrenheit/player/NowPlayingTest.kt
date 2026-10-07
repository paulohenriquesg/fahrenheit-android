package com.paulohenriquesg.fahrenheit.player

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * One player for books and episodes (#73). What differs is three things: the
 * line under the title, whether chapter marks are drawn, and which file plays.
 * Frames 4 and 4b of docs/mocks/screens.html.
 */
// Robolectric: an episode's notes are read through Android's HTML parser.
@RunWith(AndroidJUnit4::class)
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

    private fun bookWithMetadata(extra: String): LibraryItemResponse = Gson().fromJson(
        """{"id":"b3","mediaType":"book","media":{"duration":600.0,
            "metadata":{"title":"A Book","authorName":"Andy Weir","explicit":false$extra}}}""",
        LibraryItemResponse::class.java
    )
    private val seriesBook = bookWithMetadata(""","narratorName":"A Reader","genres":["Science fiction"],"series":[{"id":"s1","name":"The Long Way","sequence":"2"}]""")
    private val standalone = bookWithMetadata(""","genres":["Science fiction","Thriller"]""")

    private val podcast: LibraryItemResponse = Gson().fromJson(
        """{"id":"p1","mediaType":"podcast","media":{"metadata":{"title":"Welcome to Night Vale","explicit":false},
            "episodes":[{"libraryItemId":"p1","id":"e295","index":1,"title":"295 - The Book of Dale","publishedAt":${now - day - 1000},
              "addedAt":0,"updatedAt":0,"description":"<p>Dale</p>",
              "audioTrack":{"index":1,"startOffset":0.0,"duration":1800.0,"title":"t","contentUrl":"/api/items/p1/file/9","mimeType":"audio/mpeg","codec":"mp3",
                            "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}}]}}""",
        LibraryItemResponse::class.java
    )

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

    @Test
    fun `a book in a series names the series and its number`() =
        assertEquals("The Long Way · Book 2", NowPlaying.of(seriesBook, episodeId = null, now = now)!!.kicker)

    @Test
    fun `a standalone book names its first genre`() =
        assertEquals("Science fiction", NowPlaying.of(standalone, episodeId = null, now = now)!!.kicker)

    @Test
    fun `a book with neither has no line above the title`() =
        assertNull(NowPlaying.of(book, episodeId = null, now = now)!!.kicker)

    @Test
    fun `the byline says who wrote it and who reads it`() =
        assertEquals("Andy Weir · read by A Reader", NowPlaying.of(seriesBook, episodeId = null, now = now)!!.byline)

    @Test
    fun `without a narrator the byline is the author`() =
        assertEquals("Andy Weir", NowPlaying.of(book, episodeId = null, now = now)!!.byline)

    @Test
    fun `an episode's line above the title is its show and when it came out`() {
        val playing = NowPlaying.of(podcast, episodeId = "e295", now = now)!!
        assertEquals("Welcome to Night Vale · Yesterday", playing.kicker)
        assertNull(playing.byline)
    }

    @Test
    fun `an episode of a show without a title still reads cleanly`() {
        val untitled = Gson().fromJson(
            """{"id":"p2","mediaType":"podcast","media":{"metadata":{"title":"","explicit":false},
                "episodes":[{"libraryItemId":"p2","id":"e1","index":1,"title":"Pilot","publishedAt":${now - day - 1000},"addedAt":0,"updatedAt":0}]}}""",
            LibraryItemResponse::class.java
        )
        assertEquals("Yesterday", NowPlaying.of(untitled, episodeId = "e1", now = now)!!.kicker)
    }

    private fun inSeries(sequence: String?): LibraryItemResponse = Gson().fromJson(
        """{"id":"b5","libraryId":"l1","mediaType":"book","media":{"duration":600.0,
            "metadata":{"title":"A Book","explicit":false,
              "series":[{"id":"s1","name":"The Long Way"${sequence?.let { ",\"sequence\":\"$it\"" } ?: ""}}]}}}""",
        LibraryItemResponse::class.java
    )

    @Test
    fun `a book knows its series and library, for About`() {
        val playing = NowPlaying.of(inSeries("2"), episodeId = null, now = now)!!
        assertEquals(SeriesRef(id = "s1", name = "The Long Way", sequence = "2"), playing.series)
        assertEquals("l1", playing.libraryId)
    }

    @Test
    fun `once the series is known the line says how many books it has`() =
        assertEquals("The Long Way · Book 2 of 3", NowPlaying.of(inSeries("2"), episodeId = null, now = now)!!.withSeriesTotal(3).kicker)

    @Test
    fun `a fractional number reads as it is`() =
        assertEquals("The Long Way · Book 1.5 of 4", NowPlaying.of(inSeries("1.5"), episodeId = null, now = now)!!.withSeriesTotal(4).kicker)

    @Test
    fun `a book with no number keeps the series name alone`() =
        assertEquals("The Long Way", NowPlaying.of(inSeries(null), episodeId = null, now = now)!!.withSeriesTotal(4).kicker)

    @Test
    fun `a book in no series is not changed by a total`() {
        val playing = NowPlaying.of(standalone, episodeId = null, now = now)!!
        assertEquals(playing, playing.withSeriesTotal(4))
    }

    @Test
    fun `a book carries its facts and whether it is finished, for About`() {
        val done: LibraryItemResponse = Gson().fromJson(
            """{"id":"b6","mediaType":"book","media":{"duration":600.0,
                "metadata":{"title":"A Book","explicit":false,"narrators":["A Reader"]}},
                "userMediaProgress":{"isFinished":true}}""",
            LibraryItemResponse::class.java
        )
        val playing = NowPlaying.of(done, episodeId = null, now = now)!!
        assertEquals(listOf(AboutFact(AboutFact.Kind.ReadBy, "A Reader"), AboutFact(AboutFact.Kind.Length, "10 min 0 s")), playing.facts)
        assertEquals(true, playing.finished)
    }

    @Test
    fun `a book's length in About is what will play`() {
        val playing = NowPlaying.of(book, episodeId = null, now = now)!!
        assertEquals(AboutFact(AboutFact.Kind.Length, PlaybackPosition.spoken(playing.trackTotal!!)), playing.facts.last())
        assertEquals(false, playing.finished)
    }

    @Test
    fun `an episode's facts are its show, date and length, and it has no Mark finished`() {
        val playing = NowPlaying.of(podcast, episodeId = "e295", now = now)!!
        val show = podcast.media.metadata.title
        assertEquals(
            listOf(AboutFact(AboutFact.Kind.Show, show), AboutFact(AboutFact.Kind.Published, "Yesterday"), AboutFact(AboutFact.Kind.Length, "30 min 0 s")),
            playing.facts
        )
        assertEquals(null, playing.finished)
    }

    // #178: About's byline, under the episode's title, is its show; the player's stays in the kicker.
    @Test
    fun `an episode carries its show, for About`() =
        assertEquals(podcast.media.metadata.title, NowPlaying.of(podcast, episodeId = "e295", now = now)!!.show)

    @Test
    fun `a book has no show`() = assertNull(NowPlaying.of(book, episodeId = null, now = now)!!.show)

    // Review: a library holding books 1, 2 and 7 of a series read "Book 7 of 3".
    @Test
    fun `a number beyond the books held keeps just the number`() =
        assertEquals("The Long Way · Book 7", NowPlaying.of(inSeries("7"), episodeId = null, now = now)!!.withSeriesTotal(3).kicker)

    @Test
    fun `a series of one says no more than Book N`() =
        assertEquals("The Long Way · Book 1", NowPlaying.of(inSeries("1"), episodeId = null, now = now)!!.withSeriesTotal(1).kicker)

    private fun show(vararg episodes: String): LibraryItemResponse = Gson().fromJson(
        """{"id":"p2","mediaType":"podcast","media":{"metadata":{"title":"A Show","explicit":false},"episodes":[${episodes.joinToString(",")}]}}""",
        LibraryItemResponse::class.java
    )

    private fun episodeJson(id: String, publishedAt: Long, extra: String = "") =
        """{"libraryItemId":"p2","id":"$id","index":1,"title":"Episode $id","publishedAt":$publishedAt,"addedAt":0,"updatedAt":0$extra,
            "audioTrack":{"index":1,"startOffset":0.0,"duration":1800.0,"title":"t","contentUrl":"/f/$id","mimeType":"audio/mpeg","codec":"mp3",
              "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}}"""

    @Test
    fun `an episode says what it is under its title`() {
        val playing = NowPlaying.of(
            show(episodeJson("e2", 2_000, ""","season":"2","episode":"295","episodeType":"bonus","subtitle":"A short subtitle"""")),
            episodeId = "e2", now = now
        )!!
        assertEquals("Bonus", playing.badge)
        assertEquals("Season 2 · Episode 295 · 30 min", playing.details)
        assertEquals("A short subtitle", playing.notes)
    }

    @Test
    fun `an episode knows the ones either side`() {
        val playing = NowPlaying.of(show(episodeJson("e1", 1_000), episodeJson("e2", 2_000), episodeJson("e3", 3_000)), episodeId = "e2", now = now)!!
        assertEquals("e1", playing.previous!!.id)
        assertEquals(EpisodeRef("e3", "Episode e3", 1800.0), playing.next)
    }

    @Test
    fun `a book has none of an episode's extras`() {
        val playing = NowPlaying.of(book, episodeId = null, now = now)!!
        assertNull(playing.badge)
        assertNull(playing.details)
        assertNull(playing.notes)
        assertNull(playing.previous)
        assertNull(playing.next)
    }
}
