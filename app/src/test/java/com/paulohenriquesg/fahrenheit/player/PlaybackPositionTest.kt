package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * The arithmetic behind the player's scrubber, pulled out of the composable
 * that repeated it at five call sites.
 */
class PlaybackPositionTest {

    private lateinit var locale: Locale

    @Before fun save() { locale = Locale.getDefault() }
    @After fun restore() { Locale.setDefault(locale) }

    @Test
    fun `progress is the fraction played`() =
        assertEquals(0.25f, PlaybackPosition.fraction(current = 900.0, total = 3600.0), 1e-6f)

    // A book whose duration is unknown yet, or reported as zero.
    @Test
    fun `no duration means no progress, not a division by zero`() {
        assertEquals(0f, PlaybackPosition.fraction(current = 900.0, total = 0.0), 0f)
        assertEquals(0f, PlaybackPosition.fraction(current = 900.0, total = -1.0), 0f)
    }

    // Some servers report a duration shorter than the file really is.
    @Test
    fun `a position past the end still fills the bar exactly once`() =
        assertEquals(1f, PlaybackPosition.fraction(current = 4000.0, total = 3600.0), 0f)

    @Test
    fun `skipping forward stops at the end`() =
        assertEquals(3600.0, PlaybackPosition.skip(current = 3590.0, by = 30.0, total = 3600.0), 1e-9)

    @Test
    fun `skipping back stops at the beginning`() =
        assertEquals(0.0, PlaybackPosition.skip(current = 10.0, by = -30.0, total = 3600.0), 1e-9)

    @Test
    fun `skipping in the middle moves by the step`() =
        assertEquals(930.0, PlaybackPosition.skip(current = 900.0, by = 30.0, total = 3600.0), 1e-9)

    @Test
    fun `chapter marks sit where each chapter ends`() {
        val chapters = listOf(
            Chapter(start = 0.0, end = 900.0),
            Chapter(start = 900.0, end = 1800.0),
            Chapter(start = 1800.0, end = 3600.0)
        )

        // The last chapter ends at the end of the book: no mark to draw there.
        assertEquals(listOf(25f, 50f), PlaybackPosition.chapterMarks(chapters, total = 3600.0))
    }

    @Test
    fun `chapter marks need a duration to be placed against`() =
        assertEquals(emptyList<Float>(), PlaybackPosition.chapterMarks(listOf(Chapter(end = 10.0)), total = 0.0))

    // 20 books on the test server report a duration longer than their audio,
    // which put marks off the end of the track.
    @Test
    fun `a chapter ending past the duration is not drawn`() {
        val chapters = listOf(Chapter(end = 900.0), Chapter(end = 9000.0), Chapter(end = 3600.0))

        assertEquals(listOf(25f), PlaybackPosition.chapterMarks(chapters, total = 3600.0))
    }

    @Test
    fun `chapters without an end are skipped`() =
        assertEquals(emptyList<Float>(), PlaybackPosition.chapterMarks(listOf(Chapter(end = null), Chapter(end = null)), total = 3600.0))

    @Test
    fun `the clock reads hours, minutes and seconds`() =
        assertEquals("01:02:03", PlaybackPosition.clock(3723.0))

    @Test
    fun `a position before the start reads as zero`() =
        assertEquals("00:00:00", PlaybackPosition.clock(-5.0))

    // Same reason the duration formatter uses a fixed locale: the separators
    // are ASCII, so the digits have to be too.
    @Test
    fun `the clock keeps Latin digits on a device with its own numerals`() {
        Locale.setDefault(Locale.forLanguageTag("fa"))

        assertEquals("01:02:03", PlaybackPosition.clock(3723.0))
    }
}
