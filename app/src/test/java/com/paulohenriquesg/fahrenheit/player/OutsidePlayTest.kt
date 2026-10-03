package com.paulohenriquesg.fahrenheit.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Play that does not come through the player screen - the remote's Play on
 * Home, say - is checked too (#144): when there is a question, the player
 * screen opens to ask it instead of playing over the other device's place.
 */
class OutsidePlayTest {

    private val offer = ResumeOffer(here = 3900.0, there = 4800.0, listenedAt = 9_000L)
    private var plays = 0
    private var opened = 0

    private fun outside(found: suspend () -> ResumeOffer?, canOpen: Boolean = true, timeoutMs: Long = 1_500) = OutsidePlay(
        scope = CoroutineScope(Dispatchers.Unconfined),
        check = found,
        play = { plays++ },
        openPlayer = { opened++; canOpen },
        timeoutMs = timeoutMs
    )

    @Test
    fun `nothing to ask plays`() {
        outside({ null }).request()

        assertEquals(1, plays)
        assertEquals(0, opened)
    }

    @Test
    fun `something to ask opens the player screen, and does not play`() {
        outside({ offer }).request()

        assertEquals(0, plays)
        assertEquals(1, opened)
    }

    // An activity cannot be started from the background: Play still plays.
    @Test
    fun `when the screen cannot be opened, it plays as before`() {
        outside({ offer }, canOpen = false).request()

        assertEquals(1, plays)
    }

    @Test
    fun `a server slow to answer does not swallow the press`() = runBlocking {
        outside({ awaitCancellation() }, timeoutMs = 50).request()
        delay(300)

        assertEquals(1, plays)
    }
}
