package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the sleep timer pauses, and how much it has left (#107). */
class SleepTimerTest {
    private val ends = listOf(1800.0, 3600.0, 5400.0)

    @Test fun `off by default, with nothing left`() {
        val timer = SleepTimer()
        assertEquals(SleepChoice.Off, timer.choice)
        assertFalse(timer.due(100.0))
        assertNull(timer.secondsLeft(100.0, 1f))
    }

    @Test fun `minutes count only the listening added`() {
        val timer = SleepTimer().apply { minutes(15) }
        timer.listened(14 * 60_000L)
        assertFalse(timer.due(0.0))
        assertEquals(60.0, timer.secondsLeft(0.0, 1f)!!, 0.001)
        timer.listened(60_000L)
        assertTrue(timer.due(0.0))
    }

    @Test fun `minutes are listening time whatever the speed`() {
        val timer = SleepTimer().apply { minutes(30) }
        assertEquals(1800.0, timer.secondsLeft(0.0, 2f)!!, 0.001)
    }

    @Test fun `end of chapter waits for the end of the chapter playing`() {
        val timer = SleepTimer().apply { endOfChapter(ends, position = 2000.0) }
        assertEquals(SleepChoice.EndOfChapter, timer.choice)
        assertFalse(timer.due(3599.0))
        assertTrue(timer.due(3600.0))
    }

    // ChapterClock's 50 ms: a seek to a chapter's start can land just before it.
    @Test fun `just before a chapter's start counts as that chapter`() {
        val timer = SleepTimer().apply { endOfChapter(ends, position = 1799.97) }
        assertFalse(timer.due(1800.0))
        assertTrue(timer.due(3600.0))
    }

    // Review Focus 2.
    @Test fun `a seek retargets the chapter end`() {
        val timer = SleepTimer().apply { endOfChapter(ends, position = 2000.0) }
        timer.seeked(4000.0)
        assertFalse(timer.due(4000.0))
        assertTrue(timer.due(5400.0))
        timer.seeked(100.0)
        assertTrue(timer.due(1800.0))
    }

    @Test fun `what is left of a chapter counts at the speed`() {
        val timer = SleepTimer().apply { endOfChapter(ends, position = 3000.0) }
        assertEquals(400.0, timer.secondsLeft(3000.0, 1.5f)!!, 0.001)
    }

    @Test fun `past the last chapter's end it is due`() {
        val timer = SleepTimer().apply { endOfChapter(ends, position = 5400.0) }
        assertTrue(timer.due(5400.0))
    }

    @Test fun `off clears it`() {
        val timer = SleepTimer().apply { minutes(15); off() }
        assertEquals(SleepChoice.Off, timer.choice)
        assertFalse(timer.due(0.0))
    }

    @Test fun `the offered choices`() =
        assertEquals(
            listOf(SleepChoice.Off, SleepChoice.EndOfChapter, SleepChoice.Minutes(15), SleepChoice.Minutes(30), SleepChoice.Minutes(60)),
            SleepChoice.OFFERED
        )
}
