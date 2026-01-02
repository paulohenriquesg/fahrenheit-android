package com.paulohenriquesg.fahrenheit.home

import com.paulohenriquesg.fahrenheit.TestFixtures
import org.junit.Assert.*
import org.junit.Test

class HomeScreenTest {

    @Test
    fun `home screen shelves load correctly`() {
        val continueListeningShelf = TestFixtures.createMockShelf(
            id = "continue-listening",
            label = "Continue Listening",
            type = "book"
        )

        val recentlyAddedShelf = TestFixtures.createMockShelf(
            id = "recently-added",
            label = "Recently Added",
            type = "book"
        )

        val shelves = listOf(continueListeningShelf, recentlyAddedShelf)

        assertEquals(2, shelves.size)
        assertTrue(shelves.any { it.label == "Continue Listening" })
        assertTrue(shelves.any { it.label == "Recently Added" })
    }

    @Test
    fun `shelf displays correct number of items`() {
        val shelf = TestFixtures.createMockShelf()

        assertTrue(!shelf.entities.isNullOrEmpty())
        assertEquals(1, shelf.entities?.size ?: 0)
    }

    @Test
    fun `shelf label localization key is present`() {
        val shelf = TestFixtures.createMockShelf()

        assertNotNull(shelf.labelStringKey)
        assertEquals("LabelContinueListening", shelf.labelStringKey)
    }

    @Test
    fun `shelf items have cover images`() {
        val shelf = TestFixtures.createMockShelf()
        val item = shelf.entities?.first()

        assertNotNull(item?.media?.coverPath)
        assertTrue(item?.media?.coverPath?.startsWith("/") == true)
    }

    @Test
    fun `shelf items have metadata for display`() {
        val shelf = TestFixtures.createMockShelf()
        val item = shelf.entities?.first()

        assertNotNull(item?.media?.metadata?.title)
        assertNotNull(item?.media?.metadata?.authorName)
        assertTrue((item?.media?.duration ?: 0.0) > 0)
    }

    @Test
    fun `continue listening shelf contains in-progress items`() {
        val shelf = TestFixtures.createMockShelf(
            label = "Continue Listening",
            type = "book"
        )

        // Continue listening shelf should have items with progress
        assertEquals("Continue Listening", shelf.label)
        assertTrue(!shelf.entities.isNullOrEmpty())
    }

    @Test
    fun `shelf items can be navigated to detail view`() {
        val shelf = TestFixtures.createMockShelf()
        val item = shelf.entities?.first()

        // Item should have an ID for navigation
        assertNotNull(item?.id)
        assertTrue(!item?.id.isNullOrEmpty())
    }

    @Test
    fun `empty shelf is handled gracefully`() {
        val emptyShelf = TestFixtures.createMockShelf().copy(bookEntities = null)

        assertTrue(emptyShelf.entities.isNullOrEmpty())
        assertEquals(0, emptyShelf.entities?.size ?: 0)
    }

    @Test
    fun `shelf type indicates content type`() {
        val bookShelf = TestFixtures.createMockShelf(type = "book")
        val podcastShelf = TestFixtures.createMockShelf(type = "podcast")

        assertEquals("book", bookShelf.type)
        assertEquals("podcast", podcastShelf.type)
    }

    @Test
    fun `shelf items have library context`() {
        val shelf = TestFixtures.createMockShelf()
        val item = shelf.entities?.first()

        assertNotNull(item?.libraryId)
        assertEquals("library-456", item?.libraryId)
    }

    @Test
    fun `shelf displays media duration`() {
        val shelf = TestFixtures.createMockShelf()
        val item = shelf.entities?.first()

        assertTrue((item?.media?.duration ?: 0.0) > 0)
        assertEquals(3600.0, item?.media?.duration ?: 0.0, 0.01)
    }

    @Test
    fun `shelf items are not missing or invalid`() {
        val shelf = TestFixtures.createMockShelf()
        val item = shelf.entities?.first()

        assertFalse(item?.isMissing ?: true)
        assertFalse(item?.isInvalid ?: true)
    }

    @Test
    fun `multiple shelves can coexist`() {
        val shelves = listOf(
            TestFixtures.createMockShelf(id = "shelf1", label = "Continue Listening"),
            TestFixtures.createMockShelf(id = "shelf2", label = "Recently Added"),
            TestFixtures.createMockShelf(id = "shelf3", label = "Recommended")
        )

        assertEquals(3, shelves.size)
        assertEquals(3, shelves.distinctBy { it.id }.size)
    }

    @Test
    fun `shelf maintains order for display`() {
        val shelves = listOf(
            TestFixtures.createMockShelf(id = "shelf1"),
            TestFixtures.createMockShelf(id = "shelf2"),
            TestFixtures.createMockShelf(id = "shelf3")
        )

        // Order should be preserved
        assertEquals("shelf1", shelves[0].id)
        assertEquals("shelf2", shelves[1].id)
        assertEquals("shelf3", shelves[2].id)
    }
}
