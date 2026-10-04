package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the question is asked and what each answer does, apart from the screen
 * (#90): races between a check and the listener, a slow server, the remote's
 * key arriving as a down and an up.
 */
class ResumePromptTest {

    private val offer = ResumeOffer(here = 3900.0, there = 4800.0, listenedAt = 9_000L)
    private var playWhenReady = false
    private var plays = 0
    private val seeks = mutableListOf<Double>()
    private val answered = mutableListOf<ResumeOffer>()
    private var checks = 0
    private var result: suspend () -> ResumeOffer? = { offer }

    private fun prompt(timeoutMs: Long = 1_500) = ResumePrompt(
        scope = CoroutineScope(Dispatchers.Unconfined),
        check = { checks++; result() },
        playWhenReady = { playWhenReady },
        play = { plays++; playWhenReady = true },
        seek = { seeks += it },
        answered = { answered += it },
        timeoutMs = timeoutMs
    )

    // State written outside any composition; left pending, it kept later
    // Compose tests in this JVM from going idle.
    @After
    fun applyStateWrites() = Snapshot.sendApplyNotifications()

    @Test
    fun `Play with nothing to ask plays`() {
        result = { null }
        val p = prompt()

        p.onPlay()

        assertEquals(1, plays)
        assertNull(p.offer)
    }

    @Test
    fun `Play with something to ask asks first, and plays after the answer`() {
        val p = prompt()

        p.onPlay()
        assertEquals(offer, p.offer)
        assertEquals(0, plays)

        p.answer(moveThere = true)

        assertEquals(listOf(4800.0), seeks)
        assertEquals(1, plays)
        assertEquals(listOf(offer), answered)
        assertNull(p.offer)
    }

    @Test
    fun `staying plays from here without seeking`() {
        val p = prompt()
        p.onPlay()

        p.answer(moveThere = false)

        assertTrue(seeks.isEmpty())
        assertEquals(1, plays)
    }

    // Coming back to a paused book: the answer does not start it.
    @Test
    fun `asked on coming back, an answer leaves it paused`() {
        val p = prompt()

        p.onReattach()
        assertEquals(offer, p.offer)
        p.answer(moveThere = true)

        assertEquals(listOf(4800.0), seeks)
        assertEquals(0, plays)
    }

    // A chapter chosen on the details screen starts playing as the screen
    // comes back; that is the listener's choice, not a question.
    @Test
    fun `coming back to something set to play is not checked`() {
        playWhenReady = true

        prompt().onReattach()

        assertEquals(0, checks)
    }

    @Test
    fun `a check that answers after playback started is dropped`() {
        val late = CompletableDeferred<ResumeOffer?>()
        result = { late.await() }
        val p = prompt()
        p.onReattach()

        playWhenReady = true
        late.complete(offer)

        assertNull(p.offer)
    }

    @Test
    fun `Play during the check on coming back makes one check, not two`() {
        val first = CompletableDeferred<ResumeOffer?>()
        var calls = 0
        result = { if (++calls == 1) first.await() else null }
        val p = prompt()
        p.onReattach()

        p.onPlay()
        first.complete(offer)

        assertEquals(1, plays)
        assertNull(p.offer)
    }

    // The server not answering must not leave Play doing nothing.
    @Test
    fun `a check that takes too long plays anyway`() = runBlocking {
        result = { awaitCancellation() }
        val p = prompt(timeoutMs = 50)

        p.onPlay()
        delay(300)

        assertEquals(1, plays)
    }

    @Test
    fun `the remote's Play from a pause is taken, down and up`() {
        result = { null }
        val p = prompt()

        assertTrue(p.onKey(down = true))
        assertTrue(p.onKey(down = false))
        assertEquals(1, plays)
    }

    // The up of a press that started playback goes with its down, not to the
    // media session on its own.
    @Test
    fun `a key whose down was taken has its up taken too`() {
        result = { null }
        val p = prompt()

        p.onKey(down = true)
        assertTrue(playWhenReady)

        assertTrue(p.onKey(down = false))
    }

    @Test
    fun `while playing, the remote's Play is left to the media session`() {
        playWhenReady = true
        val p = prompt()

        assertFalse(p.onKey(down = true))
        assertFalse(p.onKey(down = false))
    }

    @Test
    fun `while the question shows, the remote's Play does not play behind it`() {
        val p = prompt()
        p.onPlay()

        assertTrue(p.onKey(down = true))
        assertTrue(p.onKey(down = false))
        assertEquals(0, plays)
    }

    // Review (#144): opened by a Play from outside, the screen asks and then
    // plays: the press is not lost.
    @Test
    fun `asked on coming back for a Play, the answer plays`() {
        val p = prompt()

        p.onReattach(playAfter = true)
        p.answer(moveThere = false)

        assertEquals(1, plays)
    }

    @Test
    fun `opened for a Play with nothing left to ask, it plays`() {
        result = { null }

        prompt().onReattach(playAfter = true)

        assertEquals(1, plays)
    }
}
