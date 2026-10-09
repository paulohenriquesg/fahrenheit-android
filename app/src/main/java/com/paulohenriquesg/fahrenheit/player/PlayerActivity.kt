package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.favourites.FavouriteButton
import com.paulohenriquesg.fahrenheit.favourites.FavouriteHeart
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.FavouritesChoice
import com.paulohenriquesg.fahrenheit.favourites.HeartChange
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.view.KeyEvent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.async
import retrofit2.awaitResponse

/**
 * The one player, for a book or a podcast episode (#73).
 *
 * Replaces BookPlayerActivity and podcast.PlayerActivity, which shared the
 * transport and differed in three lines of content, now [NowPlaying]. Every
 * fix had to be made twice, and the second copy was nearly missed each time.
 *
 * Playback lives in [PlaybackService]; this screen drives it through a
 * MediaController, connected while it is visible. Back and Home both leave
 * it playing (#155); Stop on the rail's Now playing entry ends it.
 */
class PlayerActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var connectFailed by mutableStateOf(false)
    /** The sleep timer as the service reports it; null when none runs. */
    private var sleep by mutableStateOf<SleepState?>(null)
    private val sleepReports = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            sleep = SleepCommand.state(extras)
        }
    }
    private val connection by lazy {
        ControllerSlot(
            connect = { Playback.connect(this, sleepReports) },
            release = { it.release() },
            executor = ContextCompat.getMainExecutor(this),
            onChange = {
                controller = it
                connectFailed = it == null
                sleep = it?.let { connected -> SleepCommand.state(connected.sessionExtras) }
            }
        )
    }
    private val speeds by lazy { SpeedMemory(this) }
    private lateinit var start: PlayerStart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, false)
        if (itemId == null) {
            Toast.makeText(this, getString(R.string.item_id_is_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // auto_play is a request to start, once: not again when this screen
        // comes back, and not after a configuration change recreates it.
        askThenPlay = asksThenPlays(intent) && savedInstanceState == null
        start = PlayerStart(
            autoPlay = autoPlay && savedInstanceState == null,
            startAt = startAtOf(intent)?.takeIf { savedInstanceState == null },
            resumed = savedInstanceState != null
        )

        setContent {
            FahrenheitTheme {
                Surface(
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Player(itemId, episodeId)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.open()
    }

    // Leaving, by Back or Home, keeps playing (#155): the controller only lets go.
    override fun onStop() {
        connection.close()
        controller = null
        super.onStop()
    }

    @androidx.compose.runtime.Composable
    private fun Player(itemId: String, episodeId: String?) {
        var nowPlaying by remember { mutableStateOf<NowPlaying?>(null) }
        var failed by remember { mutableStateOf(false) }
        var currentTime by remember { mutableDoubleStateOf(0.0) }
        var wash by remember { mutableStateOf<Color?>(null) }
        // The item as loaded, and the episode shown: with auto-advance the
        // queue moves on by itself, and the screen follows it (#108).
        var loaded by remember { mutableStateOf<LibraryItemResponse?>(null) }
        var shownEpisode by remember { mutableStateOf(episodeId) }
        val serverFormat = remember { SharedPreferencesHandler(this@PlayerActivity).getUserPreferences().dateFormat }

        LaunchedEffect(itemId, episodeId) {
            // The cover's colour, worked out alongside the item: the screen
            // waits for it so it does not open black and then change, but
            // only for what is left of WASH_WAIT_MS once the item is here, so
            // it does not delay playing either. A slow one fades in later.
            val started = SystemClock.elapsedRealtime()
            val colour = async { washOrNothing { coverWashOf(coverBitmap(this@PlayerActivity, itemId)) } }
            val api = ApiClient.getLibraryApi()
            val item = api?.let { LibraryRepository(it).item(itemId).getOrNull() }
            loaded = item
            val playing = item?.let { NowPlaying.of(it, episodeId, System.currentTimeMillis(), serverFormat) }
            if (playing != null) {
                wash = washBeforeShowing(
                    waitMs = WASH_WAIT_MS - (SystemClock.elapsedRealtime() - started),
                    colour = colour,
                    late = { wash = it }
                )
            } else {
                colour.cancel()
            }
            nowPlaying = playing
            failed = nowPlaying == null
        }

        // The queue moved on to the next episode: show that one, from the item
        // already loaded - no request, and no "Loading" in between.
        // Keyed on the item too: a move made before it loaded is followed once it has.
        LaunchedEffect(shownEpisode, loaded) {
            if (shownEpisode == nowPlaying?.episodeId) return@LaunchedEffect
            val item = loaded ?: return@LaunchedEffect
            NowPlaying.of(item, shownEpisode, System.currentTimeMillis(), serverFormat)?.let { nowPlaying = it }
        }

        val playing = nowPlaying
        val connected = controller
        DisposableEffect(connected) {
            val follow = object : androidx.media3.common.Player.Listener {
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    shownEpisode = EpisodeFollow.shown(QueuedFile.of(mediaItem), itemId, shownEpisode)
                }
            }
            connected?.addListener(follow)
            onDispose { connected?.removeListener(follow) }
        }
        // Keyed on the item, not the episode: following a move is not a new start.
        var ready by remember(connected, playing?.itemId) { mutableStateOf(false) }
        // Which position to continue from, when the server's is newer and from
        // elsewhere (#90); asked on coming back to a paused item, and on Play.
        val scope = rememberCoroutineScope()
        // Set when the screen came back to this item queued already; asked once the prompt exists.
        var reattached by remember(connected, playing?.itemId, playing?.episodeId) { mutableStateOf(false) }
        // After an answer the question's focused button is gone: Play takes focus.
        var focusPlayAgain by remember { mutableIntStateOf(0) }
        val resumeCheck = remember {
            ResumeCheck(
                progress = ResumeSources::progress,
                latestSession = ResumeSources::latestSession,
                thisDevice = PlaybackDevice.info(this).deviceId.orEmpty()
            )
        }
        val listening = remember(connected, playing) {
            if (connected == null || playing == null) null
            else ListeningControls(connected, itemId, playing.chapters, playing.trackTotal ?: 0.0, speeds, connected::sendCustomCommand)
        }
        LaunchedEffect(connected, playing) {
            if (connected == null || playing == null) return@LaunchedEffect
            // Before anything plays, or the first moments play at the last book's speed.
            listening?.applyRememberedSpeed()
            // The saved position is read when it is needed, not when the
            // screen opened: by then it may have been listened past elsewhere.
            // Coming back after the queue moved on with the screen closed: show
            // the episode playing, and begin again with that (#108).
            start.follows(connected, playing)?.let {
                shownEpisode = it
                return@LaunchedEffect
            }
            // With auto-advance on, the next newer episode follows this one, from
            // where it was left (#108).
            val next = playing.next?.takeIf { playerSettings.playNextEpisode }
                ?.let { ref -> loaded?.let { NowPlaying.of(it, ref.id, System.currentTimeMillis(), serverFormat) } }
            val nextStartAt = next?.let {
                ResumePoint.decide(savedProgress(itemId, it.episodeId), it.trackTotal, it.mediaDuration).positionSeconds
            } ?: 0.0
            val reattaching = QueuedFile.of(connected.currentMediaItem)?.isFor(itemId, playing.episodeId) == true
            ready = start.begin(
                connected,
                playing,
                progress = { savedProgress(itemId, playing.episodeId) },
                resolveUrl = { ApiClient.generateFullUrl(it) },
                next = next,
                nextStartAt = nextStartAt
            )
            if (!ready) failed = true
            // A start chosen on the opening screen is not questioned.
            if (ready && reattaching && !start.choseStart) reattached = true
        }
        // Above the screen's states, so a mark made in this visit survives Home and back.
        var finished by rememberFinished(connected, playing?.finished)
        var marking by remember(playing) { mutableStateOf(false) }
        // A panel left open when the screen went away does not come back over it.
        val panels = rememberPlayerPanels(connected)
        val playback = remember(connected, playing) {
            val timeline = playing?.timeline
            if (connected == null || timeline == null) null else BookPlayback(connected, timeline)
        }
        val spans = remember(playing) { ChapterClock.spans(playing?.chapters, playing?.trackTotal ?: 0.0) }
        // The rest of the series, for About and "Book N of M"; kept apart
        // from nowPlaying, whose change would start playback over.
        var series by remember(playing) { mutableStateOf<SeriesBooks?>(null) }
        LaunchedEffect(playing?.series, playing?.libraryId) {
            val ref = playing?.series ?: return@LaunchedEffect
            val libraryId = playing.libraryId ?: return@LaunchedEffect
            val api = ApiClient.getLibraryApi() ?: return@LaunchedEffect
            // Said in the log when it fails: the row simply not being there hid
            // a reply the app could not read (device check, #130).
            series = LibraryRepository(api).seriesBooks(libraryId, ref.id)
                .onFailure { Log.w(TAG, "Couldn't read the series ${ref.id}", it) }
                .getOrNull()
                ?.let { SeriesBooks.of(it, currentId = itemId) }
                ?.also { if (it.current == null) Log.w(TAG, "The series ${ref.id} does not list $itemId") }
                ?.takeIf { it.current != null }
        }
        // The heart, while the library has a Favourites playlist (#180).
        val heart = remember(playing?.libraryId, playing?.itemId, playing?.episodeId) {
            val shown = playing
            val libraryId = shown?.libraryId
            val kept = ApiClient.getPlaylistApi()?.let { Favourites(it, FavouritesChoice(this@PlayerActivity)) }
            if (shown == null || libraryId == null || kept == null) null
            else FavouriteHeart(kept, libraryId, shown.itemId, shown.episodeId)
        }
        LaunchedEffect(heart) { heart?.load() }
        // Keyed on the episode too: an answer must not seek one episode to another's position.
        val prompt = remember(connected, playing?.itemId, playing?.episodeId, playback) {
            val player = connected
            val shown = playing
            val at = playback
            if (player == null || shown == null || at == null) null
            else ResumePrompt(
                scope = scope,
                check = { resumeCheck.offer(itemId, shown.episodeId, at.bookPosition(), playing = false) },
                playWhenReady = { player.playWhenReady },
                play = { player.play() },
                seek = { at.seekToBookTime(it) },
                answered = {
                    resumeCheck.answered(itemId, shown.episodeId, it)
                    focusPlayAgain++
                }
            )
        }
        LaunchedEffect(prompt, reattached) {
            if (!reattached) return@LaunchedEffect
            reattached = false
            prompt?.onReattach(playAfter = askThenPlay)
            // The press it was opened for is spent.
            askThenPlay = false
        }
        // The remote's Play reaches the screen before the media session: from a
        // pause it asks first too, and while the question shows it waits.
        DisposableEffect(prompt) {
            remotePlay = { down -> prompt?.onKey(down) ?: false }
            onDispose { remotePlay = null }
        }
        when {
            failed || connectFailed -> Text(
                text = stringResource(R.string.item_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            playing == null || connected == null || !ready -> Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
            else -> {
                PlayerScreen(
                    nowPlaying = series?.let { playing.withSeriesTotal(it.total) } ?: playing,
                    currentTime = currentTime,
                    wash = wash,
                    transport = {
                        // ready implies a timeline: PlayerStart refuses a NowPlaying without one.
                        val timeline = playing.timeline
                        if (timeline != null && playback != null) {
                            MediaPlayerController(
                                player = connected,
                                playback = playback,
                                totalTime = timeline.totalDuration,
                                chapters = playing.chapters,
                                // The lengths set in Settings (#107), as the remote's keys use them. A plain
                                // read is enough: Settings is reached only by leaving the player.
                                skipBack = playerSettings.skipBackSeconds,
                                skipForward = playerSettings.skipForwardSeconds,
                                onCurrentTimeUpdate = { currentTime = it },
                                onPlay = { if (prompt != null) prompt.onPlay() else connected.play() },
                                focusPlayAgain = focusPlayAgain,
                                // An episode's outer buttons open the episodes either side (#108).
                                episodes = playing.episodeId?.let {
                                    EpisodeSkip(
                                        onPrevious = playing.previous?.let { other -> { goToEpisode(connected, other.id) } },
                                        onNext = playing.next?.let { other -> { goToEpisode(connected, other.id) } }
                                    )
                                },
                                trailing = {
                                    PlayerActions(
                                        panels = panels,
                                        speed = rememberPlaybackSpeed(connected),
                                        sleep = sleep,
                                        chapters = spans.isNotEmpty(),
                                        onGoToPodcast = if (playing.goToPodcast) ({ goToPodcast(itemId) }) else null,
                                        favourite = heart?.playlist?.let { playlist ->
                                            {
                                                FavouriteButton(
                                                    filled = heart.filled,
                                                    playlist = playlist.name,
                                                    onClick = { scope.launch { heart.toggle()?.let(::confirm) } }
                                                )
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    },
                    overlay = {
                        prompt?.offer?.let { asked ->
                            ResumeChoice(
                                asked,
                                item = ResumeItem(
                                    itemId = playing.itemId,
                                    title = playing.title,
                                    length = playing.trackTotal ?: 0.0,
                                    chapters = spans,
                                    episode = playing.episodeId != null
                                ),
                                now = System.currentTimeMillis(),
                                onContinue = { prompt.answer(moveThere = true) },
                                onStay = { prompt.answer(moveThere = false) }
                            )
                        }
                        PlayerPanelHost(panels) { panel ->
                            when (panel) {
                                PlayerPanel.Chapters -> ChaptersPanel(
                                    spans = spans,
                                    at = playback?.bookPosition() ?: currentTime,
                                    onChoose = { playback?.seekToBookTime(it) },
                                    onClose = panels::close
                                )
                                PlayerPanel.About -> {
                                    val playInstead: (SeriesBook) -> Unit = { other ->
                                        switchTo(this@PlayerActivity, connected, other.itemId)
                                    }
                                    val mark: (Boolean) -> Unit = { done ->
                                        // The service marks it, once the closing report is in (FinishMarker).
                                        marking = true
                                        listening?.markFinished(done) { worked ->
                                            marking = false
                                            if (worked) finished = done
                                            else Toast.makeText(this@PlayerActivity, getString(R.string.mark_finished_failed), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    // A book's and an episode's About alike: the one book layout over the wash (#134, #178).
                                    AboutScreen(
                                        nowPlaying = playing, wash = wash, series = series, finished = finished, marking = marking,
                                        onPlayInstead = playInstead, onMarkFinished = mark, onClose = panels::close
                                    )
                                }
                                PlayerPanel.Speed -> SpeedPanel(
                                    current = rememberPlaybackSpeed(connected),
                                    onChoose = { listening?.chooseSpeed(it) },
                                    onClose = panels::close,
                                    forShow = playing.goToPodcast
                                )
                                PlayerPanel.Sleep -> SleepPanel(
                                    sleep = sleep,
                                    chapters = listening?.hasChapters == true,
                                    onChoose = { listening?.chooseSleep(it) },
                                    onClose = panels::close
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    /** Opened by a Play from outside the player: ask, then play (#144). Once. */
    private var askThenPlay = false

    /** Set while the player screen shows: takes the remote's Play from a pause (#90). */
    private var remotePlay: ((down: Boolean) -> Boolean)? = null

    // Keys the screen leaves alone come here before the window hands media keys
    // to the media session, so Play can ask first.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        (isPlayKey(keyCode) && remotePlay?.invoke(true) == true) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        (isPlayKey(keyCode) && remotePlay?.invoke(false) == true) || super.onKeyUp(keyCode, event)

    private fun isPlayKey(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY

    private fun goToPodcast(podcastId: String) = leaveForPodcast(this, podcastId)

    /** The short note after the heart (frame 1). */
    private fun confirm(change: HeartChange) {
        val text = when (change) {
            is HeartChange.Added -> getString(R.string.favourite_added, change.playlist)
            is HeartChange.Removed -> getString(R.string.favourite_removed, change.playlist)
            HeartChange.Failed -> getString(R.string.favourites_change_failed)
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private val playerSettings by lazy { PlayerSettings(this) }

    /** Previous or Next episode: this one stops, and the other opens, playing (see [switchTo]). */
    private fun goToEpisode(controller: androidx.media3.common.Player, episodeId: String) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        switchTo(this, controller, itemId, episodeId)
    }

    /**
     * Where the listener left off; null starts from the beginning. When the
     * server could not be read that is said, since playing on will save the
     * new position over whatever it holds.
     */
    private suspend fun savedProgress(itemId: String, episodeId: String?): MediaProgressResponse? {
        val api = ApiClient.getApiService() ?: return null
        return SavedProgress.read(episodeId) {
            val call = if (episodeId != null) api.userGetMediaProgress(itemId, episodeId) else api.userGetMediaProgress(itemId)
            call.awaitResponse()
        }.progressOr {
            Toast.makeText(this, getString(R.string.progress_unreadable), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "PlayerActivity"
        private const val EXTRA_ITEM_ID = "item_id"

        /** How long the player waits for its cover's colour before it shows anyway. */
        private const val WASH_WAIT_MS = 1_500L
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_AUTO_PLAY = "auto_play"
        private const val EXTRA_START_AT = "start_at"
        private const val EXTRA_ASK_THEN_PLAY = "ask_then_play"

        /**
         * @param episodeId the episode to play, for a podcast; null for a book.
         * @param startAt where to start, in whole-book seconds, over the saved position (#105).
         */
        /** Opened by a Play from outside the player (#144): ask, then play. */
        fun asksThenPlays(intent: Intent): Boolean = intent.getBooleanExtra(EXTRA_ASK_THEN_PLAY, false)

        fun createIntent(
            context: Context,
            itemId: String,
            episodeId: String? = null,
            autoPlay: Boolean = false,
            startAt: Double? = null,
            askThenPlay: Boolean = false
        ): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
                episodeId?.let { putExtra(EXTRA_EPISODE_ID, it) }
                putExtra(EXTRA_AUTO_PLAY, autoPlay)
                startAt?.let { putExtra(EXTRA_START_AT, it) }
                if (askThenPlay) putExtra(EXTRA_ASK_THEN_PLAY, true)
            }

        /**
         * "Go to podcast". Clears back to the podcast's screen when the player
         * was opened from it, rather than stacking a second copy on top.
         */
        fun podcastIntent(context: Context, podcastId: String): Intent =
            DetailActivity.createIntent(context, podcastId).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** The start position the intent asks for (see [PlayerStart]); null for the saved one. */
        internal fun startAtOf(intent: Intent): Double? =
            if (intent.hasExtra(EXTRA_START_AT)) intent.getDoubleExtra(EXTRA_START_AT, 0.0) else null

        /**
         * About's "Play <title> instead?", or Previous and Next episode
         * (#108): this book or episode stops - its closing
         * report going to it - before the other opens and plays.
         */
        internal fun switchTo(player: android.app.Activity, controller: androidx.media3.common.Player, itemId: String, episodeId: String? = null) {
            Playback.end(controller)
            player.startActivity(createIntent(player, itemId, episodeId, autoPlay = true))
            player.finish()
        }

        /**
         * "Go to podcast" leaves the player for the podcast's screen; the
         * episode plays on, as it does after Back (#155).
         */
        internal fun leaveForPodcast(player: android.app.Activity, podcastId: String) {
            player.startActivity(podcastIntent(player, podcastId))
            player.finish()
        }
    }
}
