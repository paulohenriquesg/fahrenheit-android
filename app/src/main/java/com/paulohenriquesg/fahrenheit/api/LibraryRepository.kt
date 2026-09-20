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

    suspend fun items(libraryId: String): Result<List<LibraryItem>> = runCatching {
        api.getLibraryItems(libraryId).results
    }

    suspend fun personalizedShelves(libraryId: String): Result<List<Shelf>> = runCatching {
        api.getPersonalizedView(libraryId)
    }
}
