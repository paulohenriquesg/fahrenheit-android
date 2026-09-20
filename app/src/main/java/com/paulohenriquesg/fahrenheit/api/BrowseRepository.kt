package com.paulohenriquesg.fahrenheit.api

/**
 * Library browsing, with failures returned rather than thrown.
 *
 * These endpoints do not agree on an envelope: series and collections come back
 * under "results", authors under "authors". Unwrapping here means a screen
 * cannot read the wrong one - which is what broke the Authors list before.
 */
class BrowseRepository(private val api: BrowseApi) {

    suspend fun series(libraryId: String): Result<List<Series>> = runCatching {
        api.getLibrarySeries(libraryId).results
    }

    suspend fun collections(libraryId: String): Result<List<Collection>> = runCatching {
        api.getLibraryCollections(libraryId).results
    }

    suspend fun authors(libraryId: String): Result<List<Author>> = runCatching {
        api.getLibraryAuthors(libraryId).authors
    }

    suspend fun listeningStats(): Result<ListeningStatsResponse> = runCatching {
        api.getListeningStats()
    }

    /**
     * Library items matching [query]. Results are grouped by media type and a
     * group is absent when it has no matches, which must still reach the screen
     * as an empty list: otherwise it keeps showing the previous search.
     */
    suspend fun search(libraryId: String, query: String, mediaType: String): Result<List<LibraryItem>> =
        runCatching {
            val response = api.searchLibraryItems(libraryId, query)
            val matches = if (mediaType == "podcast") response.podcast else response.book
            matches.orEmpty().mapNotNull { it.libraryItem }
        }
}
