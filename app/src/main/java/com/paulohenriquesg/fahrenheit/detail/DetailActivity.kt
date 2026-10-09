package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.paulohenriquesg.fahrenheit.podcast.PodcastViewModel
import com.paulohenriquesg.fahrenheit.podcast.PodcastPage
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.FavouritesChoice
import com.paulohenriquesg.fahrenheit.favourites.LibraryFavourites
import com.paulohenriquesg.fahrenheit.favourites.note
import com.paulohenriquesg.fahrenheit.podcast.EpisodeHearts
import com.paulohenriquesg.fahrenheit.podcast.EpisodeTabChoice
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.podcast.EpisodeOrder
import com.paulohenriquesg.fahrenheit.podcast.DownloadProgress
import com.paulohenriquesg.fahrenheit.podcast.EpisodeProgress
import com.paulohenriquesg.fahrenheit.podcast.EpisodeMarking
import com.paulohenriquesg.fahrenheit.podcast.EpisodeMarks
import com.paulohenriquesg.fahrenheit.api.Me
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulohenriquesg.fahrenheit.progress.ProgressKey
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import com.paulohenriquesg.fahrenheit.podcast.DownloadWatch
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.podcast.FeedLoad
import com.paulohenriquesg.fahrenheit.podcast.PodcastEpisodesView
import com.paulohenriquesg.fahrenheit.podcast.PodcastFeed
import com.paulohenriquesg.fahrenheit.podcast.PodcastScreenModel
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate
import com.paulohenriquesg.fahrenheit.podcast.FeedCheck
import com.paulohenriquesg.fahrenheit.podcast.FeedCheckState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import com.paulohenriquesg.fahrenheit.player.ActionChip
import com.paulohenriquesg.fahrenheit.podcast.DownloadSettings
import com.paulohenriquesg.fahrenheit.podcast.DownloadsPanel
import com.paulohenriquesg.fahrenheit.podcast.PodcastDownloads
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import android.util.Log
import com.paulohenriquesg.fahrenheit.player.AboutFact
import com.paulohenriquesg.fahrenheit.player.AboutFacts
import com.paulohenriquesg.fahrenheit.player.ChapterClock
import com.paulohenriquesg.fahrenheit.player.SeriesBooks
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.ControllerSlot
import com.paulohenriquesg.fahrenheit.main.NowPlayingBarSlot
import com.paulohenriquesg.fahrenheit.main.queuedChapters
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

class DetailActivity : ComponentActivity() {
    /** Counts returns to this screen, so favourites are read again (#180); progress is the store's (#207). */
    private var resumes by mutableIntStateOf(0)

    /**
     * The playback service's player while this screen is visible, for the
     * Now playing bar (#159): this screen has no rail. As on the main screen.
     */
    private var playback by mutableStateOf<MediaController?>(null)
    private val connection by lazy {
        ControllerSlot(
            connect = { Playback.connect(this) },
            release = { it.release() },
            executor = ContextCompat.getMainExecutor(this),
            onChange = { playback = it }
        )
    }

    override fun onStart() {
        super.onStart()
        connection.open()
    }

    override fun onStop() {
        // Leaves playback alone: the bar's Stop ends it.
        connection.close()
        playback = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)

        setContent {
            FahrenheitTheme {
                Surface(
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    if (itemId != null) {
                        DetailScreen(itemId)
                    } else {
                        Toast.makeText(this@DetailActivity, getString(R.string.item_id_is_missing), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    fun DetailScreen(itemId: String) {
        var itemDetail by remember { mutableStateOf<LibraryItemResponse?>(null) }
        var loadFailed by remember { mutableStateOf(false) }

        val context = LocalContext.current
        var marking by remember { mutableStateOf(false) }

        LaunchedEffect(itemId) {
            val api = ApiClient.getLibraryApi()
            if (api == null) {
                loadFailed = true
                return@LaunchedEffect
            }
            val since = ProgressStore.process.generation
            LibraryRepository(api).item(itemId)
                // Its progress goes in the store, which says what it is from then on (#207).
                .onSuccess { ProgressStore.process.readItem(it, since); itemDetail = it; loadFailed = false }
                .onFailure { loadFailed = true }
        }

        // Progress is the shared store's (#207): back from the player, Chapters
        // opens where it was left and Mark says what the server holds, with no
        // read of this screen's own.
        val progress by ProgressStore.process.entries.collectAsStateWithLifecycle()

        // The rest of a book's series (#134), as the player reads it.
        val seriesRef = itemDetail?.media?.metadata?.series?.firstOrNull()
        var seriesBooks by remember { mutableStateOf<SeriesBooks?>(null) }
        LaunchedEffect(seriesRef?.id, itemDetail?.libraryId) {
            val ref = seriesRef ?: return@LaunchedEffect
            val libraryId = itemDetail?.libraryId ?: return@LaunchedEffect
            val api = ApiClient.getLibraryApi() ?: return@LaunchedEffect
            seriesBooks = LibraryRepository(api).seriesBooks(libraryId, ref.id)
                .onFailure { Log.w(TAG, "Couldn't read the series ${ref.id}", it) }
                .getOrNull()
                ?.let { SeriesBooks.of(it, currentId = itemId) }
                ?.also { if (it.current == null) Log.w(TAG, "The series ${ref.id} does not list $itemId") }
                ?.takeIf { it.current != null }
        }

        if (loadFailed) {
            // The previous version showed an empty screen with a toast that
            // was gone by the time anyone looked at it.
            Text(
                text = stringResource(R.string.item_load_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            return
        }

        val item = itemDetail
        if (item == null) {
            Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
            return
        }

        val isBook = item.mediaType != "podcast"
        val nowPlaying: @Composable () -> Unit = {
            NowPlayingBarSlot(playback, chaptersOf = ::queuedChapters) {
                // The player reattaches to what is queued, where it is.
                context.startActivity(PlayerActivity.createIntent(context, it.itemId, it.episodeId))
            }
        }
        DetailBody { margin ->
            if (!isBook) {
                PodcastEpisodes(itemId, item, margin, nowPlaying = nowPlaying)
            } else {
                val bookProgress = progress[ProgressKey(itemId, null)]
                BookDetailView(
                    itemId = itemId,
                    content = DetailHeaderModel.book(item, bookProgress),
                    onPrimary = { context.startActivity(playBookIntent(context, itemId)) },
                    chapters = remember(item) { ChapterClock.spans(item.media.chapters, item.media.duration ?: 0.0) },
                    at = bookProgress?.currentTime ?: 0.0,
                    onChapter = { start -> context.startActivity(playChapterIntent(context, itemId, start)) },
                    padding = margin,
                    facts = remember(item, bookProgress) {
                        AboutFacts.book(item.media.metadata, item.media.duration) +
                            listOfNotNull(DetailHeaderModel.progressOf(bookProgress)?.let { AboutFact(AboutFact.Kind.Progress, it) })
                    },
                    series = seriesBooks,
                    seriesName = seriesRef?.name,
                    onSeriesBook = { context.startActivity(createIntent(context, it.itemId)) },
                    nowPlaying = nowPlaying,
                    finished = bookProgress?.isFinished == true,
                    marking = marking,
                    onMarkFinished = { done ->
                        marking = true
                        // Through the playback service, in case this book is the one playing (#105).
                        Playback.markFinished(context, itemId, done, keepAt = placeToKeep(item, bookProgress)) { worked ->
                            marking = false
                            // Done, the facts and Resume follow: the service put the mark in the store (#207).
                            if (!worked) {
                                Toast.makeText(context, context.getString(R.string.mark_finished_failed), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * The podcast half of this screen (#76), in the book page's layout (#205):
     * its state is [PodcastViewModel]'s (#208); here are only where a press
     * leads and what is said aloud.
     */
    @Composable
    private fun PodcastEpisodes(
        itemId: String,
        item: LibraryItemResponse,
        margin: PaddingValues,
        nowPlaying: @Composable () -> Unit
    ) {
        val context = LocalContext.current
        val app = context.applicationContext
        val now = remember { System.currentTimeMillis() }
        val serverFormat = remember { SharedPreferencesHandler(app).getUserPreferences().dateFormat }
        val model = viewModel(key = "podcast:$itemId") {
            PodcastViewModel(
                itemId = itemId,
                item = item,
                podcastApi = ApiClient.getPodcastApi(),
                settingsApi = ApiClient.getPodcastSettingsApi(),
                reloadItem = { ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(itemId).getOrNull() } },
                markFinished = { episodeId, finished, keepAt ->
                    // Through the playback service, in case this episode is the one playing.
                    suspendCancellableCoroutine { done ->
                        Playback.markFinished(app, itemId, finished, episodeId, keepAt) { done.resume(it) }
                    }
                },
                now = now,
                serverFormat = serverFormat,
                // The library's Favourites playlist, for the hearts and the tab (#180).
                favourites = ApiClient.getPlaylistApi()?.let {
                    LibraryFavourites(Favourites(it, FavouritesChoice(app)), item.libraryId)
                }
            )
        }
        val state by model.state.collectAsState()
        // Coming back, from the player say: what was heard there.
        LaunchedEffect(resumes) { if (resumes > 1) model.refresh() }
        LaunchedEffect(state.markFailed) {
            if (!state.markFailed) return@LaunchedEffect
            Toast.makeText(context, context.getString(R.string.mark_finished_failed), Toast.LENGTH_LONG).show()
            model.markFailureShown()
        }
        LaunchedEffect(state.heartChange) {
            val change = state.heartChange ?: return@LaunchedEffect
            Toast.makeText(context, change.note(context), Toast.LENGTH_SHORT).show()
            model.heartChangeShown()
        }
        val play: (Episode) -> Unit = { episode ->
            context.startActivity(playEpisodeIntent(context, itemId, episode.id))
        }
        PodcastPage(
            state = state,
            margin = margin,
            onPrimary = { state.primaryEpisode?.let(play) },
            onTab = model::chooseTab,
            onPlay = play,
            onDownload = model::download,
            onMark = model::mark,
            onCheckFeed = model::checkFeed,
            onChangeDownloads = model::changeDownloads,
            onDownloadsClosed = model::downloadsSeen,
            onFavourite = model::toggleFavourite,
            date = { row -> EpisodeDate.of(row.publishedAt, now, serverFormat) },
            nowPlaying = nowPlaying
        )
    }

    companion object {
        private const val TAG = "DetailActivity"
        private const val EXTRA_ITEM_ID = "item_id"

        fun createIntent(context: Context, itemId: String): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
            }
        }

        /**
         * The book's Play. Starts playing, as Home's podcast shelves do: the
         * press was already a request to play (#122).
         */
        fun playBookIntent(context: Context, itemId: String): Intent =
            PlayerActivity.createIntent(context, itemId, autoPlay = true)

        /** An episode chosen from the list, or the header's Play/Resume. Starts playing (#122). */
        fun playEpisodeIntent(context: Context, itemId: String, episodeId: String): Intent =
            PlayerActivity.createIntent(context, itemId, episodeId, autoPlay = true)

        /**
         * Where an un-finished book should stay, rather than the start the
         * server would put it at: where it was, unless that is so near the end
         * that the server would finish it again (its rule: under 10 s left).
         */
        internal fun placeToKeep(item: LibraryItemResponse, progress: MediaProgressResponse?): Double? {
            val at = progress?.currentTime ?: return null
            val length = item.media.duration ?: return null
            return at.takeIf { at > 0 && length - at > 10 }
        }

        /** A chapter chosen from the book's Chapters: the player opens there and plays (#105). */
        fun playChapterIntent(context: Context, itemId: String, start: Double): Intent =
            PlayerActivity.createIntent(context, itemId, autoPlay = true, startAt = start)
    }
}

/**
 * The details screen's body. The screen's margin is kept inside the content
 * (handed to [content]), so a panel - a book's Chapters, a podcast's
 * Downloads (#182) - reaches the screen's edges.
 */
@Composable
internal fun DetailBody(content: @Composable (PaddingValues) -> Unit) {
    Box(Modifier.fillMaxSize()) { content(PaddingValues(horizontal = 24.dp, vertical = 16.dp)) }
}
