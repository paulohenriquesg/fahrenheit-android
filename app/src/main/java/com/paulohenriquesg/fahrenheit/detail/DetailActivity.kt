package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.podcast.EpisodeOrder
import com.paulohenriquesg.fahrenheit.podcast.DownloadProgress
import com.paulohenriquesg.fahrenheit.podcast.EpisodeProgress
import com.paulohenriquesg.fahrenheit.api.Me
import androidx.compose.runtime.mutableIntStateOf
import com.paulohenriquesg.fahrenheit.podcast.DownloadWatch
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.podcast.EpisodeTab
import com.paulohenriquesg.fahrenheit.podcast.FeedLoad
import com.paulohenriquesg.fahrenheit.podcast.PodcastEpisodesView
import com.paulohenriquesg.fahrenheit.podcast.PodcastFeed
import com.paulohenriquesg.fahrenheit.podcast.PodcastScreenModel
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate
import com.paulohenriquesg.fahrenheit.podcast.FeedCheck
import com.paulohenriquesg.fahrenheit.podcast.FeedCheckRow
import com.paulohenriquesg.fahrenheit.podcast.FeedCheckState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

class DetailActivity : ComponentActivity() {
    /** Counts returns to this screen, so progress is read again after the player. */
    private var resumes by mutableIntStateOf(0)

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
        var me by remember { mutableStateOf<Me?>(null) }

        val context = LocalContext.current

        LaunchedEffect(itemId) {
            val api = ApiClient.getLibraryApi()
            if (api == null) {
                loadFailed = true
                return@LaunchedEffect
            }
            LibraryRepository(api).item(itemId)
                .onSuccess { itemDetail = it; loadFailed = false }
                .onFailure { loadFailed = true }
        }

        // One call for both who the user is (the admin check) and what they
        // have heard (#78); read again on every return, e.g. from the player.
        val isPodcast = itemDetail?.mediaType == "podcast"
        LaunchedEffect(isPodcast, resumes) {
            if (!isPodcast) return@LaunchedEffect
            val podcastApi = ApiClient.getPodcastApi() ?: return@LaunchedEffect
            runCatching { podcastApi.me() }.onSuccess { me = it }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (item.mediaType == "podcast") {
                PodcastEpisodes(itemId, item, me, onReloaded = { itemDetail = it })
            } else {
                BookDetailView(
                    itemId = itemId,
                    content = DetailHeaderModel.book(item),
                    onPrimary = { context.startActivity(playBookIntent(context, itemId)) }
                )
            }
        }
    }

    /**
     * The podcast half of this screen (#76): every episode in the feed for an
     * admin, marked by whether the server has it, and the server's own for
     * anyone else. The header is the top of the episode list, so it scrolls
     * away as the viewer moves into the episodes.
     */
    @Composable
    private fun PodcastEpisodes(
        itemId: String,
        item: LibraryItemResponse,
        me: Me?,
        onReloaded: (LibraryItemResponse) -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var feed by remember { mutableStateOf<FeedLoad>(FeedLoad.Unavailable) }
        var tab by remember { mutableStateOf(EpisodeTab.All) }
        var feedCheck by remember { mutableStateOf<FeedCheckState>(FeedCheckState.Idle) }
        var queue by remember { mutableStateOf(DownloadQueue(null, emptyList())) }
        var misses by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
        val watch = remember(itemId) {
            ApiClient.getPodcastApi()?.let { DownloadWatch(it, item.libraryId, itemId) }
        }
        var watching by remember { mutableStateOf(false) }
        val serverFormat = remember { SharedPreferencesHandler(context).getUserPreferences().dateFormat }
        val media = item.media
        val feedUrl = media.metadata.feedUrl
        val isAdmin = FeedCheck.mayCheck(me?.type, feedUrl)
        val heard = EpisodeProgress.index(me?.mediaProgress.orEmpty(), itemId)
        val resumeEpisode = EpisodeProgress.resumable(
            me?.mediaProgress.orEmpty(), itemId, onServer = media.episodes.orEmpty().map { it.id }.toSet()
        )?.let { id -> media.episodes?.firstOrNull { it.id == id } }

        // Read on open, every time: nothing of the feed is stored.
        LaunchedEffect(isAdmin, feedUrl) {
            val podcastApi = ApiClient.getPodcastApi()
            if (!isAdmin || feedUrl.isNullOrBlank() || podcastApi == null) {
                feed = FeedLoad.Unavailable
                return@LaunchedEffect
            }
            feed = FeedLoad.Loading
            feed = PodcastFeed(podcastApi).episodes(feedUrl)
                .fold(onSuccess = { FeedLoad.Loaded(it) }, onFailure = { FeedLoad.Failed })
        }

        val now = remember(media) { System.currentTimeMillis() }
        val screen = PodcastScreenModel.of(
            server = media.episodes.orEmpty(),
            feed = feed,
            tab = tab,
            lastEpisodeCheck = media.lastEpisodeCheck,
            autoDownload = media.autoDownloadEpisodes,
            now = now,
            serverFormat = serverFormat
        )
        val header = DetailHeaderModel.podcast(item, screen.facts, resumeTitle = resumeEpisode?.title)
        val reload: suspend () -> LibraryItemResponse? = {
            ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(itemId).getOrNull() }
                ?.also(onReloaded)
        }
        // One watcher at a time; it stops once nothing here is queued or
        // awaited, and starts again on the next download.
        fun startWatch() {
            if (watching || watch == null) return
            watching = true
            scope.launch {
                try {
                    do {
                        watch.watch(
                            onUpdate = { q, m -> queue = q; misses = m },
                            reload = { reload()?.media?.episodes }
                        )
                    } while (watch.waiting)
                } finally {
                    watching = false
                }
            }
        }
        // On open too: episodes queued elsewhere show where they are.
        LaunchedEffect(isAdmin) { if (isAdmin) startWatch() }
        val downloads = screen.rows.mapNotNull { row ->
            DownloadProgress.state(row, queue, misses[row.key])?.let { row.key to it }
        }.toMap()

        val play: (Episode) -> Unit = { episode ->
            context.startActivity(playEpisodeIntent(context, itemId, episode.id))
        }

        PodcastEpisodesView(
            screen = screen,
            tab = tab,
            onTab = { tab = it },
            downloads = downloads,
            onPlay = play,
            progress = heard,
            onDownload = { row ->
                val episode = row.feed ?: return@PodcastEpisodesView
                if (watch == null) return@PodcastEpisodesView
                misses = misses + (row.key to 0)
                scope.launch {
                    if (watch.request(row.key, episode)) {
                        startWatch()
                    } else {
                        misses = misses + (row.key to DownloadProgress.MISSES_BEFORE_FAILED)
                    }
                }
            },
            coverItemId = itemId,
            date = { row -> EpisodeDate.of(row.publishedAt, now, serverFormat) },
            focusFirstRow = header.primary == null,
            title = media.metadata.title,
            header = {
                DetailHeader(
                    itemId = itemId,
                    content = header,
                    onPrimary = {
                        (resumeEpisode ?: EpisodeOrder.newestFirst(media.episodes.orEmpty()).firstOrNull())?.let(play)
                    },
                    actions = {
                        if (isAdmin) {
                            FeedCheckRow(state = feedCheck, onCheck = {
                                val podcastApi = ApiClient.getPodcastApi() ?: return@FeedCheckRow
                                scope.launch {
                                    FeedCheck(podcastApi).run(
                                        podcastId = itemId,
                                        episodesBefore = media.episodes?.size ?: 0,
                                        onState = {
                                            feedCheck = it
                                            // What it found is queued; watch it arrive in the list.
                                            if (it is FeedCheckState.Found && it.count > 0) startWatch()
                                        },
                                        reload = { reload()?.media?.episodes?.size }
                                    )
                                }
                            })
                        }
                    }
                )
            }
        )
    }

    companion object {
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
    }
}