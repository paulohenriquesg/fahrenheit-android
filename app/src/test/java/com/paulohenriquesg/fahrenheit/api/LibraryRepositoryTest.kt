package com.paulohenriquesg.fahrenheit.api

import com.paulohenriquesg.fahrenheit.TestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Plain JUnit, no Android: fetching a library is not an Android concern.
 *
 * The screens these calls feed were written with Retrofit callbacks fired from
 * inside LaunchedEffect. enqueue is not cancelled when a composable leaves
 * composition, so a slow response could still write to state belonging to a
 * screen the user had already left. A suspend call in the same place IS
 * cancelled, which is the point of the migration.
 */
class LibraryRepositoryTest {

    private class FakeLibraryApi(
        private val libraries: (() -> LibrariesResponse)? = null,
        private val items: (() -> LibraryItemsResponse)? = null,
        private val shelves: (() -> List<Shelf>)? = null,
        private val library: (() -> Library)? = null,
        private val item: (() -> LibraryItemResponse)? = null
    ) : LibraryApi {
        var lastLibraryId: String? = null

        override suspend fun getLibraries(): LibrariesResponse =
            libraries?.invoke() ?: error("no libraries response configured")

        override suspend fun getLibraryItems(
            libraryId: String, sort: String, limit: Int?, page: Int?,
            desc: Boolean?, include: String, minified: Int
        ): LibraryItemsResponse {
            lastLibraryId = libraryId
            return items?.invoke() ?: error("no items response configured")
        }

        override suspend fun getPersonalizedView(
            libraryId: String, limit: Int, include: String
        ): List<Shelf> {
            lastLibraryId = libraryId
            return shelves?.invoke() ?: error("no shelves response configured")
        }

        override suspend fun getLibrary(libraryId: String) =
            library?.invoke() ?: error("no library configured")

        override suspend fun getLibraryItem(itemId: String, expanded: Int, include: String) =
            item?.invoke() ?: error("no item configured")
    }

    @Test
    fun `libraries are returned in display order`() = runBlocking {
        val api = FakeLibraryApi(libraries = {
            LibrariesResponse(
                listOf(
                    TestFixtures.createMockLibrary(id = "b", name = "Podcasts", displayOrder = 2),
                    TestFixtures.createMockLibrary(id = "a", name = "Books", displayOrder = 1)
                )
            )
        })

        val result = LibraryRepository(api).libraries()

        assertEquals(listOf("a", "b"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `a network failure is returned, not thrown`() = runBlocking {
        val api = FakeLibraryApi(libraries = { throw IOException("connection refused") })

        val result = LibraryRepository(api).libraries()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `library items are unwrapped from the response envelope`() = runBlocking {
        val api = FakeLibraryApi(items = {
            TestFixtures.createMockLibraryItemsResponse(
                listOf(TestFixtures.createMockLibraryItemEntry(id = "item-1"))
            )
        })

        val result = LibraryRepository(api).items("library-456")

        assertEquals(listOf("item-1"), result.getOrThrow().map { it.id })
        assertEquals("library-456", api.lastLibraryId)
    }

    @Test
    fun `personalized shelves are passed through`() = runBlocking {
        val api = FakeLibraryApi(shelves = { listOf(TestFixtures.createMockShelf(id = "shelf-1")) })

        val result = LibraryRepository(api).personalizedShelves("library-456")

        assertEquals(listOf("shelf-1"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `a failure fetching shelves does not surface as an empty shelf list`() = runBlocking {
        // An empty list and a failed request must stay distinguishable: the old
        // callback code reported both as "nothing to show".
        val api = FakeLibraryApi(shelves = { throw IOException("boom") })

        val result = LibraryRepository(api).personalizedShelves("library-456")

        assertTrue(result.isFailure)
    }

    @Test
    fun `a single library is returned`() = runBlocking {
        val api = FakeLibraryApi(library = { TestFixtures.createMockLibrary(id = "lib", name = "Books") })

        val library = LibraryRepository(api).library("lib").getOrThrow()

        assertEquals("Books", library.name)
    }

    @Test
    fun `a failure fetching one library is returned, not thrown`() = runBlocking {
        val api = FakeLibraryApi(library = { throw IOException("offline") })

        assertTrue(LibraryRepository(api).library("lib").isFailure)
    }

    @Test
    fun `one item is returned with its media`() = runBlocking {
        val api = FakeLibraryApi(item = {
            com.google.gson.Gson().fromJson(
                """{"id":"li1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
                    "isFile":true,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
                    "isMissing":false,"isInvalid":false,"mediaType":"book",
                    "media":{"metadata":{"title":"Dune"},"tags":[],"chapters":[],"episodes":[]},
                    "libraryFiles":[]}""",
                LibraryItemResponse::class.java
            )
        })

        val item = LibraryRepository(api).item("li1").getOrThrow()

        assertEquals("Dune", item.media.metadata.title)
    }

    @Test
    fun `a failure loading one item is returned, not thrown`() = runBlocking {
        val api = FakeLibraryApi(item = { throw IOException("offline") })

        assertTrue(LibraryRepository(api).item("li1").isFailure)
    }
}
