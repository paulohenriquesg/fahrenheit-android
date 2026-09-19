package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.AudioFileMetadata
import com.paulohenriquesg.fahrenheit.api.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Whole-book time <-> (file, position within that file).
 *
 * Audiobookshelf reports a multi-file book as tracks whose startOffset is the
 * summed duration of the files before it, and keeps chapters, resume position
 * and progress in whole-book time. The player loads one file at a time, so every
 * position crossing that boundary needs translating - and getting it wrong is
 * silent: a seek past a file's end just plays nothing (#16).
 *
 * Offsets and durations below are real, from a two-file book on a live 2.36.0
 * server. Names are not.
 */
class TrackTimelineTest {
    private val eps = 1e-6

    private val part1 = TimelineTrack(index = 1, startOffset = 0.0, duration = 31124.073333, contentUrl = "/f/1")
    private val part2 = TimelineTrack(index = 2, startOffset = 31124.073333, duration = 43027.318322, contentUrl = "/f/2")
    private val twoFileBook = TrackTimeline(listOf(part1, part2))

    @Test
    fun `a single-file book maps whole-book time straight through`() {
        val single = TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/f/1")))

        val at = single.locate(1234.5)

        assertEquals(0, at.trackIndex)
        assertEquals(1234.5, at.positionInTrack, eps)
    }

    @Test
    fun `a position in the second file is offset by the first file's length`() {
        val at = twoFileBook.locate(40000.0)

        assertEquals(1, at.trackIndex)
        assertEquals(40000.0 - 31124.073333, at.positionInTrack, eps)
    }

    @Test
    fun `a chapter start past the first file resolves into the second file`() {
        // A real chapter boundary, stored by ABS in whole-book time.
        val at = twoFileBook.locate(38677.189025)

        assertEquals(1, at.trackIndex)
        assertEquals(38677.189025 - 31124.073333, at.positionInTrack, eps)
    }

    @Test
    fun `the exact start of a file belongs to that file, not the end of the previous one`() {
        val at = twoFileBook.locate(31124.073333)

        assertEquals(1, at.trackIndex)
        assertEquals(0.0, at.positionInTrack, eps)
    }

    @Test
    fun `just before a boundary is still the earlier file`() {
        val at = twoFileBook.locate(31124.0)

        assertEquals(0, at.trackIndex)
        assertEquals(31124.0, at.positionInTrack, eps)
    }

    @Test
    fun `a position in the file maps back to whole-book time for progress sync`() {
        // What the server must be told is book time, not time within the file.
        assertEquals(40000.0, twoFileBook.bookTime(trackIndex = 1, positionInTrack = 40000.0 - 31124.073333), eps)
    }

    @Test
    fun `locating then mapping back returns the same book time`() {
        for (t in listOf(0.0, 10.5, 31124.073333, 31200.25, 74151.391655)) {
            val at = twoFileBook.locate(t)
            assertEquals(t, twoFileBook.bookTime(at.trackIndex, at.positionInTrack), eps)
        }
    }

    @Test
    fun `total duration is where the last file ends`() {
        assertEquals(31124.073333 + 43027.318322, twoFileBook.totalDuration, eps)
    }

    @Test
    fun `a negative position clamps to the start`() {
        val at = twoFileBook.locate(-5.0)

        assertEquals(0, at.trackIndex)
        assertEquals(0.0, at.positionInTrack, eps)
    }

    @Test
    fun `a position past the end clamps to the end of the last file`() {
        val at = twoFileBook.locate(999_999.0)

        assertEquals(1, at.trackIndex)
        assertEquals(43027.318322, at.positionInTrack, eps)
    }

    @Test
    fun `tracks given out of order are placed by their offsets`() {
        val shuffled = TrackTimeline(listOf(part2, part1))

        assertEquals(1, shuffled.locate(40000.0).trackIndex)
        assertEquals("/f/2", shuffled.track(1).contentUrl)
    }

    @Test
    fun `the next file follows the current one, and the last has none`() {
        assertEquals("/f/2", twoFileBook.next(afterTrackIndex = 0)?.contentUrl)
        assertNull(twoFileBook.next(afterTrackIndex = 1))
    }

    @Test
    fun `server offsets are trusted over summed durations`() {
        // Real offsets are not always the exact float sum of earlier durations.
        // The server's startOffset is authoritative; recomputing drifts.
        val drifting = TrackTimeline(
            listOf(
                TimelineTrack(1, 0.0, 100.0000004, "/f/1"),
                TimelineTrack(2, 100.0, 50.0, "/f/2")
            )
        )

        assertEquals(1, drifting.locate(100.0).trackIndex)
    }

    private fun apiTrack(index: Int, startOffset: Double, duration: Double, url: String) = Track(
        index = index,
        startOffset = startOffset,
        duration = duration,
        title = "Part $index",
        contentUrl = url,
        mimeType = "audio/mp4",
        metadata = AudioFileMetadata(
            filename = "part$index.m4b", ext = ".m4b", path = "part$index.m4b",
            relPath = "part$index.m4b", size = 1L, mtimeMs = 0L, ctimeMs = 0L, birthtimeMs = 0L
        )
    )

    @Test
    fun `a book's API tracks become a timeline`() {
        val timeline = timelineOf(
            listOf(
                apiTrack(1, 0.0, 31124.073333, "/api/items/li_1/file/1"),
                apiTrack(2, 31124.073333, 43027.318322, "/api/items/li_1/file/2")
            )
        )!!

        assertEquals(2, timeline.size)
        assertEquals("/api/items/li_1/file/2", timeline.locate(40000.0).let { timeline.track(it.trackIndex).contentUrl })
        assertEquals(31124.073333 + 43027.318322, timeline.totalDuration, eps)
    }

    @Test
    fun `a book with no tracks has no timeline rather than crashing`() {
        // Items still being scanned can come back with an empty track list.
        assertNull(timelineOf(emptyList()))
    }
}
