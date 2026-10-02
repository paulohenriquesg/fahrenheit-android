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

    // Frame 4: lengths read as words, not a clock, and what is left matters
    // more than what has passed on a 16-hour book (#93).
    @Test
    fun `a length over an hour reads in hours and minutes`() =
        assertEquals("5 h 28 min", PlaybackPosition.spoken(19_680.0))

    @Test
    fun `a whole hour still says its minutes`() =
        assertEquals("1 h 0 min", PlaybackPosition.spoken(3_600.0))

    @Test
    fun `under an hour the seconds show, so a short episode visibly moves`() =
        assertEquals("12 min 30 s", PlaybackPosition.spoken(750.0))

    @Test
    fun `under a minute it is only seconds`() =
        assertEquals("45 s", PlaybackPosition.spoken(45.0))

    @Test
    fun `a position before the start reads as zero`() =
        assertEquals("0 s", PlaybackPosition.spoken(-5.0))

    @Test
    fun `what is left is the rest of the book`() =
        assertEquals("10 h 42 min", PlaybackPosition.spoken(PlaybackPosition.left(current = 19_680.0, total = 58_200.0)))

    @Test
    fun `nothing is left past the end`() =
        assertEquals(0.0, PlaybackPosition.left(current = 6_000.0, total = 5_400.0), 0.0)

    // The digits are composed with Latin units, so they have to be Latin too.
    @Test
    fun `lengths keep Latin digits on a device with its own numerals`() {
        Locale.setDefault(Locale.forLanguageTag("fa"))

        assertEquals("5 h 28 min", PlaybackPosition.spoken(19_680.0))
    }
}
