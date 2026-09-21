package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where playback starts, and which of the three durations on offer to believe.
 *
 * The server's media.duration can exceed the audio it actually has: on the
 * test library 20 books counted files the scanner had excluded. The tracks are
 * what will really be played, so their total wins where there is one.
 */
class ResumePointTest {

    private fun progress(current: Double?, duration: Double? = null, finished: Boolean? = false) =
        MediaProgressResponse(currentTime = current, duration = duration, isFinished = finished)

    @Test
    fun `playback resumes where it was left`() {
        val start = ResumePoint.decide(progress(current = 900.0), trackTotal = 3600.0, mediaDuration = 3600.0)

        assertEquals(900.0, start.positionSeconds, 1e-9)
        assertEquals(3600.0, start.totalSeconds, 1e-9)
    }

    @Test
    fun `nothing listened yet starts at the beginning`() {
        val start = ResumePoint.decide(null, trackTotal = 3600.0, mediaDuration = 3600.0)

        assertEquals(0.0, start.positionSeconds, 1e-9)
    }

    @Test
    fun `the tracks' total is preferred over an inflated media duration`() {
        val start = ResumePoint.decide(progress(current = 10.0, duration = 7200.0), trackTotal = 3600.0, mediaDuration = 7200.0)

        assertEquals(3600.0, start.totalSeconds, 1e-9)
    }

    @Test
    fun `without tracks the saved duration is used, then the media's`() {
        assertEquals(
            7000.0,
            ResumePoint.decide(progress(current = 10.0, duration = 7000.0), trackTotal = null, mediaDuration = 7200.0).totalSeconds,
            1e-9
        )
        assertEquals(
            7200.0,
            ResumePoint.decide(progress(current = 10.0, duration = null), trackTotal = null, mediaDuration = 7200.0).totalSeconds,
            1e-9
        )
        assertEquals(
            0.0,
            ResumePoint.decide(progress(current = 10.0), trackTotal = null, mediaDuration = null).totalSeconds,
            1e-9
        )
    }

    // Pressing play on something already finished should not drop the listener
    // at the last second of it.
    @Test
    fun `a finished book starts again from the beginning`() {
        val start = ResumePoint.decide(progress(current = 3599.0, finished = true), trackTotal = 3600.0, mediaDuration = 3600.0)

        assertEquals(0.0, start.positionSeconds, 1e-9)
    }

    // An inflated duration lets the saved position run past the real audio.
    @Test
    fun `a position past the end of the audio starts again from the beginning`() {
        val start = ResumePoint.decide(progress(current = 5000.0), trackTotal = 3600.0, mediaDuration = 7200.0)

        assertEquals(0.0, start.positionSeconds, 1e-9)
    }

    @Test
    fun `a negative saved position starts at the beginning`() {
        val start = ResumePoint.decide(progress(current = -5.0), trackTotal = 3600.0, mediaDuration = 3600.0)

        assertEquals(0.0, start.positionSeconds, 1e-9)
    }

    @Test
    fun `with no duration at all the saved position is still honoured`() {
        val start = ResumePoint.decide(progress(current = 900.0), trackTotal = null, mediaDuration = null)

        assertEquals(900.0, start.positionSeconds, 1e-9)
        assertEquals(0.0, start.totalSeconds, 1e-9)
    }
}
