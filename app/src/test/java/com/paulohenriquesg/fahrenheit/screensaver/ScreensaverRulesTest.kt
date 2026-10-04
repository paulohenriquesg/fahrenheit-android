package com.paulohenriquesg.fahrenheit.screensaver

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the listening screensaver runs, and what it does with keys (#156). */
class ScreensaverRulesTest {

    private val minute = 60_000L

    // --- when it shows, and when the screen is kept on ---

    @Test
    fun `while playing, it shows after the chosen minutes without a key`() {
        assertFalse(ScreensaverPolicy.decide(delayMinutes = 5, playing = true, idleMs = 4 * minute, notPlayingMs = null).show)
        assertTrue(ScreensaverPolicy.decide(delayMinutes = 5, playing = true, idleMs = 5 * minute, notPlayingMs = null).show)
    }

    @Test
    fun `while playing, the screen is kept on, so the system screensaver does not start`() =
        assertTrue(ScreensaverPolicy.decide(5, playing = true, idleMs = 0, notPlayingMs = null).keepScreenOn)

    @Test
    fun `when Off, nothing is kept on and nothing shows, as today`() {
        val decision = ScreensaverPolicy.decide(delayMinutes = null, playing = true, idleMs = 60 * minute, notPlayingMs = null)
        assertFalse(decision.keepScreenOn)
        assertFalse(decision.show)
    }

    @Test
    fun `with nothing playing at all, the system screensaver and sleep run as today`() {
        val decision = ScreensaverPolicy.decide(5, playing = false, idleMs = 60 * minute, notPlayingMs = null)
        assertFalse(decision.keepScreenOn)
        assertFalse(decision.show)
    }

    // Pausing ends the reason; the system takes over after the same delay.
    @Test
    fun `after a pause the screen is kept on only for the same delay`() {
        assertTrue(ScreensaverPolicy.decide(5, playing = false, idleMs = 6 * minute, notPlayingMs = 4 * minute).keepScreenOn)
        val later = ScreensaverPolicy.decide(5, playing = false, idleMs = 7 * minute, notPlayingMs = 5 * minute)
        assertFalse(later.keepScreenOn)
        assertFalse(later.show)
    }

    // --- keys ---

    @Test
    fun `any key resets the idle time`() {
        var now = 0L
        val gate = KeyGate { now }
        now = 4 * minute
        gate.key(down = true)

        assertEquals(0L, gate.idleMs())
        now = 5 * minute
        assertEquals(minute, gate.idleMs())
    }

    @Test
    fun `a key while it shows only wakes the screen, and is not acted on`() {
        var now = 0L
        val gate = KeyGate { now }
        gate.showing = true

        assertTrue("the press is eaten", gate.key(down = true))
        assertFalse(gate.showing)
        assertTrue("and so is its release", gate.key(down = false))
        assertFalse("the next press goes through", gate.key(down = true))
    }

    @Test
    fun `a key while it does not show goes through`() =
        assertFalse(KeyGate { 0L }.key(down = true))

    // --- what moves ---

    @Test
    fun `the now-playing line moves to another corner every minute`() {
        val corners = (0 until 5).map { NowPlayingCorner.at(it * minute + 1) }

        assertEquals(4, corners.take(4).toSet().size)
        corners.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", a != b) }
        assertEquals(NowPlayingCorner.at(1), NowPlayingCorner.at(30_000))
    }

    @Test
    fun `the bouncing cover stays on the screen and turns at its edges`() {
        val area = 1000f to 600f
        val cover = 200f
        // Sampled every 0.1 s over half an hour: near each edge means within
        // one sample's travel of it.
        val path = (0..18_000L).map { Bounce.at(it * 100L, area.first, area.second, cover) }
        val step = 10f

        assertTrue(path.all { it.first in 0f..(area.first - cover) && it.second in 0f..(area.second - cover) })
        assertTrue("reaches the right edge", path.any { it.first >= area.first - cover - step })
        assertTrue("comes back to the left edge", path.drop(1).any { it.first <= step })
        assertTrue("reaches the bottom", path.any { it.second >= area.second - cover - step })
    }

    // The wall itself (#168's CoverWall) repeats covers to fill the screen.
    @Test
    fun `the wall shows the series being played first, then the library, each once`() {
        val series = listOf(item("s1"), item("s2"))
        val library = listOf(item("l1"), item("s1"), item("l2"))

        val wall = WallCovers.pick(series, library) { it.id }

        assertEquals(listOf("s1", "s2", "l1", "l2"), wall.map { it.id })
    }

    @Test
    fun `an empty wall stays empty rather than inventing covers`() =
        assertTrue(WallCovers.pick(emptyList<LibraryItem>(), emptyList()) { it.id }.isEmpty())

    // --- one wait at a time: no timer that ticks for ever ---

    @Test
    fun `while playing, the next change is when it should show, then none`() {
        assertEquals(3 * minute, ScreensaverPolicy.nextChangeMs(5, playing = true, idleMs = 2 * minute, notPlayingMs = null))
        assertEquals(null, ScreensaverPolicy.nextChangeMs(5, playing = true, idleMs = 6 * minute, notPlayingMs = null))
    }

    @Test
    fun `after a pause, the next change is the sooner of showing and letting go`() {
        // 4 min idle, paused 2 min ago: it shows in 1 min, and lets go in 3.
        assertEquals(minute, ScreensaverPolicy.nextChangeMs(5, playing = false, idleMs = 4 * minute, notPlayingMs = 2 * minute))
        // Already showing: only letting go is left.
        assertEquals(3 * minute, ScreensaverPolicy.nextChangeMs(5, playing = false, idleMs = 6 * minute, notPlayingMs = 2 * minute))
        // Let go: nothing more.
        assertEquals(null, ScreensaverPolicy.nextChangeMs(5, playing = false, idleMs = 6 * minute, notPlayingMs = 5 * minute))
    }

    @Test
    fun `Off or nothing playing waits for nothing`() {
        assertEquals(null, ScreensaverPolicy.nextChangeMs(null, playing = true, idleMs = 0, notPlayingMs = null))
        assertEquals(null, ScreensaverPolicy.nextChangeMs(5, playing = false, idleMs = 0, notPlayingMs = null))
    }

    private fun item(id: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"T"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )
}
