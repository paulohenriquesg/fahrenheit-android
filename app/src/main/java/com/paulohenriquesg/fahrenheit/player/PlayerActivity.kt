package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemRequest
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

/**
 * The one player, for a book or a podcast episode (#73).
 *
 * Replaces BookPlayerActivity and podcast.PlayerActivity, which shared the
 * transport and differed in three lines of content, now [NowPlaying]. Every
 * fix had to be made twice, and the second copy was nearly missed each time.
 *
 * Playback still lives and dies with this screen. A player that outlives it
 * (an app-wide mini-player, #16) needs a media service; [NowPlaying] and
 * [ProgressReporter] are kept out of the Activity so they can move there.
 */
class PlayerActivity : ComponentActivity() {
    private lateinit var mediaSession: MediaSessionCompat
    private var isPlaying by mutableStateOf(false)
    private var reporting: Job? = null

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

        mediaSession = MediaSessionCompat(this, "PlayerActivity").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    if (isPlaying) {
                        GlobalMediaPlayer.getInstance().pause()
                        playing(false, itemId, episodeId)
                    } else {
                        GlobalMediaPlayer.getInstance().start()
                        playing(true, itemId, episodeId)
                    }
                }

                override fun onPause() {
                    GlobalMediaPlayer.getInstance().pause()
                    playing(false, itemId, episodeId)
                }

                override fun onStop() {
                    GlobalMediaPlayer.getInstance().stop()
                    playing(false, itemId, episodeId)
                }
            })
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_STOP
                    )
                    .build()
            )
            isActive = true
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
                    Player(itemId, episodeId, autoPlay)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Player(itemId: String, episodeId: String?, autoPlay: Boolean) {
        var nowPlaying by remember { mutableStateOf<NowPlaying?>(null) }
        var progress by remember { mutableStateOf<MediaProgressResponse?>(null) }
        var failed by remember { mutableStateOf(false) }
        var currentTime by remember { mutableDoubleStateOf(0.0) }

        LaunchedEffect(itemId, episodeId) {
            val api = ApiClient.getLibraryApi()
            val item = api?.let { LibraryRepository(it).item(itemId).getOrNull() }
            val serverFormat = SharedPreferencesHandler(this@PlayerActivity).getUserPreferences().dateFormat
            // Progress first: the transport takes its start position once, when
            // it is first drawn, so a position arriving after it was ignored and
            // playback started from zero.
            progress = savedProgress(itemId, episodeId)
            nowPlaying = item?.let { NowPlaying.of(it, episodeId, System.currentTimeMillis(), serverFormat) }
            failed = nowPlaying == null
            if (episodeId != null) openSession(itemId, episodeId)
        }

        val playing = nowPlaying
        when {
            failed -> Text(
                text = stringResource(R.string.item_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            playing == null -> Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
            else -> PlayerScreen(
                nowPlaying = playing,
                currentTime = currentTime,
                onGoToPodcast = { startActivity(podcastIntent(this, itemId)) },
                transport = {
                    val url = playing.contentUrl?.let { ApiClient.generateFullUrl(it) }
                    // The file that will play decides the length, where it has one.
                    val start = ResumePoint.decide(
                        progress = progress,
                        trackTotal = playing.trackTotal,
                        mediaDuration = playing.mediaDuration
                    )
                    if (url != null) {
                        MediaPlayerController(
                            url,
                            mediaSession,
                            isPlaying,
                            { playing(it, itemId, episodeId) },
                            start.totalSeconds,
                            start.positionSeconds,
                            playing.chapters,
                            authToken = ApiClient.getToken(),
                            shouldAutoPlay = autoPlay,
                            onCurrentTimeUpdate = { currentTime = it }
                        )
                    }
                }
            )
        }
    }

    /** Playback started or stopped; while it plays, the position is reported, once. */
    private fun playing(now: Boolean, itemId: String, episodeId: String?) {
        isPlaying = now
        if (!now || reporting?.isActive == true) return
        val api = ApiClient.getApiService() ?: return
        val player = GlobalMediaPlayer.getInstance()
        reporting = lifecycleScope.launch {
            ProgressReporter(
                send = { request ->
                    val call = if (episodeId != null) {
                        api.userCreateOrUpdateMediaProgress(itemId, episodeId, request)
                    } else {
                        api.userCreateOrUpdateMediaProgress(itemId, request)
                    }
                    val response = call.awaitResponse()
                    // Logged, not shown: the next round retries.
                    if (!response.isSuccessful) error("progress rejected: ${response.code()}")
                },
                position = { player.currentPosition / 1000.0 },
                total = { player.duration / 1000.0 }
            ).run { isPlaying }
        }
    }

    /** Where the listener left off, or null if never started or unreadable. */
    private suspend fun savedProgress(itemId: String, episodeId: String?): MediaProgressResponse? {
        val api = ApiClient.getApiService() ?: return null
        val call = if (episodeId != null) api.userGetMediaProgress(itemId, episodeId) else api.userGetMediaProgress(itemId)
        val response = runCatching { call.awaitResponse() }.getOrNull() ?: return null
        val body = response.body().takeIf { response.isSuccessful } ?: return null
        // The server answers for the item when it has nothing for the episode.
        return body.takeIf { episodeId == null || it.episodeId == episodeId }
    }

    /** Opens a listening session on the server for an episode, as the podcast player did. */
    private suspend fun openSession(itemId: String, episodeId: String) {
        val api = ApiClient.getApiService() ?: return
        val request = PlayLibraryItemRequest(
            deviceInfo = PlayLibraryItemDeviceInfo(
                deviceId = "Fire Stick",
                clientName = getString(R.string.app_name),
                clientVersion = "0.0.1",
                manufacturer = "Amazon",
                model = Build.MODEL,
                sdkVersion = 25
            ),
            forceDirectPlay = false,
            forceTranscode = false,
            supportedMimeTypes = emptyList(),
            mediaPlayer = "unknown"
        )
        runCatching { api.playLibraryItem(itemId, episodeId, request).awaitResponse() }
            .onFailure { Log.w(TAG, "Play session not opened: ${it.message}") }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaSession.isInitialized) mediaSession.release()
        // Without this the player lives on, holding a codec and audio focus.
        GlobalMediaPlayer.release()
    }

    companion object {
        private const val TAG = "PlayerActivity"
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
    }
}
