package com.paulohenriquesg.fahrenheit.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.SearchResults
import com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorCard
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemCard
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val libraryId = (context as? SearchActivity)?.libraryId
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var searchResults by remember { mutableStateOf(SearchResults.None) }
    var library by remember { mutableStateOf<Library?>(null) }
    var searchFailed by remember { mutableStateOf(false) }

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
            searchResults = SearchResults.None
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
            .onFailure { searchResults = SearchResults.None; searchFailed = true }
    }

    SearchContent(
        query = query,
        onQueryChange = { query = it },
        results = searchResults,
        failed = searchFailed,
        mediaType = library?.mediaType,
        onItemClick = { context.startActivity(DetailActivity.createIntent(context, it.id)) },
        onAuthorClick = { context.startActivity(AuthorDetailActivity.createIntent(context, it.id)) }
    )
}

object SearchTags {
    const val FIELD = "search_field"
}

/**
 * Frame 5 of docs/mocks/screens.html (#106). The field holds focus on arrival,
 * since typing is the only thing to do here; the answer comes grouped by kind,
 * each group a single row headed with its count, so it fits above the keyboard.
 */
@Composable
fun SearchContent(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    results: SearchResults,
    failed: Boolean,
    mediaType: String?,
    onItemClick: (LibraryItem) -> Unit,
    onAuthorClick: (Author) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val fieldFocus = rememberInitialFocus(enabled = true)
    val itemKind = stringResource(if (mediaType == "podcast") R.string.podcasts else R.string.books)
    val authorKind = stringResource(R.string.authors)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Space.gap)
                .focusRequester(fieldFocus)
                .testTag(SearchTags.FIELD),
            placeholder = { Text(stringResource(R.string.search_2)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        when {
            failed -> Text(
                text = stringResource(R.string.search_failed),
                color = MaterialTheme.colorScheme.error
            )

            query.text.isNotBlank() && results.items.isEmpty() && results.authors.isEmpty() -> Text(
                text = stringResource(R.string.search_nothing_found, query.text),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                if (results.items.isNotEmpty()) {
                    item(key = "items") {
                        ResultGroup(itemKind, results.items.size) {
                            val keys = StableKeys.of(results.items) { it.id }
                            items(results.items.size, key = { keys[it] }) { index ->
                                LibraryItemCard(results.items[index], onClick = onItemClick)
                            }
                        }
                    }
                }
                if (results.authors.isNotEmpty()) {
                    item(key = "authors") {
                        ResultGroup(authorKind, results.authors.size) {
                            items(results.authors, key = { "author-${it.id}" }) { author ->
                                AuthorCard(author) { onAuthorClick(author) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One kind of match: "Books · 3", then the matches in a single row. */
@Composable
private fun ResultGroup(kind: String, count: Int, row: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.search_group, kind, count),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space.gap),
            contentPadding = PaddingValues(vertical = Space.inset),
            content = row
        )
    }
}

private const val SEARCH_DEBOUNCE_MS = 300L
