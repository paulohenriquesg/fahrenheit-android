package com.paulohenriquesg.fahrenheit.main

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.player.PlayerSettings
import android.app.Activity
import com.paulohenriquesg.fahrenheit.screensaver.PlaybackListening
import com.paulohenriquesg.fahrenheit.settings.SettingsView
import com.paulohenriquesg.fahrenheit.settings.UpdateCheck
import com.paulohenriquesg.fahrenheit.player.PlaybackDevice
import com.paulohenriquesg.fahrenheit.update.AvailableUpdate
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
import androidx.compose.runtime.SideEffect
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
import com.paulohenriquesg.fahrenheit.api.LibraryQuery
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.FavouritesSetting
import com.paulohenriquesg.fahrenheit.favourites.FavouritesShelf
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.ui.theme.LayoutManager
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuConfig
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.search.SearchActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.SeriesShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.ShelvesPlaceholder
import com.paulohenriquesg.fahrenheit.ui.elements.ShelfRow
import com.paulohenriquesg.fahrenheit.ui.elements.CoverProgress
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemsFluid
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemsRow
import kotlinx.coroutines.launch
import com.paulohenriquesg.fahrenheit.utils.StartupTimeline

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(
    fetchLibraryItems: suspend (String, LibraryQuery) -> List<LibraryItem>,
    /** A library's Home shelves; null when they could not be read. */
    fetchPersonalizedView: suspend (String) -> List<Shelf>?,
    /** The playback service's player, while this screen is visible; null when not connected (#107). */
    playback: Player? = null,
    /** Favourites (#180), while signed in. */
    favourites: () -> Favourites? = { null },
    /** Settings' Install: the activity asks the update question over Home (#229). */
    onInstallUpdate: (AvailableUpdate) -> Unit = {},
    openSection: String? = null,
    onSectionOpened: () -> Unit = {},
    /** Home's state, out of this composable as it is touched (#208). */
    homeModel: HomeViewModel = viewModel { HomeViewModel(ProgressStore.process, fetchPersonalizedView) },
    /** The shell's state: libraries, the view, the rail's highlight, the browse lists (#208). */
    mainModel: MainViewModel = LocalContext.current.let { context ->
        viewModel { MainViewModel.of(context, fetchLibraryItems, homeModel::open) }
    }
) {
    val home by homeModel.uiState.collectAsStateWithLifecycle()
    val main by mainModel.uiState.collectAsStateWithLifecycle()
    val shelves = home.shelves
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPreferencesHandler = remember { SharedPreferencesHandler(context) }
    val userPreferences = sharedPreferencesHandler.getUserPreferences()
    val username = userPreferences.username
    val currentLibrary = main.library
    val view = main.view
    val listState = rememberLazyListState()
    val isRowLayout by LayoutManager.isRowLayout  // Use LayoutManager instead of local state

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
        // Stopped since the last resume: the first resume is the start-up load's.
        var left = false
        val observer = LifecycleEventObserver { _, event ->
            // And only while the screen is up: news from the player waits for the return.
            if (event == Lifecycle.Event.ON_START) homeModel.screenUp(true)
            if (event == Lifecycle.Event.ON_STOP) {
                left = true
                homeModel.screenUp(false)
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                // A library chosen meanwhile is opened, Home with it; otherwise,
                // back from the player, say: the shelves are read again (#197),
                // Favourites among them, which the heart may have changed (#180).
                if (!mainModel.followSavedLibrary() && left) homeModel.returned()
                left = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(shelves.isNotEmpty()) {
        if (shelves.isNotEmpty()) StartupTimeline.app.mark("home shelves loaded", "${shelves.size} shelves")
    }

    // Home reloads only while it is the view; what comes meanwhile is owed (#197).
    SideEffect { homeModel.visible(view == MainView.HOME) }

    // Only a load asked for that leaves Home empty says it failed (#197).
    LaunchedEffect(homeModel) {
        homeModel.failures.collect {
            Toast.makeText(context, context.getString(R.string.home_shelves_failed), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(mainModel) {
        mainModel.librariesFailed.collect { why ->
            Toast.makeText(context, "Failed to load libraries: $why", Toast.LENGTH_SHORT).show()
        }
    }

    // Something must hold focus or the remote does nothing at all (#53). The
    // rail's selected section is the one target every view is guaranteed to
    // have, and RIGHT goes from there into the content.
    val initialFocus = rememberInitialFocus(
        enabled = true,
        view, shelves, main.listing.items, main.series, main.collections, main.listeningStats
    )

    var updateCheck by remember { mutableStateOf<UpdateCheck>(UpdateCheck.Idle) }
    var deviceName by remember { mutableStateOf(PlaybackDevice.name(context)) }
    var playNextEpisode by remember { mutableStateOf(PlayerSettings(context).playNextEpisode) }
    var screensaverMinutes by remember { mutableStateOf(PlayerSettings(context).screensaverMinutes) }
    var screensaverStyle by remember { mutableStateOf(PlayerSettings(context).screensaverStyle) }
    val listening = remember { PlaybackListening(context) }
    var skipBack by remember { mutableStateOf(PlayerSettings(context).skipBackSeconds) }
    var skipForward by remember { mutableStateOf(PlayerSettings(context).skipForwardSeconds) }
    // The library's Favourites playlist, read each time Settings opens (#180).
    var favouritesChosen by remember { mutableStateOf<Playlist?>(null) }
    LaunchedEffect(view, currentLibrary?.id) {
        if (view != MainView.SETTINGS) return@LaunchedEffect
        val libraryId = currentLibrary?.id ?: return@LaunchedEffect
        favourites()?.current(libraryId)?.onSuccess { favouritesChosen = it }
    }
    val favouritesSetting = currentLibrary?.id?.let { libraryId ->
        favourites()?.let { kept ->
            FavouritesSetting(
                libraryId = libraryId,
                libraryName = currentLibrary.name.orEmpty(),
                chosen = favouritesChosen,
                load = { kept.playlists(libraryId) },
                choose = { pick ->
                    kept.choose(libraryId, pick).map { chosen ->
                        favouritesChosen = chosen
                        // Home's shelf follows the choice, in this screen's scope: the panel closes meanwhile.
                        homeModel.refreshFavourites(kept)
                    }
                }
            )
        }
    }

    // Back walks up a level: content -> rail -> Home -> out (#125).
    var railHasFocus by remember { mutableStateOf(false) }
    MainBackHandler(view = view, railHasFocus = railHasFocus, rail = initialFocus, onGoHome = mainModel::goHome)

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

    // The model changes the view; what is left here acts on the screen itself.
    fun handleMenuAction(action: MenuAction, query: LibraryQuery, choose: () -> Unit) {
        // A different view opens at its top, not where the last one was.
        val anotherList = action == MenuAction.LIBRARY && query != mainModel.uiState.value.listing.query
        choose()
        when (action) {
            MenuAction.HOME -> if (mainModel.uiState.value.library?.id != null) homeModel.choseHome()
            MenuAction.LIBRARY -> if (anotherList) scope.launch { listState.scrollToItem(0) }
            MenuAction.LOGOUT -> signOut()
            else -> Unit
        }
    }

    // A section chosen from the side menu on another screen (#230), applied
    // as the rail's own choice once the library's menu has it.
    LaunchedEffect(openSection, menuItems) {
        val asked = openSection ?: return@LaunchedEffect
        val item = (menuItems + MenuConfig.commonItems).firstOrNull { it.id == asked } ?: return@LaunchedEffect
        handleMenuAction(item.action, LibraryQuery.Everything) { mainModel.select(item.id, item.action) }
        onSectionOpened()
    }

    NavigationRail(
        items = menuItems,
        secondary = MenuConfig.commonItems,
        selectedId = main.highlighted,
        firstFocus = initialFocus,
        onRailFocusChanged = { railHasFocus = it },
        // What the playback service has queued: the way back to the player (#107).
        nowPlaying = playback?.let { player ->
            { open ->
                NowPlayingSlot(
                    player = player,
                    open = open,
                    chaptersOf = ::queuedChapters,
                    // The player reattaches to what is queued, where it is.
                    onOpen = { context.startActivity(PlayerActivity.reopenIntent(context, it)) },
                    // Stop's button goes with the entry: focus to the section, or the remote has nothing (#53).
                    onStopped = { runCatching { initialFocus.requestFocus() } }
                )
            }
        },
        onSelect = { menuItem ->
            handleMenuAction(menuItem.action, LibraryQuery.Everything) { mainModel.select(menuItem.id, menuItem.action) }
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
                    MainView.HOME -> PersonalizedHomeView(shelves, currentLibrary?.id, home.loading, home.covers, onSeeAll = { tile ->
                        handleMenuAction(tile.opens, tile.query) { mainModel.seeAll(tile.opens, tile.query) }
                    })
                    MainView.LIBRARY -> LibraryBrowseView(
                        name = currentLibrary?.name,
                        itemLabel = if (main.libraries.find { it.name == currentLibrary?.name }?.mediaType == "book") "books" else "podcasts",
                        items = main.listing.items,
                        rowLayout = isRowLayout,
                        listState = listState,
                        query = main.listing.query,
                        loading = main.listing.loading
                    )
                    MainView.SERIES -> SeriesBrowseView(main.series, main.seriesLoading)
                    MainView.AUTHORS -> AuthorsBrowseView(currentLibrary?.id)
                    MainView.NARRATORS -> NarratorsBrowseView(currentLibrary?.id)
                    MainView.COLLECTIONS -> CollectionsBrowseView(main.collections, main.collectionsLoading)
                    MainView.STATS -> StatsBrowseView(main.listeningStats, main.statsLoading)
                    MainView.LATEST -> currentLibrary?.id?.let { id ->
                        com.paulohenriquesg.fahrenheit.podcast.LatestEpisodesView(libraryId = id, heard = home.episodes)
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
                                // Asked for, so it offers an ignored version too,
                                // and a failure says so.
                                updateCheck = UpdateCheck.from(AppUpdates.checker(context).checkNow())
                            }
                        },
                        onInstall = onInstallUpdate,
                        username = username,
                        server = sharedPreferencesHandler.getUserPreferences().host,
                        onSignOut = { signOut() },
                        deviceName = deviceName,
                        onDeviceName = {
                            PlaybackDevice.setName(context, it)
                            deviceName = PlaybackDevice.name(context)
                        },
                        playNextEpisode = playNextEpisode,
                        onPlayNextEpisode = {
                            PlayerSettings(context).playNextEpisode = it
                            playNextEpisode = it
                        },
                        skipBack = skipBack,
                        onSkipBack = {
                            PlayerSettings(context).skipBackSeconds = it
                            skipBack = it
                        },
                        screensaverMinutes = screensaverMinutes,
                        onScreensaverMinutes = {
                            PlayerSettings(context).screensaverMinutes = it
                            screensaverMinutes = it
                        },
                        screensaverStyle = screensaverStyle,
                        onScreensaverStyle = {
                            PlayerSettings(context).screensaverStyle = it
                            screensaverStyle = it
                        },
                        screensaver = listening,
                        skipForward = skipForward,
                        onSkipForward = {
                            PlayerSettings(context).skipForwardSeconds = it
                            skipForward = it
                        },
                        favourites = favouritesSetting
                    )
                    MainView.SWITCH_LIBRARY -> SwitchLibraryView(
                        libraries = main.libraries,
                        currentId = currentLibrary?.id,
                        stats = main.libraryStats,
                        onSelect = mainModel::chooseLibrary
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
    // The shelves carry no progress: it comes from the shared store (#207),
    // which the player writes as it reports.
    progress: CoverProgress = CoverProgress.None,
    onSeeAll: (ShelfSeeAll.Tile) -> Unit = {}
) {
    val context = LocalContext.current
    // The shelves Home draws, each with what it does (#147); empty ones and
    // ones it cannot draw are left out.
    val drawn = shelves
        .filter { !it.bookEntities.isNullOrEmpty() || !it.authorEntities.isNullOrEmpty() || !it.seriesEntities.isNullOrEmpty() }
        .mapNotNull { shelf -> HomeShelves.of(shelf)?.let { shelf to it } }
    val nonEmptyShelves = drawn.map { it.first }

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
            ShelvesPlaceholder()
            return@Column
        }

        androidx.compose.foundation.lazy.LazyColumn {
            val shelfKeys = StableKeys.of(nonEmptyShelves) { s -> s.id }
            items(nonEmptyShelves.size, key = { shelfKeys[it] }) { index ->
                val (shelf, behaviour) = drawn[index]
                val seeAll = ShelfSeeAll.of(shelf)
                when (behaviour.style) {
                    HomeShelves.Style.Episodes -> {
                        shelf.bookEntities?.let { books ->
                            ShelfRow(shelf = shelf, progress = progress, marksFinished = behaviour.marksFinished, seeAllTotal = seeAll?.total, onSeeAll = { seeAll?.let { onSeeAll(it) } }) { item ->
                                val episodeId = item.recentEpisode?.id
                                val podcastId = item.recentEpisode?.libraryItemId ?: item.id
                                if (episodeId != null) {
                                    val intent = PlayerActivity.createIntent(context, podcastId, episodeId, autoPlay = true, preview = PlayerPreview.of(item))
                                    context.startActivity(intent)
                                } else {
                                    val intent = com.paulohenriquesg.fahrenheit.detail.DetailActivity.intentFor(context, item)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                    HomeShelves.Style.Covers -> {
                        shelf.bookEntities?.let { books ->
                            fun act(action: HomeShelves.Action, item: LibraryItem) {
                                when {
                                    // A book whose audio has gone would only reach
                                    // a player that cannot start; its details say why.
                                    action == HomeShelves.Action.Play && item.media.numAudioFiles > 0 ->
                                        context.startActivity(PlayerActivity.createIntent(context, item.id, autoPlay = true, preview = PlayerPreview.of(item)))
                                    else ->
                                        context.startActivity(com.paulohenriquesg.fahrenheit.detail.DetailActivity.intentFor(context, item))
                                }
                            }
                            ShelfRow(
                                shelf = shelf,
                                progress = progress,
                                seeAllTotal = seeAll?.total,
                                onSeeAll = { seeAll?.let { onSeeAll(it) } },
                                onItemLongClick = behaviour.longPress?.let { longPress -> { item -> act(longPress, item) } }
                            ) { item -> act(behaviour.press, item) }
                        }
                    }
                    HomeShelves.Style.Authors -> {
                        shelf.authorEntities?.let { authors ->
                            AuthorShelfRow(shelf = shelf, authors = authors, seeAllTotal = seeAll?.total, onSeeAll = { seeAll?.let { onSeeAll(it) } }) { author ->
                                val intent = com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity.createIntent(context, author.id, PlayerPreview.author(author))
                                context.startActivity(intent)
                            }
                        }
                    }
                    HomeShelves.Style.Series -> {
                        shelf.seriesEntities?.let { series ->
                            SeriesShelfRow(shelf = shelf, series = series, seeAllTotal = seeAll?.total, onSeeAll = { seeAll?.let { onSeeAll(it) } }) { seriesItem ->
                                val intent = com.paulohenriquesg.fahrenheit.group.BookGroupActivity.forSeries(context, seriesItem)
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
