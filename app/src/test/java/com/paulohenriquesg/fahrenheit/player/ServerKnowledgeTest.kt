package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When this player last knew the server's state of an item (#90). */
class ServerKnowledgeTest {

    @Test
    fun `the later time is kept`() {
        val knowledge = ServerKnowledge()

        knowledge.saw("b1", null, 2000)
        knowledge.saw("b1", null, 1000)

        assertEquals(2000L, knowledge.knownAt("b1", null))
    }

    @Test
    fun `each book and episode is its own`() {
        val knowledge = ServerKnowledge()

        knowledge.saw("p1", "e1", 2000)

        assertNull(knowledge.knownAt("p1", "e2"))
        assertNull(knowledge.knownAt("p1", null))
        assertEquals(2000L, knowledge.knownAt("p1", "e1"))
    }
}
