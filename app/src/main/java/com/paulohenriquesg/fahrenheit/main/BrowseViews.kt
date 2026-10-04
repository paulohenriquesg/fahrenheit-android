package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import android.app.Activity
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.utils.Alphabetical
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.tv.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.tv.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuConfig
import com.paulohenriquesg.fahrenheit.navigation.MenuItem
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.search.SearchActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.SeriesShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.ShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemsFluid
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemsRow
import com.paulohenriquesg.fahrenheit.ui.theme.LayoutManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/*
 * The series, authors, collections and stats views the main screen switches
 * between. Moved out of MainScreen.kt unchanged: it held them alongside the
 * drawer, the menu handling and the library loading in one 875-line file.
 */

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesBrowseView(seriesList: List<com.paulohenriquesg.fahrenheit.api.Series>, isLoading: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = stringResource(R.string.series),
            modifier = Modifier.padding(bottom = Space.gap)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.loading_series),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (seriesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_series_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val seriesKeys = StableKeys.of(seriesList) { s -> s.id }
                items(seriesList.size, key = { seriesKeys[it] }) { index ->
                    val series = seriesList[index]
                    com.paulohenriquesg.fahrenheit.ui.elements.BookGroupCard(
                        name = series.name,
                        bookCount = series.books?.size,
                        books = series.books.orEmpty(),
                        look = com.paulohenriquesg.fahrenheit.ui.elements.BookGroupLook.Series,
                        onClick = {
                            val intent = com.paulohenriquesg.fahrenheit.group.BookGroupActivity.forSeries(context, series)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuthorsBrowseView(libraryId: String?) {
    val context = LocalContext.current
    var authors by remember { mutableStateOf<List<com.paulohenriquesg.fahrenheit.api.Author>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch authors using ApiClient (which has correct auth credentials)
    LaunchedEffect(libraryId) {
        android.util.Log.d("AuthorsBrowseView", "Starting to fetch authors for libraryId: $libraryId")
        if (libraryId != null) {
            ApiClient.getBrowseApi()?.let { api ->
                BrowseRepository(api).authors(libraryId)
                    .onSuccess { authors = it.sortedWith(Alphabetical.byName { a -> a.name }) }
                    .onFailure { android.util.Log.e("AuthorsBrowseView", "Failure: ${it.message}", it) }
            }
            isLoading = false
        } else {
            android.util.Log.e("AuthorsBrowseView", "libraryId is null!")
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = stringResource(R.string.authors),
            modifier = Modifier.padding(bottom = Space.gap)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.loading_authors),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (authors.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_authors_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val authorKeys = StableKeys.of(authors) { a -> a.id }
                items(authors.size, key = { authorKeys[it] }) { index ->
                    val author = authors[index]
                    com.paulohenriquesg.fahrenheit.ui.elements.AuthorCard(
                        author = author,
                        onClick = {
                            val intent = com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity.createIntent(context, author.id)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectionsBrowseView(collectionsList: List<com.paulohenriquesg.fahrenheit.api.Collection>, isLoading: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = stringResource(R.string.collections),
            modifier = Modifier.padding(bottom = Space.gap)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.loading_collections),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (collectionsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_collections_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val collectionKeys = StableKeys.of(collectionsList) { c -> c.id }
                items(collectionsList.size, key = { collectionKeys[it] }) { index ->
                    val collection = collectionsList[index]
                    com.paulohenriquesg.fahrenheit.ui.elements.BookGroupCard(
                        name = collection.name,
                        bookCount = collection.books?.size,
                        books = collection.books.orEmpty(),
                        look = com.paulohenriquesg.fahrenheit.ui.elements.BookGroupLook.Collection,
                        onClick = {
                            val intent = com.paulohenriquesg.fahrenheit.group.BookGroupActivity.forCollection(context, collection)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsBrowseView(stats: com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse?, isLoading: Boolean) {
    // The title is here rather than on the board so that it is there while
    // loading too, and stays put while the board scrolls under it.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = stringResource(R.string.listening_statistics),
            modifier = Modifier.padding(bottom = Space.gap)
        )
        when {
            isLoading -> CenteredNote(stringResource(R.string.loading_stats))
            stats == null -> CenteredNote(stringResource(R.string.no_statistics_available))
            else -> com.paulohenriquesg.fahrenheit.stats.StatsBoard(
                summary = com.paulohenriquesg.fahrenheit.stats.StatsSummary.of(stats)
            )
        }
    }
}

/**
 * One library's items, under its name and how many there are. Before a library
 * is chosen it is still titled, so the section does not open on a blank band.
 */
@Composable
fun LibraryBrowseView(
    name: String?,
    itemLabel: String,
    items: List<LibraryItem>,
    rowLayout: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    query: LibraryQuery = LibraryQuery.Everything,
    loading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = name ?: stringResource(R.string.library),
            modifier = Modifier.padding(bottom = Space.gap)
        ) {
            // Opened from a Home shelf, the list is narrower than the
            // library and says which view it is (#146).
            val view = when (query) {
                LibraryQuery.Everything -> null
                LibraryQuery.RecentlyAdded -> stringResource(R.string.library_recently_added)
                LibraryQuery.InProgress -> stringResource(R.string.library_in_progress)
                LibraryQuery.Finished -> stringResource(R.string.library_finished)
            }
            Text(
                // Loading, the count would be "0" and read as an answer.
                text = listOfNotNull(
                    view,
                    if (loading && items.isEmpty()) stringResource(R.string.loading) else "(${items.size} $itemLabel)"
                ).joinToString("  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (rowLayout) {
            LibraryItemsRow(items, listState)
        } else {
            LibraryItemsFluid(items)
        }
    }
}

@Composable
private fun CenteredNote(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
