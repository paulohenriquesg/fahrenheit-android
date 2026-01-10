package com.paulohenriquesg.fahrenheit.podcast

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.GlobalMediaPlayer
import com.paulohenriquesg.fahrenheit.MediaPlayerController
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemRequest
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemResponse
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.MarqueeText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlayerActivity : ComponentActivity() {
    private lateinit var mediaSession: MediaSessionCompat
    private var isPlaying by mutableStateOf(false)
    private var mediaProgress by mutableStateOf<MediaProgressResponse?>(null)
    private var shouldAutoPlay by mutableStateOf(false)
    private var isProgressUpdateRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val podcastId = intent.getStringExtra(EXTRA_PODCAST_ID)
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, false)

        android.util.Log.d("PlayerActivity", "onCreate - podcastId: $podcastId, episodeId: $episodeId, autoPlay: $autoPlay")

        // Store autoPlay flag but don't start playing yet - wait for media to be prepared
        shouldAutoPlay = autoPlay

        mediaSession = MediaSessionCompat(this, "PlayerActivity").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    if (isPlaying) {
                        GlobalMediaPlayer.getInstance().pause()
                        isPlaying = false
                    } else {
                        GlobalMediaPlayer.getInstance().start()
                        isPlaying = true
                        startProgressUpdateCoroutine(podcastId, episodeId)
                    }
                }

                override fun onPause() {
                    super.onPause()
                    GlobalMediaPlayer.getInstance().pause()
                    isPlaying = false
                }

                override fun onStop() {
                    super.onStop()
                    GlobalMediaPlayer.getInstance().stop()
                    isPlaying = false
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
                    color = androidx.tv.material3.MaterialTheme.colorScheme.background,
                    contentColor = androidx.tv.material3.MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    if (podcastId != null && episodeId != null) {
                        fetchMediaProgress(podcastId, episodeId)

                        PlayerScreen(
                            podcastId,
                            episodeId,
                            mediaSession,
                            isPlaying,
                            mediaProgress,
                            shouldAutoPlay
                        ) { newIsPlaying ->
                            isPlaying = newIsPlaying
                            // Start progress updates when playback starts
                            if (newIsPlaying) {
                                startProgressUpdateCoroutine(podcastId, episodeId)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Podcast or Episode ID is missing", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                }
            }
        }
    }

    private fun startProgressUpdateCoroutine(podcastId: String?, episodeId: String?) {
        // Prevent multiple coroutines from running
        if (isProgressUpdateRunning) {
            android.util.Log.d("PlayerActivity", "Progress update already running, skipping")
            return
        }

        val apiClient = ApiClient.getApiService()
        val mediaPlayer = GlobalMediaPlayer.getInstance()

        // Validate required parameters
        if (podcastId == null || episodeId == null || apiClient == null) {
            android.util.Log.e("PlayerActivity", "Cannot update progress: missing podcastId, episodeId, or apiClient")
            return
        }

        isProgressUpdateRunning = true
        android.util.Log.d("PlayerActivity", "Starting progress update coroutine")

        lifecycleScope.launch {
            while (isPlaying) {
                delay(5000L)
                val currentTimeState = mediaPlayer.currentPosition / 1000.0
                val totalTime = mediaPlayer.duration / 1000.0
                val request =
                    MediaProgressRequest(currentTime = currentTimeState, duration = totalTime)
                apiClient.userCreateOrUpdateMediaProgress(podcastId, episodeId, request)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (!response.isSuccessful) {
                                Toast.makeText(
                                    this@PlayerActivity,
                                    "Failed to update media progress",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Toast.makeText(
                                this@PlayerActivity,
                                "Network error: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
            // Reset flag when coroutine exits
            isProgressUpdateRunning = false
            android.util.Log.d("PlayerActivity", "Progress update coroutine stopped")
        }
    }

    private fun fetchMediaProgress(libraryItemId: String, episodeId: String) {
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
            apiClient.userGetMediaProgress(libraryItemId, episodeId)
                .enqueue(object : Callback<MediaProgressResponse> {
                    override fun onResponse(
                        call: Call<MediaProgressResponse>,
                        response: Response<MediaProgressResponse>
                    ) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody?.episodeId == episodeId) {
                                mediaProgress = responseBody
                            } else {
                                mediaProgress = null
                            }
                        } else if (response.code() == 404) {
                            // Call userCreateOrUpdateMediaProgress if 404
                            val request = MediaProgressRequest(currentTime = 0.0, duration = 0.0)
                            apiClient.userCreateOrUpdateMediaProgress(
                                libraryItemId,
                                episodeId,
                                request
                            )
                                .enqueue(object : Callback<Void> {
                                    override fun onResponse(
                                        call: Call<Void>,
                                        response: Response<Void>
                                    ) {
                                        if (response.isSuccessful) {
                                            // Handle successful creation
                                        } else {
                                            Toast.makeText(
                                                this@PlayerActivity,
                                                "Failed to create media progress",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        Toast.makeText(
                                            this@PlayerActivity,
                                            "Network error: ${t.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        } else {
                            Toast.makeText(
                                this@PlayerActivity,
                                "Failed to load media progress",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<MediaProgressResponse>, t: Throwable) {
                        Toast.makeText(
                            this@PlayerActivity,
                            "Network error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    companion object {
        private const val EXTRA_PODCAST_ID = "podcast_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_AUTO_PLAY = "auto_play"

        fun createIntent(context: Context, podcastId: String, episodeId: String, autoPlay: Boolean = false): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_PODCAST_ID, podcastId)
                putExtra(EXTRA_EPISODE_ID, episodeId)
                putExtra(EXTRA_AUTO_PLAY, autoPlay)
            }
        }
    }
}

@Composable
fun PlayerScreen(
    podcastId: String,
    episodeId: String,
    mediaSession: MediaSessionCompat,
    isPlaying: Boolean,
    mediaProgress: MediaProgressResponse?,
    shouldAutoPlay: Boolean,
    onPlayPause: (Boolean) -> Unit
) {
    var episode by remember { mutableStateOf<Episode?>(null) }
    var podcastName by remember { mutableStateOf<String?>(null) }
    var playLibraryItemResponse by remember { mutableStateOf<PlayLibraryItemResponse?>(null) }
    var isTitleFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(podcastId, episodeId) {
        android.util.Log.d("PlayerScreen", "LaunchedEffect - podcastId: $podcastId, episodeId: $episodeId")
        loadEpisodeDetails(context, podcastId, episodeId) { response ->
            android.util.Log.d("PlayerScreen", "Episode details loaded - episodes count: ${response?.media?.episodes?.size}")
            episode = response?.media?.episodes?.find { it.id == episodeId }
            podcastName = response?.media?.metadata?.title
            android.util.Log.d("PlayerScreen", "Found episode: ${episode?.title}, duration: ${episode?.audioTrack?.duration}, podcast: $podcastName")
        }
        playLibraryItem(context, podcastId, episodeId) { response ->
            android.util.Log.d("PlayerScreen", "PlayLibraryItem response received")
            playLibraryItemResponse = response
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover image (left side)
            CoverImage(
                itemId = podcastId,
                contentDescription = episode?.title ?: "Cover Image",
                size = 200.dp
            )

            // Episode information (right side)
            episode?.let { ep ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Episode title with marquee
                    MarqueeText(
                        text = ep.title,
                        isFocused = isTitleFocused,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier.onFocusChanged { isTitleFocused = it.isFocused }
                    )

                    // Podcast name (for context)
                    podcastName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Episode metadata row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Publication date
                        Text(
                            text = formatPubDate(ep.pubDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Season/Episode number
                        if (ep.season != null || ep.episode != null) {
                            Text(
                                text = "• S${ep.season ?: "?"}E${ep.episode ?: "?"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Duration
                        ep.audioTrack?.duration?.let { duration ->
                            Text(
                                text = "• ${formatDuration(duration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Episode description (scrollable)
                    if (ep.description.isNotEmpty()) {
                        Text(
                            text = ep.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } ?: Text(text = "Loading...", color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MediaPlayerController (playback controls at bottom)
        episode?.let {
            val contentUrl = ApiClient.generateFullUrl(it.audioTrack?.contentUrl ?: "")
            // Use mediaProgress duration only if it's valid (positive), otherwise use episode duration
            val duration = if (mediaProgress?.duration != null && mediaProgress.duration > 0) {
                mediaProgress.duration
            } else {
                episode?.audioTrack?.duration ?: 0.0
            }
            val currentTime = mediaProgress?.currentTime ?: 0.0
            android.util.Log.d("PlayerScreen", "MediaPlayerController - duration: $duration, currentTime: $currentTime, contentUrl: $contentUrl, mediaProgress.duration: ${mediaProgress?.duration}")
            if (contentUrl != null) {
                MediaPlayerController(
                    contentUrl,
                    mediaSession,
                    isPlaying,
                    onPlayPause,
                    duration,
                    currentTime,
                    authToken = ApiClient.getToken(),
                    shouldAutoPlay = shouldAutoPlay
                )
            }
        }
    }
}

private fun playLibraryItem(
    context: Context,
    libraryItemId: String,
    episodeId: String,
    callback: (PlayLibraryItemResponse?) -> Unit
) {
    val apiClient = ApiClient.getApiService()
    if (apiClient != null) {
        val deviceInfo = PlayLibraryItemDeviceInfo(
            deviceId = "Fire Stick",
            clientName = context.getString(R.string.app_name),
            clientVersion = "0.0.1",
            manufacturer = "Amazon",
            model = Build.MODEL,
            sdkVersion = 25
        )
        val request = PlayLibraryItemRequest(
            deviceInfo = deviceInfo,
            forceDirectPlay = false,
            forceTranscode = false,
            supportedMimeTypes = emptyList(),
            mediaPlayer = "unknown"
        )

        apiClient.playLibraryItem(libraryItemId, episodeId, request)
            .enqueue(object : Callback<PlayLibraryItemResponse> {
                override fun onResponse(
                    call: Call<PlayLibraryItemResponse>,
                    response: Response<PlayLibraryItemResponse>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<PlayLibraryItemResponse>, t: Throwable) {
                    callback(null)
                }
            })
    } else {
        callback(null)
    }
}

private fun loadEpisodeDetails(
    context: Context,
    podcastId: String,
    episodeId: String,
    callback: (LibraryItemResponse?) -> Unit
) {
    val apiClient = ApiClient.getApiService()
    if (apiClient != null) {
        apiClient.getLibraryItem(podcastId).enqueue(object : Callback<LibraryItemResponse> {
            override fun onResponse(
                call: Call<LibraryItemResponse>,
                response: Response<LibraryItemResponse>
            ) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    Toast.makeText(context, "Failed to load episode details", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<LibraryItemResponse>, t: Throwable) {
                Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

private fun formatPubDate(pubDate: String): String {
    val formats = listOf(
        java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.getDefault()),
        java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
    )
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    for (format in formats) {
        try {
            return format.parse(pubDate)?.let { formatter.format(it) } ?: pubDate
        } catch (e: java.text.ParseException) {
            // Continue to the next format
        }
    }
    return pubDate // Return the original date string if no format matches
}

private fun formatDuration(seconds: Double): String {
    val hours = (seconds / 3600).toInt()
    val minutes = ((seconds % 3600) / 60).toInt()

    return if (hours > 0) {
        String.format("%dh %dm", hours, minutes)
    } else {
        String.format("%dm", minutes)
    }
}