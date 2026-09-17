package com.paulohenriquesg.fahrenheit.main

import android.content.Context
import android.widget.Toast
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.Shelf

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

    suspend fun fetchLibraryItems(libraryId: String): List<LibraryItem> =
        fetch("Failed to load library items") { it.items(libraryId) }

    suspend fun fetchPersonalizedView(libraryId: String): List<Shelf> =
        fetch("Failed to load personalized view") { it.personalizedShelves(libraryId) }

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
