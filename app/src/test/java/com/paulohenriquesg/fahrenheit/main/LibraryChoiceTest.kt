package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryChoiceTest {

    private val books = TestFixtures.createMockLibrary(id = "books", name = "Books")
    private val podcasts = TestFixtures.createMockLibrary(id = "podcasts", name = "Podcasts")

    @Test
    fun `the library chosen last time is opened again`() =
        assertEquals("podcasts", LibraryChoice.pick(listOf(books, podcasts), savedId = "podcasts")?.id)

    @Test
    fun `with nothing chosen before, the first library opens`() =
        assertEquals("books", LibraryChoice.pick(listOf(books, podcasts), savedId = null)?.id)

    // A library can be deleted or renamed on the server between sessions.
    @Test
    fun `a saved library that is gone falls back to the first`() =
        assertEquals("books", LibraryChoice.pick(listOf(books, podcasts), savedId = "deleted")?.id)

    @Test
    fun `a server with no libraries chooses nothing`() =
        assertNull(LibraryChoice.pick(emptyList(), savedId = "books"))
}
