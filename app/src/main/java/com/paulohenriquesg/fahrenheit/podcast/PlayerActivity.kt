package com.paulohenriquesg.fahrenheit.podcast

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class PlayerActivity : ComponentActivity() {
    private lateinit var mediaSession: MediaSessionCompat
    private var isPlaying by mutableStateOf(false)
    private var mediaProgress by mutableStateOf<MediaProgressResponse?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val podcastId = intent.getStringExtra(EXTRA_PODCAST_ID)
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID)

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
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
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
                if (podcastId != null && episodeId != null) {
                    fetchMediaProgress(podcastId, episodeId)

                    PlayerScreen(podcastId, episodeId, mediaSession, isPlaying, mediaProgress) { newIsPlaying ->
                        isPlaying = newIsPlaying
                    }
                } else {
                    Toast.makeText(this, "Podcast or Episode ID is missing", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }
    }

    private fun startProgressUpdateCoroutine(podcastId: String?, episodeId: String?) {
        val apiClient = ApiClient.getApiService()
        val mediaPlayer = GlobalMediaPlayer.getInstance()

        lifecycleScope.launch {
            while (isPlaying) {
                delay(5000L)
                val currentTimeState = mediaPlayer.currentPosition / 1000f
                val totalTime = mediaPlayer.duration / 1000f
                val request = MediaProgressRequest(currentTime = currentTimeState, duration = totalTime)
                apiClient?.userCreateOrUpdateMediaProgress(podcastId!!, episodeId!!, request)
                    ?.enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (!response.isSuccessful) {
                                Toast.makeText(this@PlayerActivity, "Failed to update media progress", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Toast.makeText(this@PlayerActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
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
                            mediaProgress = if (response.body()?.episodeId == episodeId) response.body() else null
                        } else if (response.code() == 404) {
                            // Call userCreateOrUpdateMediaProgress if 404
                            val request = MediaProgressRequest(currentTime = 0f, duration = 0f)
                            apiClient.userCreateOrUpdateMediaProgress(libraryItemId, episodeId, request)
                                .enqueue(object : Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                        if (response.isSuccessful) {
                                            // Handle successful creation
                                        } else {
                                            Toast.makeText(this@PlayerActivity, "Failed to create media progress", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        Toast.makeText(this@PlayerActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            } else {
                                Toast.makeText(this@PlayerActivity, "Failed to load media progress", Toast.LENGTH_SHORT).show()
                            }
                        }

                    override fun onFailure(call: Call<MediaProgressResponse>, t: Throwable) {
                        Toast.makeText(this@PlayerActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
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

        fun createIntent(context: Context, podcastId: String, episodeId: String): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_PODCAST_ID, podcastId)
                putExtra(EXTRA_EPISODE_ID, episodeId)
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
    onPlayPause: (Boolean) -> Unit
) {
    var episode by remember { mutableStateOf<Episode?>(null) }
    var coverImage by remember { mutableStateOf<Bitmap?>(null) }
    var playLibraryItemResponse by remember { mutableStateOf<PlayLibraryItemResponse?>(null) }
    val context = LocalContext.current

    LaunchedEffect(podcastId, episodeId) {
        loadEpisodeDetails(context, podcastId, episodeId) { response ->
            episode = response?.media?.episodes?.find { it.id == episodeId }
        }
        loadCoverImage(context, podcastId) { bitmap ->
            coverImage = bitmap
        }
        playLibraryItem(context, podcastId, episodeId) { response ->
            playLibraryItemResponse = response
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        coverImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = episode?.title ?: "Cover Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        episode?.let {
            Text(text = it.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            val contentUrl = ApiClient.generateFullUrl(it.audioTrack?.contentUrl?: "")
            if (contentUrl != null) {
                MediaPlayerController(
                    contentUrl,
                    mediaSession,
                    isPlaying,
                    onPlayPause,
                    mediaProgress?.duration ?: episode?.audioTrack?.duration?.toFloat() ?: 0f,
                    mediaProgress?.currentTime ?: 0f
                )
            }
        } ?: Text(text = "Loading...")
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

private suspend fun loadCoverImage(
    context: Context,
    podcastId: String,
    callback: (Bitmap?) -> Unit
) {
    val apiClient = ApiClient.getApiService()
    if (apiClient != null) {
        withContext(Dispatchers.IO) {
            val response = apiClient.getItemCover(podcastId).execute()
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val inputStream = responseBody.byteStream()
                    callback(BitmapFactory.decodeStream(inputStream))
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load cover image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}