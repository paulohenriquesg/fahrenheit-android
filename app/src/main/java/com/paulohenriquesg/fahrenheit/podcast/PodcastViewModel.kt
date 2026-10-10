package com.paulohenriquesg.fahrenheit.podcast

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulohenriquesg.fahrenheit.api.DownloadQueue
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.PodcastApi
import com.paulohenriquesg.fahrenheit.api.PodcastSettingsApi
import com.paulohenriquesg.fahrenheit.detail.DetailHeaderModel
import com.paulohenriquesg.fahrenheit.favourites.HeartChange
import com.paulohenriquesg.fahrenheit.favourites.LibraryFavourites
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the podcast page shows (#205), all of it; see [PodcastViewModel]. */
data class PodcastUiState(
    val itemId: String,
    val title: String,
    val byline: String,
    val description: String?,
    /** The primary button's label, or null when there is nothing to play. */
    val primary: String?,
    /** What the primary button plays: the episode to resume, or the newest. */
    val primaryEpisode: Episode?,
    /** The facts at the foot of the left column, the tabs, the rows and the note. */
    val screen: PodcastScreen,
    val tab: EpisodeTab = EpisodeTab.All,
    val progress: Map<String, EpisodeProgress> = emptyMap(),
    val downloads: Map<String, DownloadState> = emptyMap(),
    /** The feed check, for whoever may run it; null for anyone else. */
    val feedCheck: FeedCheckState? = null,
    /** The show's own download settings, for whoever may change them (#182); null for anyone else. */
    val autoDownloads: DownloadSettings? = null,
    val downloadsFailed: Boolean = false,
    /** A mark the server refused, to be said once ([PodcastViewModel.markFailureShown]). */
    val markFailed: Boolean = false,
    /** The library's Favourites playlist, for the hearts (#180); null for None. */
    val favouritesPlaylist: String? = null,
    /** This show's episodes in it. */
    val favourites: Set<String> = emptySet(),
    /** What the last heart did, to be said once ([PodcastViewModel.heartChangeShown]). */
    val heartChange: HeartChange? = null
)

/**
 * The podcast page's state (#208): the show, what the user has heard of it
 * (the shared progress store, #207), the feed for an
 * admin, the download queue, the feed check, marks, the library's Favourites
 * and the show's download settings. The page is a function of [state]; its events are the methods here.
 *
 * @param reloadItem the show read again, as its episodes land; null on failure.
 * @param markFinished through the playback service, so the episode playing is
 *   paused and reported first (#181); whether the server took it.
 * @param scope where the work runs; the ViewModel's own unless a test gives one.
 */
class PodcastViewModel(
    private val itemId: String,
    item: LibraryItemResponse,
    private val podcastApi: PodcastApi?,
    settingsApi: PodcastSettingsApi?,
    private val reloadItem: suspend () -> LibraryItemResponse?,
    private val markFinished: suspend (episodeId: String, finished: Boolean, keepAt: Double?) -> Boolean,
    private val now: Long,
    private val serverFormat: String?,
    private val favourites: LibraryFavourites? = null,
    private val progressStore: ProgressStore = ProgressStore.process,
    /** For the download window (#215): one that does not jump when the device sets its time. */
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    /** Between polls of the download queue; a test moves its clock instead of waiting. */
    private val watchPause: (suspend () -> Unit)? = null,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val scope = scope ?: viewModelScope
    private var item = item
    private var me: Me? = null
    /** The server has been asked who this is, whatever it answered. */
    private var asked = false
    /** The feed is read, and the queue watched, once an admin is known: once. */
    private var feedAsked = false
    private var feed: FeedLoad = FeedLoad.Unavailable
    private val tabs = EpisodeTabChoice()
    private var heartChange: HeartChange? = null
    private var feedCheck: FeedCheckState = FeedCheckState.Idle
    private var queue = DownloadQueue(null, emptyList())
    private var requests: Map<String, DownloadRequest> = emptyMap()
    private var markFailed = false
    private val marking = EpisodeMarking()
    private val autoDownloads = settingsApi?.let { PodcastDownloads(itemId, DownloadSettings.of(item.media), it) }
    private val watch = podcastApi?.let { DownloadWatch(it, item.libraryId, itemId, clock) }
    private var watching = false

    private val _state = MutableStateFlow(build())
    val state: StateFlow<PodcastUiState> = _state.asStateFlow()

    init {
        // What has been heard is the shared store's (#207): the player's
        // reports and marks, and the server's own copy, all land there.
        this.scope.launch {
            progressStore.entries.collect {
                // Each change is the server's word: playing a finished episode un-finishes it there.
                marking.settle()
                publish()
            }
        }
        refresh()
    }

    private val isAdmin get() = FeedCheck.mayCheck(me?.type, item.media.metadata.feedUrl)

    /**
     * Reads who the user is again, for the admin's actions and the download
     * settings, and the Favourites playlist: on open, and on coming back.
     */
    fun refresh() {
        val api = podcastApi ?: return
        scope.launch {
            val since = progressStore.generation
            try {
                me = api.me().also { progressStore.readMe(it, since) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline: what was read before stands.
            }
            asked = true
            // The playlist too: a heart changed in the player shows here (#180).
            favourites?.let {
                it.load()
                tabs.follow(it.episodesOf(itemId))
            }
            publish()
            if (isAdmin && !feedAsked) {
                feedAsked = true
                // Episodes queued elsewhere show where they are.
                startWatch()
                readFeed()
            }
        }
    }

    fun chooseTab(tab: EpisodeTab) {
        tabs.choose(tab, favourites?.episodesOf(itemId))
        publish()
    }

    /** Marks an episode finished or not: shown at once, put back if the server refuses. */
    fun mark(episode: Episode, finished: Boolean) {
        val keepAt = if (finished) null else EpisodeMarks.keepAt(progressStore.of(itemId, episode.id))
        scope.launch {
            val worked = marking.mark(episode.id, finished) {
                publish()
                markFinished(episode.id, finished, keepAt)
            }
            when (worked) {
                // The service put the mark in the store, which the rows and Resume read (#207).
                true -> marking.settle()
                false -> markFailed = true
                null -> Unit
            }
            publish()
        }
    }

    /** Adds the episode to the library's Favourites, or takes it out (#180). */
    fun toggleFavourite(episode: Episode) {
        val favourites = favourites ?: return
        scope.launch {
            // Null for a press while another is on its way: that one's note will say.
            favourites.toggle(itemId, episode.id)?.let { heartChange = it }
            tabs.follow(favourites.episodesOf(itemId))
            publish()
        }
    }

    fun heartChangeShown() {
        heartChange = null
        publish()
    }

    fun markFailureShown() {
        markFailed = false
        publish()
    }

    /** Asks the server to download a feed episode it does not have. */
    fun download(row: EpisodeRow) {
        val episode = row.feed ?: return
        val watch = watch ?: return
        requests = requests + (row.key to DownloadRequest.Asked(clock()))
        publish()
        scope.launch {
            if (watch.request(row.key, episode)) {
                startWatch()
            } else {
                requests = requests + (row.key to DownloadRequest.Refused)
                publish()
            }
        }
    }

    fun checkFeed() {
        val api = podcastApi ?: return
        if (feedCheck == FeedCheckState.Checking) return
        scope.launch {
            FeedCheck(api).run(
                podcastId = itemId,
                episodesBefore = item.media.episodes?.size ?: 0,
                onState = {
                    feedCheck = it
                    publish()
                    // What it found is queued; watch it arrive in the list.
                    if (it is FeedCheckState.Found && it.count > 0) startWatch()
                },
                reload = { reload()?.media?.episodes?.size }
            )
        }
    }

    fun changeDownloads(change: DownloadChange) {
        val downloads = autoDownloads ?: return
        // The choice shows at once: the change sets it before it is sent.
        scope.launch {
            downloads.change(change)
            publish()
        }
        publish()
    }

    /** The panel was closed: an old failure is not news when it opens again. */
    fun downloadsSeen() {
        autoDownloads?.seen()
        publish()
    }

    private suspend fun readFeed() {
        val api = podcastApi ?: return
        val url = item.media.metadata.feedUrl?.takeIf { it.isNotBlank() } ?: return
        feed = FeedLoad.Loading
        publish()
        feed = PodcastFeed(api).episodes(url).fold(onSuccess = { FeedLoad.Loaded(it) }, onFailure = { FeedLoad.Failed })
        publish()
    }

    private suspend fun reload(): LibraryItemResponse? =
        reloadItem()?.also {
            item = it
            publish()
        }

    // One watcher at a time; it stops once nothing here is queued or
    // awaited, and starts again on the next download.
    private fun startWatch() {
        val watch = watch ?: return
        if (watching) return
        watching = true
        scope.launch {
            try {
                do {
                    watch.watch(
                        onUpdate = { q, asked ->
                            queue = q
                            // Over what this screen asked, not instead of it: a refusal is
                            // its own to remember, and a request still on its way not yet the watch's.
                            requests = requests + asked
                            publish()
                        },
                        pause = watchPause ?: DownloadWatch.pollPause,
                        reload = { reload()?.media?.episodes }
                    )
                } while (watch.waiting)
            } finally {
                watching = false
            }
        }
    }

    private fun publish() {
        _state.value = build()
    }

    private fun build(): PodcastUiState {
        val media = item.media
        val episodes = media.episodes.orEmpty()
        val settings = autoDownloads?.settings
        val mayChangeDownloads = settings != null && DownloadSettings.mayChange(me)
        val inFavourites = favourites?.episodesOf(itemId)
        val screen = PodcastScreenModel.of(
            server = episodes,
            feed = feed,
            tab = tabs.tab,
            lastEpisodeCheck = media.lastEpisodeCheck,
            // The button says it when there is one (#205); a fact says it otherwise,
            // once it is known there will be no button.
            autoDownload = if (mayChangeDownloads || !asked) null else media.autoDownloadEpisodes,
            schedule = media.autoDownloadSchedule,
            now = now,
            serverFormat = serverFormat,
            favourites = inFavourites,
            favouritesKept = tabs.kept
        )
        val progress = progressStore.entries.value.values.toList()
        val resume = EpisodeProgress.resumable(progress, itemId, onServer = episodes.map { it.id }.toSet())
            ?.let { id -> episodes.firstOrNull { it.id == id } }
        val header = DetailHeaderModel.podcast(item, screen.facts, resumeTitle = resume?.title)
        return PodcastUiState(
            itemId = itemId,
            title = header.title,
            byline = header.byline.orEmpty(),
            description = header.description,
            primary = header.primary,
            primaryEpisode = resume ?: EpisodeOrder.newestFirst(episodes).firstOrNull(),
            screen = screen,
            tab = tabs.tab,
            progress = EpisodeMarks.over(EpisodeProgress.index(progress, itemId), marking.marks),
            downloads = screen.rows.mapNotNull { row ->
                DownloadProgress.state(row, queue, requests[row.key], clock())?.let { row.key to it }
            }.toMap(),
            feedCheck = feedCheck.takeIf { isAdmin },
            autoDownloads = settings.takeIf { mayChangeDownloads },
            downloadsFailed = autoDownloads?.failed == true,
            markFailed = markFailed,
            favouritesPlaylist = favourites?.playlist?.name,
            favourites = inFavourites.orEmpty(),
            heartChange = heartChange
        )
    }
}
