package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When to ask which position to continue from (#90): the server's is newer
 * than anything this player knows, more than 30 seconds away, and was not
 * written by this device.
 */
class ResumeOfferTest {

    private val now = 1_700_000_000_000L
    private val tenMinutesAgo = now - 600_000

    private fun server(at: Double?, lastUpdate: Long? = now - 60_000, finished: Boolean = false) =
        MediaProgressResponse(currentTime = at, lastUpdate = lastUpdate, isFinished = finished)

    private fun offer(
        here: Double = 3900.0,
        server: MediaProgressResponse? = server(4800.0),
        known: KnownProgress? = KnownProgress.ServerCopy(tenMinutesAgo),
        latestDevice: String? = "phone-1"
    ) = ResumeOffer.of(here, server, known, latestDevice, thisDevice = "tv-1")

    @Test
    fun `a newer position from another device, far enough away, is offered`() {
        assertEquals(ResumeOffer(here = 3900.0, there = 4800.0, listenedAt = now - 60_000), offer())
    }

    // Decided on #90: a flat 30 seconds, and exactly 30 does not ask.
    @Test
    fun `thirty seconds apart is not worth asking`() {
        assertNull(offer(here = 4770.0))
        assertNull(offer(here = 4830.0))
    }

    @Test
    fun `just over thirty seconds is`() {
        assertEquals(4800.0, offer(here = 4769.0)!!.there, 0.0)
    }

    @Test
    fun `a position behind this one is offered too, when it is newer`() {
        assertEquals(3000.0, offer(server = server(3000.0))!!.there, 0.0)
    }

    @Test
    fun `a server position no newer than this player knows is not offered`() {
        assertNull(offer(server = server(4800.0, lastUpdate = tenMinutesAgo)))
        assertNull(offer(server = server(4800.0, lastUpdate = tenMinutesAgo - 1)))
    }

    // The queue can predate this process: then any server position is newer.
    @Test
    fun `with nothing known, the server's position counts as newer`() {
        assertEquals(4800.0, offer(known = null)!!.there, 0.0)
    }

    @Test
    fun `listening on this same device is not asked about`() {
        assertNull(offer(latestDevice = "tv-1"))
    }

    // The server's copy is newer than anything this player knows; asking is
    // the safe side when the sessions cannot say who wrote it.
    @Test
    fun `when the sessions cannot be read, it asks`() {
        assertEquals(4800.0, offer(latestDevice = null)!!.there, 0.0)
    }

    // An unreadable position is #16's notice, not a choice between two.
    @Test
    fun `nothing to compare is nothing to ask`() {
        assertNull(offer(server = null))
        assertNull(offer(server = server(null)))
        assertNull(offer(server = server(4800.0, lastUpdate = null)))
    }

    @Test
    fun `a position marked finished is not offered`() {
        assertNull(offer(server = server(4800.0, finished = true)))
    }

    // #145: the server keeps the last write. Back at what this player wrote,
    // nobody wrote after it, however far apart the two clocks are.
    @Test
    fun `at the position this player last wrote, nothing is asked`() {
        assertNull(offer(here = 3900.0, server = server(4800.0, lastUpdate = now + 3_600_000), known = KnownProgress.Wrote(4800.0)))
    }

    // #145: a TV clock running ahead made this look older than the last report.
    @Test
    fun `a position other than the one this player wrote came after it, whatever its time says`() {
        val offer = offer(server = server(4800.0, lastUpdate = 1L), known = KnownProgress.Wrote(3900.0))

        assertEquals(4800.0, offer!!.there, 0.0)
    }

    @Test
    fun `within a second of what was written is the same write`() {
        assertNull(offer(here = 3000.0, server = server(4800.6), known = KnownProgress.Wrote(4800.0)))
    }

    // The fallback, for a start chosen on the details screen: the TV's clock,
    // with a minute's margin for the two clocks to disagree.
    @Test
    fun `against the TV's clock, a minute's margin`() {
        val since = KnownProgress.Since(now)

        assertNull(offer(server = server(4800.0, lastUpdate = now + 60_000), known = since))
        assertEquals(4800.0, offer(server = server(4800.0, lastUpdate = now + 60_001), known = since)!!.there, 0.0)
    }
}
