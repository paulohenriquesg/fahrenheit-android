package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.utils.formatPubDate
import com.paulohenriquesg.fahrenheit.utils.formatDuration
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.utils.RichText
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.podcast.EpisodeOrder
import com.paulohenriquesg.fahrenheit.podcast.DownloadState
import com.paulohenriquesg.fahrenheit.podcast.EpisodeTab
import com.paulohenriquesg.fahrenheit.podcast.FeedLoad
import com.paulohenriquesg.fahrenheit.podcast.PodcastEpisodesView
import com.paulohenriquesg.fahrenheit.podcast.PodcastFeed
import com.paulohenriquesg.fahrenheit.podcast.PodcastScreenModel
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate
import androidx.compose.runtime.mutableStateMapOf
import com.paulohenriquesg.fahrenheit.podcast.FeedCheck
import com.paulohenriquesg.fahrenheit.podcast.FeedCheckRow
import com.paulohenriquesg.fahrenheit.podcast.FeedCheckState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.book.BookPlayerActivity
import com.paulohenriquesg.fahrenheit.podcast.PlayerActivity
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.MarqueeText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import com.paulohenriquesg.fahrenheit.ui.CardFocus
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

class DetailActivity : ComponentActivity() {
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
        var expanded by remember { mutableStateOf(false) }
        var loadFailed by remember { mutableStateOf(false) }
        var mayCheckFeed by remember { mutableStateOf(false) }

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

        val feedUrl = itemDetail?.media?.metadata?.feedUrl
        LaunchedEffect(feedUrl) {
            val podcastApi = ApiClient.getPodcastApi() ?: return@LaunchedEffect
            mayCheckFeed = FeedCheck(podcastApi).mayCheck(feedUrl)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                CoverImage(
                    itemId = itemId,
                    contentDescription = itemDetail?.media?.metadata?.title ?: "Cover Image"
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.height(200.dp)
                ) {
                    itemDetail?.let {
                        Text(
                            text = it.media.metadata.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val description =
                                it.media.metadata.description ?: "No description available"
                            // Was converted to a plain string, which dropped the
                            // emphasis along with the tags.
                            val annotatedDescription = remember(description) {
                                RichText.fromHtml(description)
                            }
                            Text(
                                text = annotatedDescription,
                                style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (expanded) Int.MAX_VALUE else Int.MAX_VALUE,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (expanded) "View Less" else "View More",
                                color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { expanded = !expanded }
                                )
                            }
                        }
                } ?: Text(text = stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (itemDetail?.mediaType == "book") {
                Button(
                    onClick = {
                        val intent = BookPlayerActivity.createIntent(context, itemId)
                        context.startActivity(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.play_book))
                }
            } else {
                PodcastEpisodes(itemId, itemDetail, mayCheckFeed, onReloaded = { itemDetail = it })
            }
        }
    }

    /**
     * The podcast half of this screen (#76): every episode in the feed for an
     * admin, marked by whether the server has it, and the server's own for
     * anyone else.
     */
    @Composable
    private fun PodcastEpisodes(
        itemId: String,
        itemDetail: LibraryItemResponse?,
        isAdmin: Boolean,
        onReloaded: (LibraryItemResponse) -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var feed by remember { mutableStateOf<FeedLoad>(FeedLoad.Unavailable) }
        var tab by remember { mutableStateOf(EpisodeTab.All) }
        var feedCheck by remember { mutableStateOf<FeedCheckState>(FeedCheckState.Idle) }
        val downloads = remember { mutableStateMapOf<String, DownloadState>() }
        val serverFormat = remember { SharedPreferencesHandler(context).getUserPreferences().dateFormat }
        val media = itemDetail?.media
        val feedUrl = media?.metadata?.feedUrl

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

        if (media == null) return
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
        val reload: suspend () -> LibraryItemResponse? = {
            ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(itemId).getOrNull() }
                ?.also(onReloaded)
        }

        PodcastEpisodesView(
            screen = screen,
            tab = tab,
            onTab = { tab = it },
            downloads = downloads,
            onPlay = { episode ->
                context.startActivity(PlayerActivity.createIntent(context, itemId, episode.id))
            },
            onDownload = { row ->
                val podcastApi = ApiClient.getPodcastApi() ?: return@PodcastEpisodesView
                val episode = row.feed ?: return@PodcastEpisodesView
                scope.launch {
                    PodcastFeed(podcastApi).download(
                        podcastId = itemId,
                        episode = episode,
                        onState = { downloads[row.key] = it },
                        reload = { reload()?.media?.episodes }
                    )
                }
            },
            coverItemId = itemId,
            date = { row -> EpisodeDate.of(row.publishedAt, now, serverFormat) },
            actions = {
                if (isAdmin) {
                    FeedCheckRow(state = feedCheck, onCheck = {
                        val podcastApi = ApiClient.getPodcastApi() ?: return@FeedCheckRow
                        scope.launch {
                            FeedCheck(podcastApi).run(
                                podcastId = itemId,
                                episodesBefore = media.episodes?.size ?: 0,
                                onState = { feedCheck = it },
                                reload = { reload()?.media?.episodes?.size }
                            )
                        }
                    })
                }
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
    }
}