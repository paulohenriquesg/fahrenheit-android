package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What this player last knew of the server's copy of each item (#90, #145). */
class ServerKnowledgeTest {

    // Events arrive in order, and times of different kinds do not compare.
    @Test
    fun `the latest event wins, whatever its kind`() {
        val knowledge = ServerKnowledge()

        knowledge.read("b1", null, lastUpdate = 9_000)
        knowledge.wrote("b1", null, position = 1200.0)
        assertEquals(KnownProgress.Wrote(1200.0), knowledge.known("b1", null))

        knowledge.since("b1", null, deviceTime = 5)
        assertEquals(KnownProgress.Since(5), knowledge.known("b1", null))

        knowledge.read("b1", null, lastUpdate = 1_000)
        assertEquals(KnownProgress.ServerCopy(1_000), knowledge.known("b1", null))
    }

    @Test
    fun `each book and episode is its own`() {
        val knowledge = ServerKnowledge()

        knowledge.wrote("p1", "e1", 60.0)

        assertNull(knowledge.known("p1", "e2"))
        assertNull(knowledge.known("p1", null))
        assertEquals(KnownProgress.Wrote(60.0), knowledge.known("p1", "e1"))
    }
}
