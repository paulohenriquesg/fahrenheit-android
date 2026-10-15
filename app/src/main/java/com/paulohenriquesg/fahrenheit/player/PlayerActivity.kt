package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.favourites.FavouriteButton
import com.paulohenriquesg.fahrenheit.favourites.note
import com.paulohenriquesg.fahrenheit.favourites.HeartChange
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.launch
import android.view.KeyEvent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.elements.coverUrlOf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

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
    /** The screen's state (#221); null only when there was no item to open. */
    private var model: PlayerViewModel? = null
    private val sleepReports = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            model?.extrasChanged(extras)
        }
    }
    private val connection by lazy {
        ControllerSlot(
            connect = { Playback.connect(this, sleepReports) },
            release = { it.release() },
            executor = ContextCompat.getMainExecutor(this),
            onChange = { model?.connect(it?.let(::ControllerLink)) }
        )
    }

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
        // Kept across a configuration change, which then starts nothing anew.
        // auto_play is a request to start, once: not again when this screen
        // comes back, and not after the process was recreated with it.
        val shown = ViewModelProvider(this, viewModelFactory {
            initializer {
                PlayerViewModel.of(
                    this@PlayerActivity,
                    itemId,
                    episodeId,
                    start = PlayerStart(
                        autoPlay = autoPlay && savedInstanceState == null,
                        startAt = startAtOf(intent)?.takeIf { savedInstanceState == null },
                        resumed = savedInstanceState != null
                    ),
                    askThenPlay = asksThenPlays(intent) && savedInstanceState == null
                )
            }
        })[PlayerViewModel::class.java]
        model = shown
        // Shown until the item is ready; after a recreation the intent still holds it.
        val preview = previewOf(intent)
        lifecycleScope.launch {
            shown.notices.collect { notice ->
                when (notice) {
                    PlayerNotice.ProgressUnreadable ->
                        Toast.makeText(this@PlayerActivity, getString(R.string.progress_unreadable), Toast.LENGTH_LONG).show()
                    PlayerNotice.MarkFinishedFailed ->
                        Toast.makeText(this@PlayerActivity, getString(R.string.mark_finished_failed), Toast.LENGTH_LONG).show()
                    is PlayerNotice.Favourite -> confirm(notice.change)
                    PlayerNotice.QueueEnded -> closeAtQueueEnd(this@PlayerActivity)
                }
            }
        }

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
                    Player(shown, itemId, preview)
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
        model?.disconnect()
        super.onStop()
    }

    @androidx.compose.runtime.Composable
    private fun Player(model: PlayerViewModel, itemId: String, preview: PlayerPreview?) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val playback = state.playback
        // After an answer the question's focused button is gone: Play takes focus.
        var focusPlayAgain by remember { mutableIntStateOf(0) }
        // A panel left open when the screen went away does not come back over it.
        val panels = rememberPlayerPanels(state.player)
        val spans = remember(state.nowPlaying) { ChapterClock.spans(state.nowPlaying?.chapters, state.nowPlaying?.trackTotal ?: 0.0) }
        // Its own layout from the first frame, with what the opening screen knew (#223).
        PlayerStage(state, itemId, preview) { playing, connected ->
            PlayerScreen(
                nowPlaying = state.series?.let { playing.withSeriesTotal(it.total) } ?: playing,
                currentTime = state.currentTime,
                wash = state.wash,
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
                            onCurrentTimeUpdate = model::timeUpdated,
                            onPlay = model::play,
                            focusPlayAgain = focusPlayAgain,
                            // An episode's outer buttons open the episodes either side (#108).
                            episodes = playing.episodeId?.let {
                                EpisodeSkip(
                                    onPrevious = playing.previous?.let { other -> { goToEpisode(connected, other, playing.show) } },
                                    onNext = playing.next?.let { other -> { goToEpisode(connected, other, playing.show) } }
                                )
                            },
                            trailing = {
                                PlayerActions(
                                    panels = panels,
                                    speed = rememberPlaybackSpeed(connected),
                                    sleep = state.sleep,
                                    chapters = spans.isNotEmpty(),
                                    onGoToPodcast = if (playing.goToPodcast) ({ goToPodcast(playing.itemId) }) else null,
                                    favourite = state.favourite?.let { heart ->
                                        {
                                            FavouriteButton(
                                                filled = heart.filled,
                                                playlist = heart.playlist,
                                                onClick = model::toggleFavourite
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                },
                overlay = {
                    state.offer?.let { asked ->
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
                            onContinue = { if (model.answerResume(moveThere = true)) focusPlayAgain++ },
                            onStay = { if (model.answerResume(moveThere = false)) focusPlayAgain++ }
                        )
                    }
                    PlayerPanelHost(panels) { panel ->
                        when (panel) {
                            PlayerPanel.Chapters -> ChaptersPanel(
                                spans = spans,
                                at = playback?.bookPosition() ?: state.currentTime,
                                onChoose = { playback?.seekToBookTime(it) },
                                onClose = panels::close
                            )
                            PlayerPanel.About -> {
                                val playInstead: (SeriesBook) -> Unit = { other ->
                                    switchTo(this@PlayerActivity, connected, other.itemId, preview = PlayerPreview.book(other.itemId, other.title, null))
                                }
                                // A book's and an episode's About alike: the one book layout over the wash (#134, #178).
                                AboutScreen(
                                    nowPlaying = playing, wash = state.wash, series = state.series, finished = state.finished, marking = state.marking,
                                    onPlayInstead = playInstead, onMarkFinished = model::markFinished, onClose = panels::close
                                )
                            }
                            PlayerPanel.Speed -> SpeedPanel(
                                current = rememberPlaybackSpeed(connected),
                                onChoose = model::chooseSpeed,
                                onClose = panels::close,
                                forShow = playing.goToPodcast
                            )
                            PlayerPanel.Sleep -> SleepPanel(
                                sleep = state.sleep,
                                chapters = spans.isNotEmpty(),
                                onChoose = model::chooseSleep,
                                onClose = panels::close
                            )
                        }
                    }
                }
            )
        }
    }

    // Keys the screen leaves alone come here before the window hands media keys
    // to the media session, so Play can ask first (#90).
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        (isPlayKey(keyCode) && model?.remotePlay(true) == true) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        (isPlayKey(keyCode) && model?.remotePlay(false) == true) || super.onKeyUp(keyCode, event)

    private fun isPlayKey(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY

    private fun goToPodcast(podcastId: String) = leaveForPodcast(this, podcastId)

    /** The short note after the heart (frame 1). */
    private fun confirm(change: HeartChange) {
        Toast.makeText(this, change.note(this), Toast.LENGTH_SHORT).show()
    }

    private val playerSettings by lazy { PlayerSettings(this) }

    /** Previous or Next episode: this one stops, and the other opens, playing (see [switchTo]). */
    private fun goToEpisode(controller: androidx.media3.common.Player, episode: EpisodeRef, show: String?) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        switchTo(this, controller, itemId, episode.id, PlayerPreview.episode(itemId, episode.title, show))
    }

    companion object {
        private const val EXTRA_ITEM_ID = "item_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_AUTO_PLAY = "auto_play"
        private const val EXTRA_START_AT = "start_at"
        private const val EXTRA_ASK_THEN_PLAY = "ask_then_play"

        /** Opened by a Play from outside the player (#144): ask, then play. */
        fun asksThenPlays(intent: Intent): Boolean = intent.getBooleanExtra(EXTRA_ASK_THEN_PLAY, false)

        /**
         * @param episodeId the episode to play, for a podcast; null for a book.
         * @param startAt where to start, in whole-book seconds, over the saved position (#105).
         * @param preview what the opening screen shows of it, for the loading frame (#223).
         */
        fun createIntent(
            context: Context,
            itemId: String,
            episodeId: String? = null,
            autoPlay: Boolean = false,
            startAt: Double? = null,
            askThenPlay: Boolean = false,
            preview: PlayerPreview? = null
        ): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
                episodeId?.let { putExtra(EXTRA_EPISODE_ID, it) }
                putExtra(EXTRA_AUTO_PLAY, autoPlay)
                startAt?.let { putExtra(EXTRA_START_AT, it) }
                if (askThenPlay) putExtra(EXTRA_ASK_THEN_PLAY, true)
                preview?.writeTo(this)
            }

        /** Now playing, in the rail or the bar: the player reattaches to what is queued, where it is. */
        fun reopenIntent(context: Context, entry: RailEntry): Intent =
            createIntent(context, entry.itemId, entry.episodeId, preview = PlayerPreview.of(entry))

        /**
         * A Play from outside with the app in view (#144), from the playback
         * service: it asks as it comes back to the item, and plays after the
         * answer. [title] is what the session holds, all it knows of the item.
         */
        fun outsidePlayIntent(context: Context, file: QueuedFile, title: String?): Intent =
            createIntent(
                context, file.itemId, file.episodeId, askThenPlay = true,
                preview = title?.takeIf { it.isNotBlank() }?.let { PlayerPreview(it, coverUrl = coverUrlOf(file.itemId)) }
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** What the opening screen handed over for the loading frame (#223); null for nothing. */
        fun previewOf(intent: Intent): PlayerPreview? = PlayerPreview.from(intent)

        /**
         * "Go to podcast". Clears back to the podcast's screen when the player
         * was opened from it, rather than stacking a second copy on top.
         */
        fun podcastIntent(context: Context, podcastId: String, title: String? = null): Intent =
            DetailActivity.createIntent(context, podcastId, PlayerPreview.show(podcastId, title), podcast = true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** The start position the intent asks for (see [PlayerStart]); null for the saved one. */
        internal fun startAtOf(intent: Intent): Double? =
            if (intent.hasExtra(EXTRA_START_AT)) intent.getDoubleExtra(EXTRA_START_AT, 0.0) else null

        /**
         * About's "Play <title> instead?", or Previous and Next episode
         * (#108): this book or episode stops - its closing
         * report going to it - before the other opens and plays, with
         * [preview] - what this screen knows of the other - while it loads (#223).
         */
        internal fun switchTo(
            player: android.app.Activity,
            controller: androidx.media3.common.Player,
            itemId: String,
            episodeId: String? = null,
            preview: PlayerPreview? = null
        ) {
            Playback.end(controller)
            player.startActivity(createIntent(player, itemId, episodeId, autoPlay = true, preview = preview))
            player.finish()
        }

        /**
         * The queue played to its end with the player open (#179): back to
         * where it was opened from - the book's or podcast's screen, or Home.
         * Closing, not opening: Back has no extra screen to go through.
         */
        internal fun closeAtQueueEnd(player: android.app.Activity) = player.finish()

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
