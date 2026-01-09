package com.paulohenriquesg.fahrenheit.main

import android.app.Activity
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
import com.paulohenriquesg.fahrenheit.api.ApiClient
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
    fetchLibraryItems: (String, (List<LibraryItem>) -> Unit) -> Unit,
    fetchPersonalizedView: (String, (List<Shelf>) -> Unit) -> Unit
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
    var currentSection by remember { mutableStateOf("home") }  // Tracks active section
    val menuItemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }  // Map menu item ID to FocusRequester
    var viewMode by remember { mutableStateOf("home") }
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
            Toast.makeText(context, "Host is not configured. Please log in.", Toast.LENGTH_LONG)
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
                viewMode = "home"
                currentSection = "home"  // Reset to home section when library changes

                // Fetch data for new library
                newLibrary.id?.let { libraryId ->
                    fetchPersonalizedView(libraryId) { personalizedShelves ->
                        shelves = personalizedShelves
                    }
                    fetchLibraryItems(libraryId) { items ->
                        libraryItems = items
                    }
                }
            }
            shouldRefreshLibrary = false
        }
    }

    // Fetch libraries from the API
    LaunchedEffect(Unit) {
        apiClient.getLibraries().enqueue(object : Callback<LibrariesResponse> {
            override fun onResponse(
                call: Call<LibrariesResponse>,
                response: Response<LibrariesResponse>
            ) {
                if (response.isSuccessful) {
                    libraries =
                        response.body()?.libraries?.sortedBy { it.displayOrder } ?: emptyList()

                    if (libraries.isNotEmpty()) {
                        // Load saved library preference
                        val savedLibraryId = sharedPreferencesHandler.getSelectedLibraryId()
                        currentLibrary = if (savedLibraryId != null) {
                            libraries.find { it.id == savedLibraryId } ?: libraries[0]
                        } else {
                            libraries[0]
                        }

                        // Save selection if using default
                        currentLibrary?.id?.let { id ->
                            sharedPreferencesHandler.saveSelectedLibraryId(id)
                        }

                        // Fetch data for selected library
                        currentLibrary?.id?.let { libraryId ->
                            fetchPersonalizedView(libraryId) { personalizedShelves ->
                                shelves = personalizedShelves
                            }
                            fetchLibraryItems(libraryId) { items ->
                                libraryItems = items
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LibrariesResponse>, t: Throwable) {
                // Handle error
            }
        })
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
                viewMode = "home"
                libraryId?.let { id ->
                    fetchPersonalizedView(id) { personalizedView ->
                        shelves = personalizedView
                    }
                }
            }
            MenuAction.LIBRARY -> {
                viewMode = "library"
                libraryId?.let { id ->
                    fetchLibraryItems(id) { items ->
                        libraryItems = items
                    }
                }
            }
            MenuAction.SERIES -> {
                viewMode = "series"
                seriesList = emptyList()  // Clear old data
                isLoadingSeries = true
                if (libraryId != null) {
                    apiClient?.getLibrarySeries(libraryId)?.enqueue(object : Callback<com.paulohenriquesg.fahrenheit.api.SeriesResponse> {
                        override fun onResponse(
                            call: Call<com.paulohenriquesg.fahrenheit.api.SeriesResponse>,
                            response: Response<com.paulohenriquesg.fahrenheit.api.SeriesResponse>
                        ) {
                            if (response.isSuccessful) {
                                seriesList = response.body()?.results?.sortedBy { it.name } ?: emptyList()
                            }
                            isLoadingSeries = false
                        }
                        override fun onFailure(call: Call<com.paulohenriquesg.fahrenheit.api.SeriesResponse>, t: Throwable) {
                            isLoadingSeries = false
                        }
                    })
                } else {
                    isLoadingSeries = false
                }
            }
            MenuAction.COLLECTIONS -> {
                viewMode = "collections"
                collectionsList = emptyList()  // Clear old data
                isLoadingCollections = true
                if (libraryId != null) {
                    apiClient?.getLibraryCollections(libraryId)?.enqueue(object : Callback<com.paulohenriquesg.fahrenheit.api.CollectionsResponse> {
                        override fun onResponse(
                            call: Call<com.paulohenriquesg.fahrenheit.api.CollectionsResponse>,
                            response: Response<com.paulohenriquesg.fahrenheit.api.CollectionsResponse>
                        ) {
                            if (response.isSuccessful) {
                                collectionsList = response.body()?.results?.sortedBy { it.name } ?: emptyList()
                            }
                            isLoadingCollections = false
                        }
                        override fun onFailure(call: Call<com.paulohenriquesg.fahrenheit.api.CollectionsResponse>, t: Throwable) {
                            isLoadingCollections = false
                        }
                    })
                } else {
                    isLoadingCollections = false
                }
            }
            MenuAction.AUTHORS -> {
                viewMode = "authors"
            }
            MenuAction.NARRATORS -> {
                Toast.makeText(context, "Narrators view - Coming soon", Toast.LENGTH_SHORT).show()
            }
            MenuAction.STATS -> {
                viewMode = "stats"
                listeningStats = null  // Clear old data
                isLoadingStats = true
                apiClient?.getListeningStats()?.enqueue(object : Callback<com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse> {
                    override fun onResponse(
                        call: Call<com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse>,
                        response: Response<com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse>
                    ) {
                        if (response.isSuccessful) {
                            listeningStats = response.body()
                        }
                        isLoadingStats = false
                    }
                    override fun onFailure(call: Call<com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse>, t: Throwable) {
                        isLoadingStats = false
                    }
                })
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
                sharedPreferencesHandler.clearPreferences()
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
            menuItemFocusRequesters[currentSection]?.requestFocus()
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
                items(menuItems) { menuItem ->
                    // Create or retrieve FocusRequester for this menu item
                    val focusRequester = menuItemFocusRequesters.getOrPut(menuItem.id) {
                        FocusRequester()
                    }

                    MenuItemRow(
                        menuItem = menuItem,
                        isFocused = currentSection == menuItem.id,
                        focusRequester = focusRequester,
                        onClick = {
                            currentSection = menuItem.id  // Track active section
                            handleMenuAction(menuItem.action, currentLibrary?.id)
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { HorizontalDivider() }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Common items (Switch Library, Settings, Logout)
                items(MenuConfig.commonItems) { menuItem ->
                    // Create or retrieve FocusRequester for this menu item
                    val focusRequester = menuItemFocusRequesters.getOrPut(menuItem.id) {
                        FocusRequester()
                    }

                    MenuItemRow(
                        menuItem = menuItem,
                        isFocused = currentSection == menuItem.id,
                        focusRequester = focusRequester,
                        onClick = {
                            currentSection = menuItem.id  // Track active section
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
                    Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
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
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    Greeting(
                        name = username,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                when (viewMode) {
                    "home" -> PersonalizedHomeView(shelves, currentLibrary?.id)
                    "library" -> {
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
                    "series" -> SeriesBrowseView(seriesList, isLoadingSeries)
                    "authors" -> AuthorsBrowseView(currentLibrary?.id)
                    "collections" -> CollectionsBrowseView(collectionsList, isLoadingCollections)
                    "stats" -> StatsBrowseView(listeningStats, isLoadingStats)
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
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        androidx.compose.foundation.lazy.LazyColumn {
            items(nonEmptyShelves) { shelf ->
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesBrowseView(seriesList: List<com.paulohenriquesg.fahrenheit.api.Series>, isLoading: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 48.dp, end = 48.dp, bottom = 16.dp)
    ) {
        Text(
            text = "Series",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading series...",
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
                    text = "No series found",
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
                items(seriesList.size) { index ->
                    val series = seriesList[index]
                    com.paulohenriquesg.fahrenheit.series.SeriesCard(
                        series = series,
                        onClick = {
                            val intent = com.paulohenriquesg.fahrenheit.series.SeriesDetailActivity.createIntent(context, series)
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
            val apiClient = ApiClient.getApiService()
            apiClient?.getLibraryAuthors(libraryId)?.enqueue(object : Callback<com.paulohenriquesg.fahrenheit.api.AuthorsResponse> {
                override fun onResponse(
                    call: Call<com.paulohenriquesg.fahrenheit.api.AuthorsResponse>,
                    response: Response<com.paulohenriquesg.fahrenheit.api.AuthorsResponse>
                ) {
                    android.util.Log.d("AuthorsBrowseView", "Got response! Code: ${response.code()}")
                    if (response.isSuccessful) {
                        val body = response.body()
                        android.util.Log.d("AuthorsBrowseView", "Response body authors count: ${body?.authors?.size}")
                        authors = body?.authors?.sortedBy { it.name } ?: emptyList()
                        android.util.Log.d("AuthorsBrowseView", "Set ${authors.size} authors")
                    } else {
                        android.util.Log.e("AuthorsBrowseView", "Response not successful: ${response.code()}")
                    }
                    isLoading = false
                }

                override fun onFailure(call: Call<com.paulohenriquesg.fahrenheit.api.AuthorsResponse>, t: Throwable) {
                    android.util.Log.e("AuthorsBrowseView", "Request failed", t)
                    isLoading = false
                }
            })
        } else {
            android.util.Log.e("AuthorsBrowseView", "libraryId is null!")
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 48.dp, end = 48.dp, bottom = 16.dp)
    ) {
        Text(
            text = "Authors",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading authors...",
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
                    text = "No authors found",
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
                items(authors.size) { index ->
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
            .padding(top = 60.dp, start = 48.dp, end = 48.dp, bottom = 16.dp)
    ) {
        Text(
            text = "Collections",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading collections...",
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
                    text = "No collections found",
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
                items(collectionsList.size) { index ->
                    val collection = collectionsList[index]
                    com.paulohenriquesg.fahrenheit.collection.CollectionCard(
                        collection = collection,
                        onClick = {
                            val intent = com.paulohenriquesg.fahrenheit.collection.CollectionDetailActivity.createIntent(context, collection)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 48.dp, end = 48.dp, bottom = 16.dp)
    ) {
        Text(
            text = "Listening Statistics",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading stats...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (stats == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No statistics available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                com.paulohenriquesg.fahrenheit.stats.StatCard(
                    title = "Total Listening Time",
                    value = com.paulohenriquesg.fahrenheit.stats.formatTime(stats.totalTime)
                )

                stats.items.size.let { itemCount ->
                    com.paulohenriquesg.fahrenheit.stats.StatCard(
                        title = "Items Listened To",
                        value = "$itemCount ${if (itemCount == 1) "item" else "items"}"
                    )
                }

                stats.days.size.let { dayCount ->
                    com.paulohenriquesg.fahrenheit.stats.StatCard(
                        title = "Days with Activity",
                        value = "$dayCount ${if (dayCount == 1) "day" else "days"}"
                    )
                }

                if (stats.days.isNotEmpty()) {
                    val avgPerDay = stats.totalTime / stats.days.size
                    com.paulohenriquesg.fahrenheit.stats.StatCard(
                        title = "Average per Day",
                        value = com.paulohenriquesg.fahrenheit.stats.formatTime(avgPerDay)
                    )
                }
            }
        }
    }
}
