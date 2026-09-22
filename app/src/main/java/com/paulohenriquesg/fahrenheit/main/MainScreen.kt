package com.paulohenriquesg.fahrenheit.main

import android.app.Activity
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
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.GlobalMediaPlayer
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.library.LibrarySelectionActivity
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.ui.theme.LayoutManager
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuConfig
import com.paulohenriquesg.fahrenheit.navigation.MenuItem
import com.paulohenriquesg.fahrenheit.podcast.PlayerActivity
import com.paulohenriquesg.fahrenheit.search.SearchActivity
import com.paulohenriquesg.fahrenheit.settings.SettingsActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.SeriesShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.ShelfRow
import com.paulohenriquesg.fahrenheit.ui.navigation.LibraryItemsFluid
import com.paulohenriquesg.fahrenheit.ui.navigation.LibraryItemsRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(
    fetchLibraryItems: suspend (String) -> List<LibraryItem>,
    fetchPersonalizedView: suspend (String) -> List<Shelf>
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPreferencesHandler = remember { SharedPreferencesHandler(context) }
    val userPreferences = sharedPreferencesHandler.getUserPreferences()
    val username = userPreferences.username
    var libraries by remember { mutableStateOf(listOf<Library>()) }
    var libraryItems by remember { mutableStateOf(listOf<LibraryItem>()) }
    var shelves by remember { mutableStateOf(listOf<Shelf>()) }
    var currentLibrary by remember { mutableStateOf<Library?>(null) }
    val listState = rememberLazyListState()
    val isRowLayout by LayoutManager.isRowLayout  // Use LayoutManager instead of local state

    val menuItemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }  // Map menu item ID to FocusRequester
    var view by remember { mutableStateOf(MainView.HOME) }
    // Which menu row the drawer highlights and focuses when reopened. Kept
    // apart from `view` because rows like Settings open another screen and
    // still take the highlight; changing that is a focus change for a TV, not
    // a refactor.
    var highlightedMenuItemId by remember { mutableStateOf(MainView.HOME.menuItemId) }
    var shouldRefreshLibrary by remember { mutableStateOf(false) }
    var seriesList by remember { mutableStateOf(listOf<com.paulohenriquesg.fahrenheit.api.Series>()) }
    var collectionsList by remember { mutableStateOf(listOf<com.paulohenriquesg.fahrenheit.api.Collection>()) }
    var isLoadingSeries by remember { mutableStateOf(false) }
    var isLoadingCollections by remember { mutableStateOf(false) }
    var isLoadingStats by remember { mutableStateOf(false) }
    var listeningStats by remember { mutableStateOf<com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse?>(null) }

    val apiClient = ApiClient.getApiService()
    if (apiClient == null) {
        // Show error and redirect to login
        LaunchedEffect(Unit) {
            Toast.makeText(context, context.getString(R.string.host_not_configured), Toast.LENGTH_LONG)
                .show()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
            (context as ComponentActivity).finish()
        }
        return
    }

    // Detect when returning from LibrarySelectionActivity
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check if library has changed
                val savedLibraryId = sharedPreferencesHandler.getSelectedLibraryId()
                if (savedLibraryId != null && savedLibraryId != currentLibrary?.id) {
                    shouldRefreshLibrary = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle library refresh when needed
    LaunchedEffect(shouldRefreshLibrary, libraries) {
        if (shouldRefreshLibrary && libraries.isNotEmpty()) {
            val savedLibraryId = sharedPreferencesHandler.getSelectedLibraryId()
            val newLibrary = libraries.find { it.id == savedLibraryId }
            if (newLibrary != null && newLibrary.id != currentLibrary?.id) {
                currentLibrary = newLibrary
                view = MainView.HOME
                highlightedMenuItemId = MainView.HOME.menuItemId

                // Fetch data for new library
                newLibrary.id?.let { libraryId ->
                    shelves = fetchPersonalizedView(libraryId)
                    libraryItems = fetchLibraryItems(libraryId)
                }
            }
            shouldRefreshLibrary = false
        }
    }

    // Fetch libraries from the API
    LaunchedEffect(Unit) {
        val libraryApi = ApiClient.getLibraryApi()
        if (libraryApi != null) {
            LibraryRepository(libraryApi).libraries()
                .onSuccess { fetched ->
                    libraries = fetched
                    if (libraries.isNotEmpty()) {
                        val savedLibraryId = sharedPreferencesHandler.getSelectedLibraryId()
                        currentLibrary = LibraryChoice.pick(libraries, savedLibraryId)
                        currentLibrary?.id?.let { sharedPreferencesHandler.saveSelectedLibraryId(it) }
                        currentLibrary?.id?.let { libraryId ->
                            shelves = fetchPersonalizedView(libraryId)
                            libraryItems = fetchLibraryItems(libraryId)
                        }
                    }
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        "Failed to load libraries: ${it.message ?: "network error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // Function to scroll to the first item
    fun scrollToFirstItem() {
        scope.launch {
            listState.scrollToItem(0)
        }
    }

    // Handle back button press
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Get menu items for current library type
    val menuItems = remember(currentLibrary?.mediaType) {
        MenuConfig.getMenuForLibraryType(currentLibrary?.mediaType)
    }

    // Handle menu actions
    fun handleMenuAction(action: MenuAction, libraryId: String?) {
        when (action) {
            MenuAction.HOME -> {
                view = MainView.HOME
                libraryId?.let { id ->
                    scope.launch { shelves = fetchPersonalizedView(id) }
                }
            }
            MenuAction.LIBRARY -> {
                view = MainView.LIBRARY
                libraryId?.let { id ->
                    scope.launch { libraryItems = fetchLibraryItems(id) }
                }
            }
            MenuAction.SERIES -> {
                view = MainView.SERIES
                seriesList = emptyList()  // Clear old data
                isLoadingSeries = true
                if (libraryId != null) {
                    scope.launch {
                        ApiClient.getBrowseApi()?.let { api ->
                            BrowseRepository(api).series(libraryId)
                                .onSuccess { seriesList = it.sortedWith(Alphabetical.byName { s -> s.name }) }
                        }
                        isLoadingSeries = false
                    }
                } else {
                    isLoadingSeries = false
                }
            }
            MenuAction.COLLECTIONS -> {
                view = MainView.COLLECTIONS
                collectionsList = emptyList()  // Clear old data
                isLoadingCollections = true
                if (libraryId != null) {
                    scope.launch {
                        ApiClient.getBrowseApi()?.let { api ->
                            BrowseRepository(api).collections(libraryId)
                                .onSuccess { collectionsList = it.sortedWith(Alphabetical.byName { c -> c.name }) }
                        }
                        isLoadingCollections = false
                    }
                } else {
                    isLoadingCollections = false
                }
            }
            MenuAction.AUTHORS -> {
                view = MainView.AUTHORS
            }
            MenuAction.NARRATORS -> {
                Toast.makeText(context, context.getString(R.string.narrators_view_coming_soon), Toast.LENGTH_SHORT).show()
            }
            MenuAction.STATS -> {
                view = MainView.STATS
                listeningStats = null  // Clear old data
                isLoadingStats = true
                scope.launch {
                    ApiClient.getBrowseApi()?.let { api ->
                        BrowseRepository(api).listeningStats().onSuccess { listeningStats = it }
                    }
                    isLoadingStats = false
                }
            }
            MenuAction.LATEST -> {
                libraryId?.let { id ->
                    val intent = com.paulohenriquesg.fahrenheit.podcast.LatestEpisodesActivity.createIntent(context, id)
                    context.startActivity(intent)
                }
            }
            MenuAction.SELECT_LIBRARY -> {
                val intent = LibrarySelectionActivity.createIntent(context)
                context.startActivity(intent)
            }
            MenuAction.SETTINGS -> {
                val intent = SettingsActivity.createIntent(context)
                context.startActivity(intent)
            }
            MenuAction.LOGOUT -> {
                // Storage, the client built from it, and anything still
                // playing with the old token.
                sharedPreferencesHandler.clearPreferences()
                ApiClient.clearSession()
                GlobalMediaPlayer.release()
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }
        }
    }

    // Restore focus to current section when drawer opens
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            delay(100)  // Wait for drawer animation
            // Request focus on the current section's menu item
            menuItemFocusRequesters[highlightedMenuItemId]?.requestFocus()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LazyColumn(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(40.dp)) }

                // Library-specific menu items
                items(menuItems, key = { it.id }) { menuItem ->
                    // Create or retrieve FocusRequester for this menu item
                    val focusRequester = menuItemFocusRequesters.getOrPut(menuItem.id) {
                        FocusRequester()
                    }

                    MenuItemRow(
                        menuItem = menuItem,
                        isFocused = highlightedMenuItemId == menuItem.id,
                        focusRequester = focusRequester,
                        onClick = {
                            highlightedMenuItemId = menuItem.id
                            handleMenuAction(menuItem.action, currentLibrary?.id)
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { HorizontalDivider() }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Common items (Switch Library, Settings, Logout)
                items(MenuConfig.commonItems, key = { it.id }) { menuItem ->
                    // Create or retrieve FocusRequester for this menu item
                    val focusRequester = menuItemFocusRequesters.getOrPut(menuItem.id) {
                        FocusRequester()
                    }

                    MenuItemRow(
                        menuItem = menuItem,
                        isFocused = highlightedMenuItemId == menuItem.id,
                        focusRequester = focusRequester,
                        onClick = {
                            highlightedMenuItemId = menuItem.id
                            handleMenuAction(menuItem.action, currentLibrary?.id)
                            if (menuItem.action != MenuAction.SELECT_LIBRARY) {
                                scope.launch { drawerState.close() }
                            }
                        }
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_screen")
                    .onKeyEvent { keyEvent ->
                        when {
                            // Open drawer with Menu button
                            keyEvent.key == Key.Menu && !drawerState.isOpen -> {
                                scope.launch { drawerState.open() }
                                true
                            }
                            // Close drawer on D-Pad right or Menu button when drawer is open
                            (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.Menu) &&
                            drawerState.isOpen -> {
                                scope.launch { drawerState.close() }
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                IconButton(
                    onClick = { if (!listState.isScrollInProgress) scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, SearchActivity::class.java).apply {
                                putExtra("libraryId", currentLibrary?.id)
                            }
                            context.startActivity(intent)
                        },
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                    Greeting(
                        name = username,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                when (view) {
                    MainView.HOME -> PersonalizedHomeView(shelves, currentLibrary?.id)
                    MainView.LIBRARY -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 30.dp)
                        ) {
                            val itemCount = libraryItems.size
                            val itemLabel =
                                if (libraries.find { it.name == currentLibrary?.name }?.mediaType == "book") "books" else "podcasts"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                currentLibrary?.name?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "($itemCount $itemLabel)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isRowLayout) {
                                LibraryItemsRow(libraryItems, listState)
                            } else {
                                LibraryItemsFluid(libraryItems)
                            }
                        }
                    }
                    MainView.SERIES -> SeriesBrowseView(seriesList, isLoadingSeries)
                    MainView.AUTHORS -> AuthorsBrowseView(currentLibrary?.id)
                    MainView.COLLECTIONS -> CollectionsBrowseView(collectionsList, isLoadingCollections)
                    MainView.STATS -> StatsBrowseView(listeningStats, isLoadingStats)
                }
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello, $name!", modifier = modifier)
}

fun getIconForMediaType(mediaType: String?): ImageVector {
    return when (mediaType) {
        "book" -> Icons.Filled.Book
        "podcast" -> Icons.Filled.Podcasts
        else -> Icons.Filled.Book // default icon
    }
}

@Composable
fun PersonalizedHomeView(shelves: List<Shelf>, libraryId: String?) {
    val context = LocalContext.current
    // Filter out empty shelves
    val nonEmptyShelves = shelves.filter {
        (it.bookEntities != null && it.bookEntities.isNotEmpty()) ||
        (it.authorEntities != null && it.authorEntities.isNotEmpty()) ||
        (it.seriesEntities != null && it.seriesEntities.isNotEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.home),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        androidx.compose.foundation.lazy.LazyColumn {
            items(nonEmptyShelves, key = { it.id }) { shelf ->
                when (shelf.type) {
                    "episode" -> {
                        shelf.bookEntities?.let { books ->
                            ShelfRow(shelf = shelf) { item ->
                                val episodeId = item.recentEpisode?.id
                                val podcastId = item.recentEpisode?.libraryItemId ?: item.id

                                android.util.Log.d("MainScreen", "Episode clicked - episodeId: $episodeId, podcastId: $podcastId")
                                android.util.Log.d("MainScreen", "Item data - id: ${item.id}, mediaType: ${item.mediaType}")
                                android.util.Log.d("MainScreen", "RecentEpisode data - ${item.recentEpisode}")

                                if (episodeId != null) {
                                    val intent = PlayerActivity.createIntent(context, podcastId, episodeId, autoPlay = true)
                                    context.startActivity(intent)
                                } else {
                                    val intent = com.paulohenriquesg.fahrenheit.detail.DetailActivity.createIntent(context, item.id)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                    "book", "podcast" -> {
                        shelf.bookEntities?.let { books ->
                            ShelfRow(shelf = shelf) { item ->
                                val intent = com.paulohenriquesg.fahrenheit.detail.DetailActivity.createIntent(context, item.id)
                                context.startActivity(intent)
                            }
                        }
                    }
                    "authors" -> {
                        shelf.authorEntities?.let { authors ->
                            AuthorShelfRow(shelf = shelf, authors = authors) { author ->
                                val intent = com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity.createIntent(context, author.id)
                                context.startActivity(intent)
                            }
                        }
                    }
                    "series" -> {
                        shelf.seriesEntities?.let { series ->
                            SeriesShelfRow(shelf = shelf, series = series) { seriesItem ->
                                val intent = com.paulohenriquesg.fahrenheit.series.SeriesDetailActivity.createIntent(context, seriesItem)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MenuItemRow(
    menuItem: MenuItem,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusRequester(focusRequester),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer  // Focus = selection
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = menuItem.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = menuItem.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}



