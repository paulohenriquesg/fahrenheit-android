package com.paulohenriquesg.fahrenheit.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.ui.navigation.LibraryItemsRow
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val libraryId = (context as? SearchActivity)?.libraryId
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var searchResults by remember { mutableStateOf(listOf<LibraryItem>()) }
    var library by remember { mutableStateOf<Library?>(null) }
    var searchFailed by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = remember { LazyListState() }

    LaunchedEffect(libraryId) {
        val api = ApiClient.getLibraryApi()
        if (libraryId != null && api != null) {
            LibraryRepository(api).library(libraryId).onSuccess { library = it }
        }
    }

    // Runs inside the effect, so a slow search is cancelled when the query
    // changes or the screen is left, instead of writing results in late.
    LaunchedEffect(query.text, library, libraryId) {
        val api = ApiClient.getBrowseApi()
        if (query.text.isBlank() || libraryId == null || api == null) {
            searchResults = emptyList()
            searchFailed = false
            return@LaunchedEffect
        }
        // Each keystroke restarts this effect, so the wait collapses a burst of
        // typing into one request instead of one per character.
        delay(SEARCH_DEBOUNCE_MS)
        BrowseRepository(api)
            .search(libraryId, query.text, library?.mediaType ?: "book")
            .onSuccess { searchResults = it; searchFailed = false }
            // Distinct from "nothing matched": the old code reported both by
            // leaving whatever the last search had found on screen.
            .onFailure { searchResults = emptyList(); searchFailed = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text("Search...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    keyboardController?.hide()
                    // Handle the search action here if needed
                }
            )
        )

        library?.let {
            Text(
                text = it.name ?: "No name",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Add more UI elements to display library details
        } ?: Text(text = "Loading...", color = MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.height(16.dp))
        when {
            searchFailed -> Text(
                text = "Search failed. Check the connection to your server.",
                color = MaterialTheme.colorScheme.error
            )

            query.text.isNotBlank() && searchResults.isEmpty() -> Text(
                text = "Nothing found for \"${query.text}\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LibraryItemsRow(libraryItems = searchResults, listState = listState)
        }
    }
}

private const val SEARCH_DEBOUNCE_MS = 300L
