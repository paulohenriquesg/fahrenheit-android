package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.favourites.FavouriteHeart
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.FavouritesChoice
import com.paulohenriquesg.fahrenheit.favourites.HeartChange
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The heart among the player's actions (#180): there while the library has a Favourites playlist. */
data class FavouriteState(val playlist: String, val filled: Boolean)

/** Something the player screen says once, or does: a toast, or closing. */
sealed interface PlayerNotice {
    /** The saved position could not be read: it starts from the beginning, and saving will write over it. */
    data object ProgressUnreadable : PlayerNotice
    data object MarkFinishedFailed : PlayerNotice
    /** What a press of the heart did. */
    data class Favourite(val change: HeartChange) : PlayerNotice
    /** The queue played to its end with the screen open (#179): it closes. */
    data object QueueEnded : PlayerNotice
}

/**
 * What the player screen shows, as one value (#221).
 *
 * @property nowPlaying the book or episode shown; with auto-advance it follows
 *   the queue on to the next episode (#108).
 * @property failed the item could not be loaded, or there is nothing to play.
 * @property connectFailed the playback service could not be reached.
 * @property ready the player holds what is shown, queued or reattached to.
 * @property player the connected player, while the screen is visible; the
 *   transport reads its own position and speed from it.
 * @property playback [player] in whole-book time, for what is shown.
 * @property currentTime where the transport last said playback is.
 * @property sleep the service's sleep timer; null when none runs.
 * @property series the rest of the series, for About and "Book N of M".
 * @property finished whether the book is finished; null for an episode.
 * @property marking a Mark finished on its way to the service.
 * @property offer the question of where to continue from (#90), while asked.
 */
data class PlayerUiState(
    val nowPlaying: NowPlaying? = null,
    val failed: Boolean = false,
    val connectFailed: Boolean = false,
    val ready: Boolean = false,
    val wash: Color? = null,
    val player: Player? = null,
    val playback: BookPlayback? = null,
    val currentTime: Double = 0.0,
    val sleep: SleepState? = null,
    val series: SeriesBooks? = null,
    val finished: Boolean? = null,
    val marking: Boolean = false,
    val favourite: FavouriteState? = null,
    val offer: ResumeOffer? = null
)

/**
 * The player screen's state and wiring, out of the Activity (#221): loading
 * the item, queueing it or reattaching through [PlayerStart], following the
 * queue to the next episode, asking where to continue ([ResumePrompt]), Mark
 * finished, the sleep timer, the series and the heart.
 *
 * Playback stays in [PlaybackService]: the screen hands over the connected
 * controller as a [PlayerLink] on [connect] and takes it back on
 * [disconnect]; nothing here builds or releases a player. What is read, and
 * when, is as it was: the saved position on each connect, just before
 * queueing (#90), never kept from an earlier one.
 *
 * @param start what this screen does with the player each time it connects.
 * @param askThenPlay opened by a Play from outside the player (#144): asked
 *   on reattaching, then played. Once.
 * @param item the item, or null when it cannot be loaded.
 * @param savedProgress the server's saved position for an item or episode.
 * @param seriesBooks a series' books, or null when there is no server to ask.
 * @param heartOf the heart for a library's item, or null with no server.
 * @param serverFormat the account's date format, for an episode's date.
 * @param clock elapsed time, for how long the cover's colour is waited for.
 * @param scope where the work runs; the ViewModel's own, posted to the main
 *   thread, unless a test gives one, which that test then cancels: [onCleared] does not.
 */
class PlayerViewModel(
    private val itemId: String,
    episodeId: String?,
    private val start: PlayerStart,
    private var askThenPlay: Boolean,
    private val item: suspend (String) -> LibraryItemResponse?,
    private val savedProgress: suspend (itemId: String, episodeId: String?) -> SavedProgress,
    private val resumeCheck: ResumeCheck,
    private val speeds: SpeedMemory,
    private val resolveUrl: (String) -> String?,
    private val playNextEpisode: () -> Boolean = { false },
    private val coverWash: suspend (String) -> Color? = { null },
    private val seriesBooks: suspend (libraryId: String, seriesId: String) -> Result<List<LibraryItem>>? = { _, _ -> null },
    private val heartOf: (libraryId: String, itemId: String, episodeId: String?) -> FavouriteHeart? = { _, _, _ -> null },
    private val serverFormat: String? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    scope: CoroutineScope? = null
) : ViewModel() {

    // Posted, never run in place, as the screen's effects were: a begin set off
    // by the controller's own callback (the queue moving on) runs after it,
    // not inside it, and the item's load starts after onCreate.
    private val scope = scope ?: CoroutineScope(viewModelScope.coroutineContext + Dispatchers.Main)
    private val state = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = state.asStateFlow()

    private val said = Channel<PlayerNotice>(Channel.BUFFERED)

    /** Toasts to show and the queue's end, each once. */
    val notices: Flow<PlayerNotice> = said.receiveAsFlow()

    /** The item as loaded: a move to the next episode is shown from it, with no request. */
    private var loaded: LibraryItemResponse? = null

    /** The episode shown: with auto-advance the queue moves on by itself, and the screen follows it (#108). */
    private var shownEpisode: String? = episodeId

    // Each connection's, rebuilt for each item shown.
    private var link: PlayerLink? = null
    private var listening: ListeningControls? = null
    private var prompt: ResumePrompt? = null
    private var beginning: Job? = null
    private var listeners: List<Player.Listener> = emptyList()
    private var queueEnd: QueueEndWatch? = null

    // What is read for the item shown, keyed as the screen keyed it.
    private var seriesKey: Pair<SeriesRef?, String?>? = null
    private var seriesRead: Job? = null
    private var heartKey: Triple<String?, String?, String?>? = null
    private var heart: FavouriteHeart? = null
    private var heartRead: Job? = null

    init {
        this.scope.launch { load() }
    }

    private suspend fun CoroutineScope.load() {
        // The cover's colour, worked out alongside the item: the screen
        // waits for it so it does not open black and then change, but only
        // for what is left of WASH_WAIT_MS once the item is here, so it does
        // not delay playing either. A slow one fades in later.
        val started = clock()
        val colour = async { washOrNothing { coverWash(itemId) } }
        val found = item(itemId)
        loaded = found
        // An episode is shown from the item at once, as the screen's
        // follow did on the item arriving; a book waits for its colour.
        followShown()
        val playing = found?.let { NowPlaying.of(it, shownEpisode, now(), serverFormat) }
        val wash = if (playing != null) {
            washBeforeShowing(
                waitMs = WASH_WAIT_MS - (clock() - started),
                colour = colour,
                late = { late -> state.update { it.copy(wash = late) } }
            )
        } else {
            colour.cancel()
            null
        }
        state.update { it.copy(wash = wash) }
        show(playing)
        state.update { it.copy(failed = playing == null) }
    }

    /** The queue moved on to another episode of this show: show that one, from the item already loaded. */
    private fun follow(episode: String?) {
        if (episode == shownEpisode) return
        shownEpisode = episode
        watchEnd()
        followShown()
    }

    private fun followShown() {
        if (shownEpisode == state.value.nowPlaying?.episodeId) return
        val found = loaded ?: return
        NowPlaying.of(found, shownEpisode, now(), serverFormat)?.let(::show)
    }

    /**
     * A new book or episode to show. The same one again is no change: the
     * screen's state compared by value, and a restart would begin again.
     */
    private fun show(playing: NowPlaying?) {
        val was = state.value.nowPlaying
        if (playing == was) return
        state.update {
            it.copy(
                nowPlaying = playing,
                series = null,
                marking = false,
                // Only new item facts reset it; a mark made in this visit survives Home and back.
                finished = if (playing?.finished != was?.finished) playing?.finished else it.finished
            )
        }
        readSeries(playing)
        readHeart(playing)
        rebuild()
    }

    /**
     * The player connected ([link]), or could not be reached (null). Begins:
     * queues what is shown at the saved position, or reattaches to it.
     */
    fun connect(link: PlayerLink?) {
        letGo()
        this.link = link
        state.update {
            it.copy(
                player = link?.player,
                connectFailed = link == null,
                ready = false,
                sleep = link?.let { connected -> SleepCommand.state(connected.sessionExtras) }
            )
        }
        link?.player?.let(::listen)
        rebuild()
    }

    /** Leaving, by Back or Home, keeps playing (#155): the screen only lets go. */
    fun disconnect() {
        letGo()
        link = null
        state.update { it.copy(player = null, ready = false) }
        rebuild()
    }

    /** The service's extras changed: the sleep timer. */
    fun extrasChanged(extras: Bundle) {
        state.update { it.copy(sleep = SleepCommand.state(extras)) }
    }

    fun timeUpdated(seconds: Double) {
        state.update { it.copy(currentTime = seconds) }
    }

    /** Play from a pause: asks first where to continue from, when there is a question (#90). */
    fun play() {
        val asking = prompt
        if (asking != null) asking.onPlay() else link?.player?.play()
    }

    /**
     * The remote's Play, which reaches the screen before the media session:
     * from a pause it asks first too, and while the question shows it waits.
     *
     * @return whether it was taken here.
     */
    fun remotePlay(down: Boolean): Boolean = prompt?.onKey(down) ?: false

    /** @return whether there was a question to answer, so focus goes back to Play only after one. */
    fun answerResume(moveThere: Boolean): Boolean {
        val asking = prompt?.takeIf { it.offer != null } ?: return false
        asking.answer(moveThere)
        return true
    }

    /** The service marks it, once the closing report is in ([FinishMarker]). */
    fun markFinished(done: Boolean) {
        state.update { it.copy(marking = true) }
        listening?.markFinished(done) { worked ->
            state.update { it.copy(marking = false, finished = if (worked) done else it.finished) }
            if (!worked) said.trySend(PlayerNotice.MarkFinishedFailed)
        }
    }

    fun toggleFavourite() {
        val pressed = heart ?: return
        scope.launch {
            val change = pressed.toggle()
            publishHeart()
            change?.let { said.trySend(PlayerNotice.Favourite(it)) }
        }
    }

    fun chooseSpeed(speed: Float) {
        listening?.chooseSpeed(speed)
    }

    fun chooseSleep(choice: SleepChoice) {
        listening?.chooseSleep(choice)
    }

    /** What this connection and the item shown are driven through; begun again for either change. */
    private fun rebuild() {
        beginning?.cancel()
        val connected = link
        val playing = state.value.nowPlaying
        listening = if (connected == null || playing == null) null
        else ListeningControls(connected.player, itemId, playing.chapters, playing.trackTotal ?: 0.0, speeds, playing.episodeId, connected::send)
        val timeline = playing?.timeline
        val playback = if (connected == null || timeline == null) null else BookPlayback(connected.player, timeline)
        prompt = if (connected == null || playing == null || playback == null) null else promptFor(connected.player, playing, playback)
        state.update { it.copy(playback = playback, offer = null) }
        if (connected == null || playing == null) return
        beginning = scope.launch { begin(connected, playing) }
    }

    // Keyed on the episode: an answer must not seek one episode to another's position.
    private fun promptFor(player: Player, shown: NowPlaying, at: BookPlayback): ResumePrompt {
        lateinit var made: ResumePrompt
        made = ResumePrompt(
            scope = scope,
            check = { resumeCheck.offer(itemId, shown.episodeId, at.bookPosition(), playing = false) },
            playWhenReady = { player.playWhenReady },
            play = { player.play() },
            seek = { at.seekToBookTime(it) },
            answered = { resumeCheck.answered(itemId, shown.episodeId, it) },
            // An older connection's question, answering late, is not this one's.
            shown = { offer -> if (prompt === made) state.update { it.copy(offer = offer) } }
        )
        return made
    }

    private suspend fun begin(connected: PlayerLink, playing: NowPlaying) {
        // Before anything plays, or the first moments play at the last book's speed.
        listening?.applyRememberedSpeed()
        // Coming back after the queue moved on with the screen closed: show
        // the episode playing, and begin again with that (#108).
        start.follows(connected.player, playing)?.let {
            follow(it)
            return
        }
        // With auto-advance on, the next newer episode follows this one, from where it was left (#108).
        val next = playing.next?.takeIf { playNextEpisode() }
            ?.let { ref -> loaded?.let { NowPlaying.of(it, ref.id, now(), serverFormat) } }
        val nextStartAt = next?.let {
            ResumePoint.decide(progress(itemId, it.episodeId), it.trackTotal, it.mediaDuration).positionSeconds
        } ?: 0.0
        val reattaching = QueuedFile.of(connected.player.currentMediaItem)?.isFor(itemId, playing.episodeId) == true
        // The saved position is read when it is needed, not when the screen
        // opened: by then it may have been listened past elsewhere.
        val ready = start.begin(
            connected.player,
            playing,
            progress = { progress(itemId, playing.episodeId) },
            resolveUrl = resolveUrl,
            next = next,
            nextStartAt = nextStartAt
        )
        state.update { it.copy(ready = ready, failed = it.failed || !ready) }
        // A start chosen on the opening screen is not questioned.
        if (ready && reattaching && !start.choseStart) {
            prompt?.onReattach(playAfter = askThenPlay)
            // The press it was opened for is spent.
            askThenPlay = false
        }
    }

    /**
     * Where the listener left off; null starts from the beginning. When the
     * server could not be read that is said, since playing on will save the
     * new position over whatever it holds.
     */
    private suspend fun progress(itemId: String, episodeId: String?): MediaProgressResponse? =
        savedProgress(itemId, episodeId).progressOr { said.trySend(PlayerNotice.ProgressUnreadable) }

    private fun listen(player: Player) {
        val follows = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                follow(EpisodeFollow.shown(QueuedFile.of(mediaItem), itemId, shownEpisode))
            }
        }
        // Playing a finished book moves its position, and the server un-finishes it (#107).
        val unfinishes = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && state.value.finished == true) state.update { it.copy(finished = false) }
            }
        }
        listeners = listOf(follows, unfinishes)
        listeners.forEach(player::addListener)
        watchEnd()
    }

    // Played to the end: nothing left to show (#179). Rebuilt for the episode
    // shown, which follows auto-advance, so the end is the last one's.
    private fun watchEnd() {
        val player = link?.player ?: return
        queueEnd?.let(player::removeListener)
        queueEnd = QueueEndWatch(player, itemId, shownEpisode) { said.trySend(PlayerNotice.QueueEnded) }
            .also(player::addListener)
    }

    private fun letGo() {
        val player = link?.player ?: return
        listeners.forEach(player::removeListener)
        listeners = emptyList()
        queueEnd?.let(player::removeListener)
        queueEnd = null
    }

    // The rest of the series, for About and "Book N of M"; kept apart from
    // what is shown, whose change would start playback over.
    private fun readSeries(playing: NowPlaying?) {
        val key = playing?.series to playing?.libraryId
        if (key == seriesKey) return
        seriesKey = key
        seriesRead?.cancel()
        val ref = playing?.series ?: return
        val libraryId = playing.libraryId ?: return
        seriesRead = scope.launch {
            // Said in the log when it fails: the row simply not being there hid
            // a reply the app could not read (device check, #130).
            val books = seriesBooks(libraryId, ref.id)
                ?.onFailure { Log.w(TAG, "Couldn't read the series ${ref.id}", it) }
                ?.getOrNull()
                ?.let { SeriesBooks.of(it, currentId = itemId) }
                ?.also { if (it.current == null) Log.w(TAG, "The series ${ref.id} does not list $itemId") }
                ?.takeIf { it.current != null }
            state.update { it.copy(series = books) }
        }
    }

    // The heart, while the library has a Favourites playlist (#180).
    private fun readHeart(playing: NowPlaying?) {
        val key = Triple(playing?.libraryId, playing?.itemId, playing?.episodeId)
        if (key == heartKey) return
        heartKey = key
        heartRead?.cancel()
        val libraryId = playing?.libraryId
        heart = if (playing == null || libraryId == null) null else heartOf(libraryId, playing.itemId, playing.episodeId)
        publishHeart()
        val shown = heart ?: return
        heartRead = scope.launch {
            shown.load()
            publishHeart()
        }
    }

    private fun publishHeart() {
        val shown = heart
        state.update { it.copy(favourite = shown?.playlist?.let { playlist -> FavouriteState(playlist.name, shown.filled) }) }
    }

    override fun onCleared() {
        letGo()
    }

    companion object {
        private const val TAG = "PlayerViewModel"

        /** How long the player waits for its cover's colour before it shows anyway. */
        private const val WASH_WAIT_MS = 1_500L

        /** The app's own: the server through [ApiClient], this device's settings and memory. */
        fun of(context: Context, itemId: String, episodeId: String?, start: PlayerStart, askThenPlay: Boolean): PlayerViewModel {
            val app = context.applicationContext
            val settings = PlayerSettings(app)
            val library = { ApiClient.getLibraryApi()?.let(::LibraryRepository) }
            return PlayerViewModel(
                itemId = itemId,
                episodeId = episodeId,
                start = start,
                askThenPlay = askThenPlay,
                item = { id -> library()?.item(id)?.getOrNull() },
                // Signed out there is nothing to read, and nothing to say about it.
                savedProgress = { id, episode ->
                    if (ApiClient.getApiService() == null) SavedProgress.NeverStarted else ResumeSources.saved(id, episode)
                },
                resumeCheck = ResumeCheck(
                    progress = ResumeSources::progress,
                    latestSession = ResumeSources::latestSession,
                    thisDevice = PlaybackDevice.info(app).deviceId.orEmpty()
                ),
                speeds = SpeedMemory(app),
                resolveUrl = { ApiClient.generateFullUrl(it) },
                playNextEpisode = { settings.playNextEpisode },
                coverWash = { id -> coverWashOf(coverBitmap(app, id)) },
                seriesBooks = { libraryId, seriesId -> library()?.seriesBooks(libraryId, seriesId) },
                heartOf = { libraryId, id, episode ->
                    ApiClient.getPlaylistApi()?.let { FavouriteHeart(Favourites(it, FavouritesChoice(app)), libraryId, id, episode) }
                },
                serverFormat = SharedPreferencesHandler(app).getUserPreferences().dateFormat
            )
        }
    }
}
