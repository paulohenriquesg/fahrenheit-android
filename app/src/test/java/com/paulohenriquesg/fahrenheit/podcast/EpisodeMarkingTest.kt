package com.paulohenriquesg.fahrenheit.podcast

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertTrue(done.await())
        assertEquals(mapOf("e1" to true), marking.marks)
    }

    @Test
    fun `a refused mark goes back to what it was`() = runBlocking {
        val marking = EpisodeMarking()
        marking.mark("e1", true) { true }

        val worked = marking.mark("e1", false) { false }

        assertFalse(worked)
        assertEquals(mapOf("e1" to true), marking.marks)
    }

    @Test
    fun `a refused first mark leaves nothing marked`() = runBlocking {
        val marking = EpisodeMarking()

        marking.mark("e1", true) { false }

        assertEquals(emptyMap<String, Boolean>(), marking.marks)
    }
}
