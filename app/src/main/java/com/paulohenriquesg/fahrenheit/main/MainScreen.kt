package com.paulohenriquesg.fahrenheit.main

import android.app.Activity
import com.paulohenriquesg.fahrenheit.settings.SettingsView
import com.paulohenriquesg.fahrenheit.settings.UpdateCheck
import com.paulohenriquesg.fahrenheit.player.PlaybackDevice
import com.paulohenriquesg.fahrenheit.library.SwitchLibraryView
import com.paulohenriquesg.fahrenheit.ui.theme.ThemeManager
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.update.AppUpdates
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.utils.Alphabetical
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import kotlin.random.Random
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryStats
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.ui.theme.LayoutManager
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuConfig
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.search.SearchActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.SeriesShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.ShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.CoverProgress
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import androidx.compose.runtime.mutableIntStateOf
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemsFluid
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemsRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(
    fetchLibraryItems: suspend (String) -> List<LibraryItem>,
    fetchPersonalizedView: suspend (String) -> List<Shelf>,
    fetchProgress: suspend () -> List<MediaProgressResponse> = { emptyList() }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPreferencesHandler = remember { SharedPreferencesHandler(context) }
    val userPreferences = sharedPreferencesHandler.getUserPreferences()
    val username = userPreferences.username
    var libraries by remember { mutableStateOf(listOf<Library>()) }
    var libraryStats by remember { mutableStateOf(mapOf<String, LibraryStats>()) }
    var libraryItems by remember { mutableStateOf(listOf<LibraryItem>()) }
    var shelves by remember { mutableStateOf(listOf<Shelf>()) }
    var currentLibrary by remember { mutableStateOf<Library?>(null) }
    val listState = rememberLazyListState()
    val isRowLayout by LayoutManager.isRowLayout  // Use LayoutManager instead of local state

    var view by remember { mutableStateOf(MainView.HOME) }
    // Which menu row the drawer highlights and focuses when reopened. Kept
    // apart from `view` because rows like Settings open another screen and
    // still take the highlight; changing that is a focus change for a TV, not
    // a refactor.
    var highlightedMenuItemId by remember { mutableStateOf(MainView.HOME.menuItemId) }
    var shouldRefreshLibrary by remember { mutableStateOf(false) }
    var seriesList by remember { mutableStateOf(listOf<com.paulohenriquesg.fahrenheit.api.Series>()) }
    var collectionsList by remember { mutableStateOf(listOf<com.paulohenriquesg.fahrenheit.api.Collection>()) }
    var isLoadingHome by remember { mutableStateOf(false) }
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

                // Drop what belongs to the library being left: Home showed its
                // shelves for the second or two the fetch took.
                shelves = emptyList()
                libraryItems = emptyList()
                isLoadingHome = true
                newLibrary.id?.let { libraryId ->
                    shelves = fetchPersonalizedView(libraryId)
                    libraryItems = fetchLibraryItems(libraryId)
                }
                isLoadingHome = false
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

    // Counts for the Switch Library tiles, asked for each time that view opens
    // and never in its way: a tile without them says what kind of library it is.
    LaunchedEffect(view, libraries) {
        if (view != MainView.SWITCH_LIBRARY) return@LaunchedEffect
        val repository = ApiClient.getLibraryApi()?.let(::LibraryRepository) ?: return@LaunchedEffect
        libraries.mapNotNull { it.id }.forEach { id ->
            repository.stats(id).onSuccess { libraryStats = libraryStats + (id to it) }
        }
    }

    // Function to scroll to the first item
    fun scrollToFirstItem() {
        scope.launch {
            listState.scrollToItem(0)
        }
    }

    // Something must hold focus or the remote does nothing at all (#53). The
    // rail's selected section is the one target every view is guaranteed to
    // have, and RIGHT goes from there into the content.
    val initialFocus = rememberInitialFocus(
        enabled = true,
        view, shelves, libraryItems, seriesList, collectionsList, listeningStats
    )

    var updateCheck by remember { mutableStateOf<UpdateCheck>(UpdateCheck.Idle) }
    var deviceName by remember { mutableStateOf(PlaybackDevice.name(context)) }

    val backAction = BackAction.decide(view)
    BackHandler(enabled = backAction != BackAction.Exit) {
        when (backAction) {
            BackAction.GoHome -> {
                view = MainView.HOME
                highlightedMenuItemId = MainView.HOME.menuItemId
            }
            BackAction.Exit -> Unit
        }
    }

    // Get menu items for current library type
    val menuItems = remember(currentLibrary?.mediaType) {
        MenuConfig.getMenuForLibraryType(currentLibrary?.mediaType)
    }

    // Handle menu actions
    // Settings offers this too, so it is not only a menu row.
    fun signOut() {
        // Stop playback, then drop the stored session and the client built
        // from it. The stop is asynchronous, so its closing progress report
        // usually goes out after the session is gone and is lost: at most
        // one round of listening, accepted rather than holding sign-out up.
        Playback.stop(context)
        sharedPreferencesHandler.clearSession()
        ApiClient.clearSession()
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

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
                view = MainView.LATEST
            }
            MenuAction.SELECT_LIBRARY -> {
                view = MainView.SWITCH_LIBRARY
            }
            MenuAction.SETTINGS -> {
                view = MainView.SETTINGS
            }
            MenuAction.LOGOUT -> signOut()
        }
    }

    NavigationRail(
        items = menuItems,
        secondary = MenuConfig.commonItems,
        selectedId = highlightedMenuItemId,
        firstFocus = initialFocus,
        onSelect = { menuItem ->
            highlightedMenuItemId = menuItem.id
            handleMenuAction(menuItem.action, currentLibrary?.id)
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_screen")
            ) {
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
                    MainView.HOME -> PersonalizedHomeView(shelves, currentLibrary?.id, isLoadingHome, fetchProgress)
                    MainView.LIBRARY -> LibraryBrowseView(
                        name = currentLibrary?.name,
                        itemLabel = if (libraries.find { it.name == currentLibrary?.name }?.mediaType == "book") "books" else "podcasts",
                        items = libraryItems,
                        rowLayout = isRowLayout,
                        listState = listState
                    )
                    MainView.SERIES -> SeriesBrowseView(seriesList, isLoadingSeries)
                    MainView.AUTHORS -> AuthorsBrowseView(currentLibrary?.id)
                    MainView.COLLECTIONS -> CollectionsBrowseView(collectionsList, isLoadingCollections)
                    MainView.STATS -> StatsBrowseView(listeningStats, isLoadingStats)
                    MainView.LATEST -> currentLibrary?.id?.let { id ->
                        com.paulohenriquesg.fahrenheit.podcast.LatestEpisodesView(libraryId = id)
                    }
                    MainView.SETTINGS -> SettingsView(
                        theme = ThemeManager.preference(context),
                        onTheme = { ThemeManager.apply(context, it) },
                        rowLayout = LayoutManager.isRowLayout.value,
                        onLayout = { LayoutManager.setLayout(context, it) },
                        version = BuildConfig.VERSION_NAME,
                        update = updateCheck,
                        onCheckUpdates = {
                            updateCheck = UpdateCheck.Checking
                            scope.launch {
                                // Asked for, so it answers even while snoozed.
                                val available = AppUpdates.checker(context).check(force = true)
                                updateCheck = if (available != null) {
                                    UpdateCheck.Available(available.versionName)
                                } else {
                                    UpdateCheck.UpToDate
                                }
                            }
                        },
                        username = username,
                        server = sharedPreferencesHandler.getUserPreferences().host,
                        onSignOut = { signOut() },
                        deviceName = deviceName,
                        onDeviceName = {
                            PlaybackDevice.setName(context, it)
                            deviceName = PlaybackDevice.name(context)
                        }
                    )
                    MainView.SWITCH_LIBRARY -> SwitchLibraryView(
                        libraries = libraries,
                        currentId = currentLibrary?.id,
                        stats = libraryStats,
                        onSelect = { chosen ->
                            // Save and ask for a refresh, and nothing else: the
                            // refresh compares the saved library with the current
                            // one, so setting the current one here first made them
                            // equal and the shelves were never fetched again.
                            chosen.id?.let { sharedPreferencesHandler.saveSelectedLibraryId(it) }
                            shouldRefreshLibrary = true
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // One greeting per visit to the screen, not one per recomposition.
    val seed = remember { Random.nextInt() }
    Text(
        text = Welcome.pick(name, seed),
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

fun getIconForMediaType(mediaType: String?): ImageVector {
    return when (mediaType) {
        "book" -> Icons.Filled.Book
        "podcast" -> Icons.Filled.Podcasts
        else -> Icons.Filled.Book // default icon
    }
}

@Composable
fun PersonalizedHomeView(
    shelves: List<Shelf>,
    libraryId: String?,
    isLoading: Boolean = false,
    fetchProgress: suspend () -> List<MediaProgressResponse> = { emptyList() }
) {
    val context = LocalContext.current
    // The shelves carry no progress, so it comes from GET /api/me: read again
    // with each new set of shelves and on every return, typically from the
    // player, so a cover's time left is not the one from before listening.
    var progress by remember { mutableStateOf(CoverProgress.None) }
    var resumes by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumes++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(shelves, resumes) {
        runCatching { fetchProgress() }.onSuccess { progress = CoverProgress.index(it) }
    }
    // Filter out empty shelves
    val nonEmptyShelves = shelves.filter {
        (it.bookEntities != null && it.bookEntities.isNotEmpty()) ||
        (it.authorEntities != null && it.authorEntities.isNotEmpty()) ||
        (it.seriesEntities != null && it.seriesEntities.isNotEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = stringResource(R.string.home),
            modifier = Modifier.padding(bottom = Space.gap)
        )

        if (isLoading && nonEmptyShelves.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        androidx.compose.foundation.lazy.LazyColumn {
            val shelfKeys = StableKeys.of(nonEmptyShelves) { s -> s.id }
            items(nonEmptyShelves.size, key = { shelfKeys[it] }) { index ->
                val shelf = nonEmptyShelves[index]
                when (shelf.type) {
                    "episode" -> {
                        shelf.bookEntities?.let { books ->
                            ShelfRow(shelf = shelf, progress = progress) { item ->
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
                            ShelfRow(shelf = shelf, progress = progress) { item ->
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




