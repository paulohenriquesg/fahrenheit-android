package com.paulohenriquesg.fahrenheit.api

/**
 * Library reads, with failures returned rather than thrown.
 *
 * Replaces Retrofit callbacks fired from inside LaunchedEffect. `enqueue` is not
 * cancelled when a composable leaves composition, so a slow response could still
 * write to state belonging to a screen the user had already left; a suspend call
 * in the same place is cancelled with the effect.
 *
 * Returning Result also keeps "no items" and "the request failed" distinct - the
 * callback code reported both to the UI as an empty list.
 */
class LibraryRepository(private val api: LibraryApi) {

    suspend fun libraries(): Result<List<Library>> = runCatching {
        api.getLibraries().libraries.orEmpty().sortedBy { it.displayOrder }
    }

    suspend fun library(libraryId: String): Result<Library> = runCatching {
        api.getLibrary(libraryId)
    }

    suspend fun stats(libraryId: String): Result<LibraryStats> = runCatching {
        api.getLibraryStats(libraryId)
    }

    suspend fun item(itemId: String): Result<LibraryItemResponse> = runCatching {
        api.getLibraryItem(itemId)
    }

    suspend fun items(libraryId: String, query: LibraryQuery = LibraryQuery.Everything): Result<List<LibraryItem>> = runCatching {
        api.getLibraryItems(
            libraryId,
            sort = query.sort,
            desc = if (query.newestFirst) 1 else null,
            filter = query.filter
        ).results
    }

    /**
     * A series' books that can be played, in series order (#107). Minified:
     * only ids, titles and track counts are wanted. An ebook-only book is left
     * out - choosing it would stop the book playing for an error screen.
     */
    suspend fun seriesBooks(libraryId: String, seriesId: String): Result<List<LibraryItem>> = runCatching {
        api.getLibraryItems(libraryId, sort = "sequence", minified = 1, filter = seriesFilter(seriesId))
            .results.filter { it.media.numTracks > 0 }
    }

    suspend fun personalizedShelves(libraryId: String): Result<List<Shelf>> = runCatching {
        api.getPersonalizedView(libraryId)
    }
}

/** The items filter for one series. */
internal fun seriesFilter(seriesId: String): String = itemsFilter("series", seriesId)

/** An items filter, "group.value": the value base64-encoded, then URL-encoded, as the web client sends it. */
internal fun itemsFilter(group: String, value: String): String =
    "$group." + java.net.URLEncoder.encode(android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.NO_WRAP), "UTF-8")
