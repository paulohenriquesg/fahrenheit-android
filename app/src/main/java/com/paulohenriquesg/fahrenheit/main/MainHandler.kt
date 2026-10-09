package com.paulohenriquesg.fahrenheit.main

import android.content.Context
import android.widget.Toast
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.FavouritesChoice
import com.paulohenriquesg.fahrenheit.favourites.FavouritesShelf

/**
 * Home screen data, as suspend calls.
 *
 * Previously Retrofit callbacks fired from inside LaunchedEffect: enqueue is not
 * cancelled when the composable leaves composition, so a slow response could
 * still write to state for a screen the user had already left. A suspend call in
 * the same place is cancelled with the effect.
 *
 * The decisions live in LibraryRepository, which is tested; this only reports
 * failures to the user.
 */
class MainHandler(private val context: Context) {

    private fun repository(): LibraryRepository? =
        ApiClient.getLibraryApi()?.let { LibraryRepository(it) }

    suspend fun fetchLibraryItems(libraryId: String, query: LibraryQuery): List<LibraryItem> =
        fetch("Failed to load library items") { it.items(libraryId, query) }

    suspend fun fetchPersonalizedView(libraryId: String): List<Shelf> =
        FavouritesShelf.onto(
            fetch("Failed to load personalized view") { it.personalizedShelves(libraryId) },
            libraryId,
            favourites()
        )

    /** Favourites (#180), while signed in. */
    fun favourites(): Favourites? = ApiClient.getPlaylistApi()?.let { Favourites(it, FavouritesChoice(context)) }

    /**
     * Everything the user has started, for the covers on Home (#104). Throws
     * when unreadable; the covers then go without, so there is no toast.
     */
    suspend fun fetchProgress(): List<MediaProgressResponse> =
        ApiClient.getPodcastApi()?.me()?.mediaProgress.orEmpty()

    private suspend fun <T> fetch(
        failureMessage: String,
        block: suspend (LibraryRepository) -> Result<List<T>>
    ): List<T> {
        val repository = repository() ?: return emptyList()
        return block(repository).getOrElse { error ->
            Toast.makeText(
                context,
                "$failureMessage: ${error.message ?: "network error"}",
                Toast.LENGTH_SHORT
            ).show()
            emptyList()
        }
    }
}
