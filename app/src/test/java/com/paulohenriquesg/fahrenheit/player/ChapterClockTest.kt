package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where a position sits in its chapter, and where chapter skip goes (#107). */
class ChapterClockTest {
    private val three = listOf(
        Chapter(start = 0.0, end = 1800.0, title = "One"),
        Chapter(start = 1800.0, end = 3600.0, title = "Two"),
        Chapter(start = 3600.0, end = 5400.0, title = "Three")
    )
    private val spans = ChapterClock.spans(three, total = 5400.0)

    @Test fun `a position is in the chapter that contains it`() = assertEquals("Two", ChapterClock.at(spans, 2000.0)!!.title)
    @Test fun `a chapter's start belongs to it`() = assertEquals("Two", ChapterClock.at(spans, 1800.0)!!.title)

    @Test fun `time in and left in the chapter`() {
        val two = ChapterClock.at(spans, 2000.0)!!
        assertEquals(200.0, two.elapsed(2000.0), 0.0)
        assertEquals(1600.0, two.left(2000.0), 0.0)
        assertEquals(0.111f, two.fraction(2000.0), 0.001f)
    }

    // Review Focus 3.
    @Test fun `past the last chapter's end is still the last chapter`() {
        val last = ChapterClock.at(spans, 9999.0)!!
        assertEquals("Three", last.title)
        assertEquals(0.0, last.left(9999.0), 0.0)
        assertNull(ChapterClock.nextTarget(spans, 9999.0))
    }

    @Test fun `previous restarts a chapter that has been playing`() = assertEquals(1800.0, ChapterClock.previousTarget(spans, 1810.0)!!, 0.0)
    @Test fun `previous right after a start goes to the chapter before`() = assertEquals(0.0, ChapterClock.previousTarget(spans, 1802.0)!!, 0.0)
    @Test fun `previous in the first chapter's first seconds stays at its start`() = assertEquals(0.0, ChapterClock.previousTarget(spans, 1.0)!!, 0.0)
    @Test fun `next goes to the next chapter's start`() = assertEquals(3600.0, ChapterClock.nextTarget(spans, 2000.0)!!, 0.0)

    // Review Focus 2.
    @Test fun `chapters without an end run to the next start`() {
        val open = ChapterClock.spans(listOf(Chapter(start = 0.0, title = "A"), Chapter(start = 600.0, title = "B")), total = 1200.0)
        assertEquals(600.0, open[0].end, 0.0)
        assertEquals(1200.0, open[1].end, 0.0)
    }

    @Test fun `chapters out of order are sorted`() {
        val shuffled = ChapterClock.spans(listOf(three[2], three[0], three[1]), total = 5400.0)
        assertEquals(listOf("One", "Two", "Three"), shuffled.map { it.title })
        assertEquals(3600.0, ChapterClock.nextTarget(shuffled, 2000.0)!!, 0.0)
    }

    @Test fun `no chapters is no clock`() {
        assertEquals(emptyList<ChapterSpan>(), ChapterClock.spans(null, 100.0))
        assertNull(ChapterClock.at(emptyList(), 10.0))
    }

    // Review: a seek drops the fraction of a millisecond, and chapter starts
    // from the server are fractional; the position must not read as the
    // chapter before, or Next repeats and Previous jumps back two.
    @Test fun `a position a hair before a start belongs to that chapter`() {
        val fractional = ChapterClock.spans(listOf(Chapter(start = 0.0, end = 1234.5678, title = "One"), Chapter(start = 1234.5678, end = 2000.0, title = "Two")), total = 2000.0)
        assertEquals("Two", ChapterClock.at(fractional, 1234.567)!!.title)
        assertEquals(2000.0 - 1234.5678, ChapterClock.at(fractional, 1234.567)!!.left(1234.567), 0.01)
    }

    @Test fun `before the first chapter, previous goes to the start and next to the first chapter`() {
        val late = ChapterClock.spans(listOf(Chapter(start = 5.0, end = 600.0, title = "One"), Chapter(start = 600.0, end = 1200.0, title = "Two")), total = 1200.0)
        assertEquals(0.0, ChapterClock.previousTarget(late, 2.0)!!, 0.0)
        assertEquals(5.0, ChapterClock.nextTarget(late, 2.0)!!, 0.0)
    }

    // Review: ticks came from each chapter's end, so chapters without one had none.
    @Test fun `the book's ticks are every chapter start after the first`() {
        val open = ChapterClock.spans(listOf(Chapter(start = 0.0, title = "A"), Chapter(start = 600.0, title = "B"), Chapter(start = 900.0, title = "C")), total = 1200.0)
        assertEquals(listOf(0.5f, 0.75f), ChapterClock.ticks(open, 1200.0))
    }
}
