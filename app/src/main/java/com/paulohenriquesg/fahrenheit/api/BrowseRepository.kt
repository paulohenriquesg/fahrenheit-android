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
}
