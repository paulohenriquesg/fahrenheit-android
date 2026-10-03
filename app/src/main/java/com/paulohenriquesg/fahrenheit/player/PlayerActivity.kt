package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.awaitResponse

/**
 * The one player, for a book or a podcast episode (#73).
 *
 * Replaces BookPlayerActivity and podcast.PlayerActivity, which shared the
 * transport and differed in three lines of content, now [NowPlaying]. Every
 * fix had to be made twice, and the second copy was nearly missed each time.
 *
 * Playback lives in [PlaybackService]; this screen drives it through a
 * MediaController, connected while it is visible. Back stops playback, Home
 * leaves it playing.
 */
class PlayerActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var connectFailed by mutableStateOf(false)
    private val connection by lazy {
        ControllerSlot(
            connect = { Playback.connect(this) },
            release = { it.release() },
            executor = ContextCompat.getMainExecutor(this),
            onChange = {
                controller = it
                connectFailed = it == null
            }
        )
    }
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
        start = PlayerStart(autoPlay = autoPlay && savedInstanceState == null)

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

    override fun onStop() {
        connection.close { Playback.leave(it, finishing = isFinishing) }
        controller = null
        super.onStop()
    }

    @androidx.compose.runtime.Composable
    private fun Player(itemId: String, episodeId: String?) {
        var nowPlaying by remember { mutableStateOf<NowPlaying?>(null) }
        var failed by remember { mutableStateOf(false) }
        var currentTime by remember { mutableDoubleStateOf(0.0) }

        LaunchedEffect(itemId, episodeId) {
            val api = ApiClient.getLibraryApi()
            val item = api?.let { LibraryRepository(it).item(itemId).getOrNull() }
            val serverFormat = SharedPreferencesHandler(this@PlayerActivity).getUserPreferences().dateFormat
            nowPlaying = item?.let { NowPlaying.of(it, episodeId, System.currentTimeMillis(), serverFormat) }
            failed = nowPlaying == null
        }

        val playing = nowPlaying
        val connected = controller
        var ready by remember(connected, playing) { mutableStateOf(false) }
        LaunchedEffect(connected, playing) {
            if (connected == null || playing == null) return@LaunchedEffect
            // The saved position is read when it is needed, not when the
            // screen opened: by then it may have been listened past elsewhere.
            ready = start.begin(connected, playing, { savedProgress(itemId, episodeId) }) { ApiClient.generateFullUrl(it) }
            if (!ready) failed = true
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
            else -> PlayerScreen(
                nowPlaying = playing,
                currentTime = currentTime,
                transport = {
                    // ready implies a timeline: PlayerStart refuses a NowPlaying without one.
                    val timeline = playing.timeline
                    if (timeline != null) {
                        val playback = remember(connected, timeline) { BookPlayback(connected, timeline) }
                        MediaPlayerController(
                            player = connected,
                            playback = playback,
                            totalTime = timeline.totalDuration,
                            chapters = playing.chapters,
                            onCurrentTimeUpdate = { currentTime = it },
                            trailing = {
                                if (playing.goToPodcast) GoToPodcastButton { goToPodcast(itemId) }
                            }
                        )
                    }
                }
            )
        }
    }

    private fun goToPodcast(podcastId: String) = leaveForPodcast(this, podcastId)

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
        private const val EXTRA_ITEM_ID = "item_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_AUTO_PLAY = "auto_play"

        /**
         * @param episodeId the episode to play, for a podcast; null for a book.
         */
        fun createIntent(context: Context, itemId: String, episodeId: String? = null, autoPlay: Boolean = false): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
                episodeId?.let { putExtra(EXTRA_EPISODE_ID, it) }
                putExtra(EXTRA_AUTO_PLAY, autoPlay)
            }

        /**
         * "Go to podcast". Clears back to the podcast's screen when the player
         * was opened from it, rather than stacking a second copy on top.
         */
        fun podcastIntent(context: Context, podcastId: String): Intent =
            DetailActivity.createIntent(context, podcastId).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /**
         * "Go to podcast" leaves the episode: closing the player stops it, as
         * Back does. Opened from the podcast's screen the clear-top closed it
         * anyway; opened from anywhere else the episode played on behind it.
         */
        internal fun leaveForPodcast(player: android.app.Activity, podcastId: String) {
            player.startActivity(podcastIntent(player, podcastId))
            player.finish()
        }
    }
}
