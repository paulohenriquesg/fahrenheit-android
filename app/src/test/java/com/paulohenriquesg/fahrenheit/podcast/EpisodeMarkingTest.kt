package com.paulohenriquesg.fahrenheit.podcast

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** A mark shows at once and goes back if the server refuses it (#181). */
class EpisodeMarkingTest {

    @Test
    fun `the mark shows before the server answers, and stays when it agrees`() = runBlocking {
        val marking = EpisodeMarking()
        val answer = CompletableDeferred<Boolean>()

        val done = async { marking.mark("e1", true) { answer.await() } }
        yield()
        assertEquals(mapOf("e1" to true), marking.marks)

        answer.complete(true)
        assertEquals(true, done.await())
        assertEquals(mapOf("e1" to true), marking.marks)
    }

    @Test
    fun `a refused mark goes back to what it was`() = runBlocking {
        val marking = EpisodeMarking()
        marking.mark("e1", true) { true }

        val worked = marking.mark("e1", false) { false }

        assertEquals(false, worked)
        assertEquals(mapOf("e1" to true), marking.marks)
    }

    @Test
    fun `a refused first mark leaves nothing marked`() = runBlocking {
        val marking = EpisodeMarking()

        marking.mark("e1", true) { false }

        assertEquals(emptyMap<String, Boolean>(), marking.marks)
    }

    // Review: a second press while the first is out would race it to the server.
    @Test
    fun `a press while that episode's mark is out is ignored`() = runBlocking {
        val marking = EpisodeMarking()
        val answer = CompletableDeferred<Boolean>()
        val first = async { marking.mark("e1", true) { answer.await() } }
        yield()
        var sent = false

        val second = marking.mark("e1", false) { sent = true; true }

        assertNull(second)
        assertFalse(sent)
        assertEquals(mapOf("e1" to true), marking.marks)
        answer.complete(true)
        assertEquals(true, first.await())
    }

    // Review: once the server's progress is read again, it says what is so -
    // playing a finished episode un-finishes it there.
    @Test
    fun `settling lets go of the marks the server has answered, and keeps those still out`() = runBlocking {
        val marking = EpisodeMarking()
        marking.mark("e1", true) { true }
        val answer = CompletableDeferred<Boolean>()
        val out = async { marking.mark("e2", true) { answer.await() } }
        yield()

        marking.settle()

        assertEquals(mapOf("e2" to true), marking.marks)
        answer.complete(true)
        out.await()
        Unit
    }
}
