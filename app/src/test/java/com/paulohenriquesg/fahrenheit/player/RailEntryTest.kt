package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the rail's Now playing entry says about what is queued (#107). */
class RailEntryTest {
    private val book = QueuedFile("b1", null, startOffset = 0.0, bookTotal = 3600.0)
    private val spans = ChapterClock.spans(
        listOf(Chapter(start = 0.0, end = 1800.0, title = "One"), Chapter(start = 1800.0, end = 3600.0, title = "Two")),
        total = 3600.0
    )

    @Test fun `nothing queued, no entry`() =
        assertNull(RailEntry.of(null, "A Book", 0.0, playing = false, speed = 1f, spans = emptyList()))

    // Review Focus 2.
    @Test fun `a book says the chapter playing and what is left of it`() {
        val entry = RailEntry.of(book, "A Book", positionInFile = 2000.0, playing = true, speed = 1f, spans = spans)!!
        assertEquals("Two", entry.chapter)
        assertEquals(1600.0, entry.leftSeconds, 0.001)
        assertEquals(2000f / 3600f, entry.progress, 0.001f)
        assertEquals(true, entry.playing)
    }

    @Test fun `the time left counts at the speed`() =
        assertEquals(800.0, RailEntry.of(book, "A Book", 2000.0, playing = true, speed = 2f, spans = spans)!!.leftSeconds, 0.001)

    @Test fun `an episode, or a book without chapters, says what is left of it all`() {
        val episode = QueuedFile("p1", "e1", startOffset = 0.0, bookTotal = 1800.0)
        val entry = RailEntry.of(episode, "An Episode", 600.0, playing = false, speed = 1f, spans = emptyList())!!
        assertNull(entry.chapter)
        assertEquals(1200.0, entry.leftSeconds, 0.001)
        assertEquals("e1", entry.episodeId)
    }

    @Test fun `a later file of a book counts from the book's start`() {
        val part2 = QueuedFile("b1", null, startOffset = 1800.0, bookTotal = 3600.0)
        assertEquals("Two", RailEntry.of(part2, "A Book", 200.0, playing = true, speed = 1f, spans = spans)!!.chapter)
    }

    @Test fun `a chapter without a title is numbered`() {
        val untitled = ChapterClock.spans(listOf(Chapter(start = 0.0, end = 3600.0, title = "")), total = 3600.0)
        val entry = RailEntry.of(book, "A Book", 10.0, playing = true, speed = 1f, spans = untitled)!!
        assertNull(entry.chapter)
        assertEquals(1, entry.chapterNumber)
    }

    // The state, honestly (#179): bars only while play is wanted and the player can play.
    @Test fun `ready or buffering, with play wanted, shows playing`() {
        assertTrue(RailEntry.showsPlaying(playWhenReady = true, state = Player.STATE_READY))
        // A seek buffers: no pause badge flashing.
        assertTrue(RailEntry.showsPlaying(playWhenReady = true, state = Player.STATE_BUFFERING))
    }

    @Test fun `paused shows paused`() {
        assertFalse(RailEntry.showsPlaying(playWhenReady = false, state = Player.STATE_READY))
        assertFalse(RailEntry.showsPlaying(playWhenReady = false, state = Player.STATE_BUFFERING))
    }

    @Test fun `ended or idle never shows playing, though play is still wanted`() {
        assertFalse(RailEntry.showsPlaying(playWhenReady = true, state = Player.STATE_ENDED))
        assertFalse(RailEntry.showsPlaying(playWhenReady = true, state = Player.STATE_IDLE))
    }
}
