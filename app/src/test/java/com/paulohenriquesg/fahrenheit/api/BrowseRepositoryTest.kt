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
        private val stats: (() -> ListeningStatsResponse)? = null,
        private val search: (() -> SearchLibraryItemsResponse)? = null,
        private val author: (() -> AuthorDetailResponse)? = null,
        private val recentEpisodes: (() -> RecentEpisodesResponse)? = null
    ) : BrowseApi {
        override suspend fun getLibrarySeries(libraryId: String, limit: Int, minified: Int) =
            series?.invoke() ?: error("no series configured")

        override suspend fun getLibraryCollections(libraryId: String, limit: Int, minified: Int) =
            collections?.invoke() ?: error("no collections configured")

        override suspend fun getLibraryAuthors(libraryId: String, limit: Int, minified: Int) =
            authors?.invoke() ?: error("no authors configured")

        override suspend fun getListeningStats() =
            stats?.invoke() ?: error("no stats configured")

        override suspend fun searchLibraryItems(libraryId: String, query: String, limit: Int) =
            search?.invoke() ?: error("no search configured")

        override suspend fun getAuthor(authorId: String, include: String) =
            author?.invoke() ?: error("no author configured")

        override suspend fun getRecentEpisodes(libraryId: String, limit: Int) =
            recentEpisodes?.invoke() ?: error("no recent episodes configured")
    }

    // Built from JSON: a search match carries a whole library item, and the
    // fields that matter here are the ones the server actually sends.
    private fun item(id: String) = com.google.gson.Gson().fromJson(
        """{"libraryItem":{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p",
             "relPath":"p","isFile":true,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,
             "updatedAt":0,"isMissing":false,"isInvalid":false,"mediaType":"book",
             "media":{"metadata":{"title":"T"},"tags":[],"numTracks":0,"numAudioFiles":0,
             "numChapters":0,"duration":0.0,"size":0},"numFiles":1,"size":1}}""",
        SearchBookItem::class.java
    )

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

    @Test
    fun `a book search returns the library items behind the matches`() = runBlocking {
        val api = FakeBrowseApi(search = {
            SearchLibraryItemsResponse(book = listOf(item("b1"), item("b2")))
        })

        val found = BrowseRepository(api).search("lib", "dune", mediaType = "book").getOrThrow()

        assertEquals(listOf("b1", "b2"), found.map { it.id })
    }

    @Test
    fun `a podcast library searches its podcasts`() = runBlocking {
        val api = FakeBrowseApi(search = {
            SearchLibraryItemsResponse(book = listOf(item("b1")), podcast = listOf(item("p1")))
        })

        val found = BrowseRepository(api).search("lib", "news", mediaType = "podcast").getOrThrow()

        assertEquals(listOf("p1"), found.map { it.id })
    }

    // Nothing found has to reach the screen, or it keeps showing the last
    // search's results as though they matched.
    @Test
    fun `a search that matches nothing returns no items rather than nothing at all`() = runBlocking {
        val api = FakeBrowseApi(search = { SearchLibraryItemsResponse() })

        assertEquals(emptyList<String>(), BrowseRepository(api).search("lib", "zzz", "book").getOrThrow().map { it.id })
    }

    @Test
    fun `a search failure is returned, not thrown`() = runBlocking {
        val api = FakeBrowseApi(search = { throw IOException("offline") })

        assertTrue(BrowseRepository(api).search("lib", "dune", "book").isFailure)
    }

    @Test
    fun `an author comes back with the books behind them`() = runBlocking {
        val api = FakeBrowseApi(author = {
            AuthorDetailResponse(
                id = "a1", name = "Terry Pratchett", libraryId = "lib",
                addedAt = 0, updatedAt = 0,
                libraryItems = listOf(item("b1").libraryItem!!)
            )
        })

        val author = BrowseRepository(api).author("a1").getOrThrow()

        assertEquals("Terry Pratchett", author.name)
        assertEquals(listOf("b1"), author.libraryItems.orEmpty().map { it.id })
    }

    @Test
    fun `a failure loading an author is returned, not thrown`() = runBlocking {
        val api = FakeBrowseApi(author = { throw IOException("offline") })

        assertTrue(BrowseRepository(api).author("a1").isFailure)
    }

    @Test
    fun `recent episodes are unwrapped from the episodes envelope`() = runBlocking {
        val api = FakeBrowseApi(recentEpisodes = {
            RecentEpisodesResponse(
                episodes = listOf(
                    RecentPodcastEpisode(
                        id = "e1", libraryItemId = "li1", title = "Episode one",
                        description = null, podcast = null
                    )
                )
            )
        })

        val episodes = BrowseRepository(api).recentEpisodes("lib").getOrThrow()

        assertEquals(listOf("e1"), episodes.map { it.id })
    }

    // Distinct from an empty list: the screen said "no recent episodes found"
    // for both, which is how a broken response went unnoticed.
    @Test
    fun `a failure loading recent episodes is returned, not thrown`() = runBlocking {
        val api = FakeBrowseApi(recentEpisodes = { throw IOException("offline") })

        assertTrue(BrowseRepository(api).recentEpisodes("lib").isFailure)
    }
}
