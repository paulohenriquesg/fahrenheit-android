package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Gathers what [ResumeOffer.of] needs for one item, and remembers what the
 * listener chose so the same server position is not asked about twice (#90).
 */
class ResumeCheckTest {

    private val knowledge = ServerKnowledge()
    private var asked = 0
    private var server = MediaProgressResponse(currentTime = 4800.0, lastUpdate = 9_000L)
    private var device: String? = "phone-1"
    private var sessionsRead = 0

    private val check = ResumeCheck(
        progress = { _, _ -> asked++; server },
        latestDevice = { _, _ -> sessionsRead++; device },
        thisDevice = "tv-1",
        knowledge = knowledge
    )

    @Test
    fun `a newer position from elsewhere is offered`() {
        knowledge.saw("b1", null, 1_000L)

        val offer = runBlocking { check.offer("b1", null, here = 3900.0, playing = false) }

        assertEquals(ResumeOffer(here = 3900.0, there = 4800.0, listenedAt = 9_000L), offer)
    }

    // Playing in the background, its own reports are the newest there are.
    @Test
    fun `a player that is playing is not checked`() {
        assertNull(runBlocking { check.offer("b1", null, here = 3900.0, playing = true) })
        assertEquals(0, asked)
    }

    // The sessions are a second request; not made when the answer is no anyway.
    @Test
    fun `the sessions are read only when everything else says ask`() {
        server = MediaProgressResponse(currentTime = 3910.0, lastUpdate = 9_000L)

        assertNull(runBlocking { check.offer("b1", null, here = 3900.0, playing = false) })
        assertEquals(0, sessionsRead)
    }

    @Test
    fun `listening on this device is not offered`() {
        device = "tv-1"

        assertNull(runBlocking { check.offer("b1", null, here = 3900.0, playing = false) })
    }

    // Review Focus 2: Stay, then Play.
    @Test
    fun `once answered, the same server position is not offered again`() {
        val offer = runBlocking { check.offer("b1", null, here = 3900.0, playing = false) }!!

        check.answered("b1", null, offer)

        assertNull(runBlocking { check.offer("b1", null, here = 3900.0, playing = false) })
    }

    @Test
    fun `a position heard elsewhere after the answer is offered again`() {
        val offer = runBlocking { check.offer("b1", null, here = 3900.0, playing = false) }!!
        check.answered("b1", null, offer)

        server = MediaProgressResponse(currentTime = 6000.0, lastUpdate = 20_000L)

        assertEquals(6000.0, runBlocking { check.offer("b1", null, here = 3900.0, playing = false) }!!.there, 0.0)
    }
}
