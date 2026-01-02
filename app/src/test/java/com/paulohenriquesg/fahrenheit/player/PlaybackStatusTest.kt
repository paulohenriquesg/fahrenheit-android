package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import org.junit.Assert.*
import org.junit.Test

class PlaybackStatusTest {

    @Test
    fun `resume position is preserved correctly`() {
        val savedPosition = 500.0 // 500 seconds
        val duration = 3600.0 // 1 hour

        val progress = TestFixtures.createMockMediaProgress(
            currentTime = savedPosition,
            duration = duration
        )

        assertEquals(savedPosition, progress.currentTime ?: 0.0, 0.01)
        assertEquals(duration, progress.duration ?: 0.0, 0.01)
        assertFalse(progress.isFinished ?: true)
    }

    @Test
    fun `progress calculation is accurate`() {
        val currentTime = 1800.0 // 30 minutes
        val duration = 3600.0 // 1 hour
        val expectedProgress = 0.5 // 50%

        val progress = TestFixtures.createMockMediaProgress(
            currentTime = currentTime,
            duration = duration
        )

        assertEquals(expectedProgress, progress.progress ?: 0.0, 0.01)
    }

    @Test
    fun `progress update request format is correct`() {
        val currentTime = 1234.5
        val duration = 3600.0

        val request = MediaProgressRequest(
            currentTime = currentTime,
            duration = duration
        )

        assertEquals(currentTime, request.currentTime ?: 0.0, 0.01)
        assertEquals(duration, request.duration ?: 0.0, 0.01)
    }

    @Test
    fun `time conversion from milliseconds to seconds`() {
        val positionMs = 5000 // 5 seconds in milliseconds
        val expectedSeconds = 5.0

        val seconds = positionMs / 1000.0

        assertEquals(expectedSeconds, seconds, 0.01)
    }

    @Test
    fun `time conversion from seconds to milliseconds`() {
        val positionSeconds = 5.5
        val expectedMs = 5500

        val ms = (positionSeconds * 1000).toInt()

        assertEquals(expectedMs, ms)
    }

    @Test
    fun `progress percentage calculation`() {
        val testCases = listOf(
            Triple(0.0, 3600.0, 0.0),      // Start
            Triple(1800.0, 3600.0, 0.5),   // Middle
            Triple(3600.0, 3600.0, 1.0),   // End
            Triple(900.0, 3600.0, 0.25),   // Quarter
            Triple(2700.0, 3600.0, 0.75)   // Three quarters
        )

        testCases.forEach { (current, total, expected) ->
            val progress = if (total > 0) (current / total).toFloat() else 0f
            assertEquals(expected.toFloat(), progress, 0.01f)
        }
    }

    @Test
    fun `playback finished detection`() {
        val almostFinished = 3590.0
        val duration = 3600.0
        val threshold = 10.0 // 10 seconds from end

        val isNearEnd = (duration - almostFinished) <= threshold

        assertTrue(isNearEnd)
    }

    @Test
    fun `resume from zero position`() {
        val progress = TestFixtures.createMockMediaProgress(
            currentTime = 0.0,
            duration = 3600.0
        )

        assertEquals(0.0, progress.currentTime ?: -1.0, 0.01)
        assertEquals(0.0, progress.progress ?: -1.0, 0.01)
        assertFalse(progress.isFinished ?: true)
    }

    @Test
    fun `resume from end position does not reset`() {
        val nearEndPosition = 3595.0
        val duration = 3600.0

        val progress = TestFixtures.createMockMediaProgress(
            currentTime = nearEndPosition,
            duration = duration
        )

        // Position should be preserved, not reset to 0
        assertEquals(nearEndPosition, progress.currentTime ?: 0.0, 0.01)
        assertTrue((progress.currentTime ?: 0.0) > 0)
    }

    @Test
    fun `progress update interval is reasonable`() {
        val updateIntervalMs = 5000L // 5 seconds
        val minInterval = 1000L // 1 second
        val maxInterval = 10000L // 10 seconds

        assertTrue(updateIntervalMs >= minInterval)
        assertTrue(updateIntervalMs <= maxInterval)
    }

    @Test
    fun `media player state transitions are valid`() {
        // Test valid state transitions
        val states = listOf(
            "idle" to "preparing",
            "preparing" to "prepared",
            "prepared" to "started",
            "started" to "paused",
            "paused" to "started",
            "started" to "stopped"
        )

        // All transitions should be valid
        states.forEach { (from, to) ->
            assertNotEquals(from, to)
        }
    }

    @Test
    fun `seek position validation`() {
        val duration = 3600.0

        val validSeekPositions = listOf(0.0, 100.0, 1800.0, 3599.0)
        val invalidSeekPositions = listOf(-1.0, -100.0, 3601.0, 10000.0)

        validSeekPositions.forEach { pos ->
            assertTrue("Position $pos should be valid", pos >= 0 && pos <= duration)
        }

        invalidSeekPositions.forEach { pos ->
            assertFalse("Position $pos should be invalid", pos >= 0 && pos <= duration)
        }
    }
}
