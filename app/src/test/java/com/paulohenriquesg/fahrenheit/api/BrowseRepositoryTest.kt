package com.paulohenriquesg.fahrenheit.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Plain JUnit: browsing a library is not an Android concern.
 *
 * These endpoints do not agree on an envelope - series and collections come
 * back under "results", authors under "authors". A screen reading the wrong one
 * is what broke the Authors list before (fixed in 283e66c). Normalising here
 * means a screen cannot get it wrong again, and the shape is pinned by tests
 * rather than by whichever screen happened to be looked at last.
 */
class BrowseRepositoryTest {

    private class FakeBrowseApi(
        private val series: (() -> SeriesResponse)? = null,
        private val collections: (() -> CollectionsResponse)? = null,
        private val authors: (() -> AuthorsResponse)? = null,
        private val stats: (() -> ListeningStatsResponse)? = null
    ) : BrowseApi {
        override suspend fun getLibrarySeries(libraryId: String, limit: Int, minified: Int) =
            series?.invoke() ?: error("no series configured")

        override suspend fun getLibraryCollections(libraryId: String, limit: Int, minified: Int) =
            collections?.invoke() ?: error("no collections configured")

        override suspend fun getLibraryAuthors(libraryId: String, limit: Int, minified: Int) =
            authors?.invoke() ?: error("no authors configured")

        override suspend fun getListeningStats() =
            stats?.invoke() ?: error("no stats configured")
    }

    @Test
    fun `series are unwrapped from the results envelope`() = runBlocking {
        val api = FakeBrowseApi(series = {
            SeriesResponse(results = listOf(Series(id = "s1", name = "Discworld")))
        })

        assertEquals(listOf("s1"), BrowseRepository(api).series("lib").getOrThrow().map { it.id })
    }

    @Test
    fun `collections are unwrapped from the results envelope`() = runBlocking {
        val api = FakeBrowseApi(collections = {
            CollectionsResponse(
                results = listOf(
                    Collection(
                        id = "c1", libraryId = "lib", name = "Favourites",
                        lastUpdate = 0L, createdAt = 0L
                    )
                )
            )
        })

        assertEquals(listOf("c1"), BrowseRepository(api).collections("lib").getOrThrow().map { it.id })
    }

    @Test
    fun `authors are unwrapped from their differently-named envelope`() = runBlocking {
        // Authors come back under "authors", not "results" - the mismatch that
        // broke this screen before.
        val api = FakeBrowseApi(authors = {
            AuthorsResponse(authors = listOf(Author(id = "a1", name = "Pratchett")))
        })

        assertEquals(listOf("a1"), BrowseRepository(api).authors("lib").getOrThrow().map { it.id })
    }

    @Test
    fun `listening stats are passed through`() = runBlocking {
        val api = FakeBrowseApi(stats = {
            ListeningStatsResponse(
                totalTime = 3600.0, items = emptyMap(), days = emptyMap(),
                dayOfWeek = emptyMap(), today = 60.0
            )
        })

        assertEquals(3600.0, BrowseRepository(api).listeningStats().getOrThrow().totalTime, 0.0)
    }

    @Test
    fun `a failure is returned, not thrown`() = runBlocking {
        val api = FakeBrowseApi(series = { throw IOException("connection refused") })

        val result = BrowseRepository(api).series("lib")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `an empty library is a success, not a failure`() = runBlocking {
        val api = FakeBrowseApi(collections = { CollectionsResponse(results = emptyList()) })

        val result = BrowseRepository(api).collections("lib")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Collection>(), result.getOrThrow())
    }
}
