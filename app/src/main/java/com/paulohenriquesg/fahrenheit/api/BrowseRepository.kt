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
     * Library items and authors matching [query]. Results are grouped by media
     * type and a group is absent when it has no matches, which must still reach
     * the screen as an empty list: otherwise it keeps showing the previous search.
     *
     * The server takes a limit per kind and no offset (#193), so a kind that
     * comes back at the limit is marked cut rather than paged. Items are judged
     * on the matches sent, before any without an item are dropped.
     */
    suspend fun search(libraryId: String, query: String, mediaType: String): Result<SearchResults> =
        runCatching {
            val response = api.searchLibraryItems(libraryId, query, SEARCH_LIMIT)
            val matches = (if (mediaType == "podcast") response.podcast else response.book).orEmpty()
            val authors = response.authors.orEmpty()
            SearchResults(
                items = matches.mapNotNull { it.libraryItem },
                authors = authors,
                itemsCut = matches.size >= SEARCH_LIMIT,
                authorsCut = authors.size >= SEARCH_LIMIT
            )
        }

    suspend fun author(authorId: String): Result<AuthorDetailResponse> = runCatching {
        api.getAuthor(authorId)
    }

    suspend fun recentEpisodes(libraryId: String): Result<List<RecentPodcastEpisode>> = runCatching {
        api.getRecentEpisodes(libraryId).episodes
    }

    companion object {
        /** Matches asked for of each kind; the server's own default is 12. */
        const val SEARCH_LIMIT = 50
    }
}
