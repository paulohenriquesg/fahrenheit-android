package com.paulohenriquesg.fahrenheit.book

import android.content.Context
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.GlobalMediaPlayer
import com.paulohenriquesg.fahrenheit.MediaPlayerController
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.player.ProgressSync
import com.paulohenriquesg.fahrenheit.player.ResumePoint
import com.paulohenriquesg.fahrenheit.player.timelineOf
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookPlayerActivity : ComponentActivity() {
    private var isPlaying by mutableStateOf(false)
    private var mediaProgress by mutableStateOf<MediaProgressResponse?>(null)
    private var bookDetail by mutableStateOf<LibraryItemResponse?>(null)
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaSession = MediaSessionCompat(this, "BookPlayerActivity")

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID)

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
                        startProgressUpdateCoroutine(bookId ?: "", bookDetail?.media?.duration ?: 0.0)
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
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    if (bookId != null) {
                        fetchMediaProgress(bookId)
                        fetchBookDetails(bookId)

                        BookPlayerScreen(
                            bookId,
                            mediaSession,
                            isPlaying,
                            mediaProgress,
                            bookDetail
                        ) { newIsPlaying ->
                            isPlaying = newIsPlaying
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.book_id_is_missing), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

            }
        }
    }

    private fun startProgressUpdateCoroutine(bookId: String, totalDuration: Double) {
        val apiClient = ApiClient.getApiService()
        val mediaPlayer = GlobalMediaPlayer.getInstance()
        var lastSent: Double? = null

        lifecycleScope.launch {
            while (isPlaying) {
                delay(ProgressSync.INTERVAL_MS)
                val request = ProgressSync.next(
                    position = mediaPlayer.currentPosition / 1000.0,
                    total = totalDuration,
                    lastSent = lastSent
                ) ?: continue
                lastSent = request.currentTime

                apiClient?.userCreateOrUpdateMediaProgress(bookId, request = request)
                    ?.enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (!response.isSuccessful) {
                                // Logged, not shown: this runs every few seconds
                                // behind playback, and the next round retries.
                                Log.w(TAG, "Progress update rejected: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.w(TAG, "Progress update failed: ${t.message}")
                        }
                    })
            }
        }
    }

    private fun fetchBookDetails(libraryItemId: String) {
        val apiClient = ApiClient.getApiService()
        apiClient?.getLibraryItem(libraryItemId)?.enqueue(object : Callback<LibraryItemResponse> {
            override fun onResponse(
                call: Call<LibraryItemResponse>,
                response: Response<LibraryItemResponse>
            ) {
                if (response.isSuccessful) {
                    bookDetail = response.body()
                }
            }

            override fun onFailure(call: Call<LibraryItemResponse>, t: Throwable) {
                Toast.makeText(
                    this@BookPlayerActivity,
                    this@BookPlayerActivity.getString(R.string.failed_to_load_book_details),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun fetchMediaProgress(libraryItemId: String) {
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
            apiClient.userGetMediaProgress(libraryItemId = libraryItemId)
                .enqueue(object : Callback<MediaProgressResponse> {
                    override fun onResponse(
                        call: Call<MediaProgressResponse>,
                        response: Response<MediaProgressResponse>
                    ) {
                        if (response.isSuccessful) {
                            mediaProgress = response.body()
                        } else if (response.code() == 404) {
                            val totalDuration = bookDetail?.media?.duration ?: 0.0
                            val request = MediaProgressRequest(currentTime = 0.0, duration = totalDuration)
                            apiClient.userCreateOrUpdateMediaProgress(
                                libraryItemId = libraryItemId,
                                request = request
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
                                                this@BookPlayerActivity,
                                                this@BookPlayerActivity.getString(R.string.failed_to_create_media_progress),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        Toast.makeText(
                                            this@BookPlayerActivity,
                                            "Network error: ${t.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        } else {
                            Toast.makeText(
                                this@BookPlayerActivity,
                                this@BookPlayerActivity.getString(R.string.failed_to_load_media_progress),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<MediaProgressResponse>, t: Throwable) {
                        Toast.makeText(
                            this@BookPlayerActivity,
                            "Network error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"

        fun createIntent(context: Context, bookId: String): Intent {
            return Intent(context, BookPlayerActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
            }
        }
    }
}

@Composable
fun BookPlayerScreen(
    bookId: String,
    mediaSession: MediaSessionCompat,
    isPlaying: Boolean,
    mediaProgress: MediaProgressResponse?,
    bookDetail: LibraryItemResponse?,
    onPlayPause: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentPlaybackTime by remember { mutableStateOf(mediaProgress?.currentTime ?: 0.0) }

    // Start progress update coroutine when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying && bookDetail != null) {
            val apiClient = ApiClient.getApiService()
            val mediaPlayer = GlobalMediaPlayer.getInstance()
            // The files that will play, not the item's reported duration.
            val totalDuration = timelineOf(bookDetail.media.tracks.orEmpty())?.totalDuration
                ?: bookDetail.media.duration ?: 0.0
            var lastSent: Double? = null

            while (isPlaying) {
                delay(ProgressSync.INTERVAL_MS)
                val request = ProgressSync.next(
                    position = mediaPlayer.currentPosition / 1000.0,
                    total = totalDuration,
                    lastSent = lastSent
                ) ?: continue
                lastSent = request.currentTime

                apiClient?.userCreateOrUpdateMediaProgress(bookId, request = request)
                    ?.enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (!response.isSuccessful) {
                                Log.w(TAG, "Progress update rejected: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.w(TAG, "Progress update failed: ${t.message}")
                        }
                    })
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            CoverImage(
                itemId = bookId,
                    contentDescription = bookDetail?.media?.metadata?.title ?: "Cover Image"
                )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                bookDetail?.let {
                    Text(
                        text = it.media.metadata.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = it.media.metadata.seriesName?.takeIf { it.isNotEmpty() }?.let { seriesName ->
                            "($seriesName)"
                        } ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    it.media.metadata.authorName?.let { authorName ->
                        Text(
                            text = "Author: $authorName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    it.media.metadata.narratorName?.let { narratorName ->
                        Text(
                            text = "Narrator: $narratorName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val currentChapter = it.media.chapters?.firstOrNull { chapter ->
                        val start = chapter.start ?: 0.0
                        val end = chapter.end ?: 0.0
                        currentPlaybackTime in start..end
                    }
                    currentChapter?.let { chapter ->
                        Text(
                            text = "Current Chapter: ${chapter.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } ?: Text(text = stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        bookDetail?.let {
            val contentUrl = it.media.tracks?.firstOrNull()?.contentUrl?.let { url ->
                ApiClient.generateFullUrl(url)
            }
            // The tracks are what will actually play, so their total is the
            // length to trust where the item has any.
            val start = ResumePoint.decide(
                progress = mediaProgress,
                trackTotal = timelineOf(it.media.tracks.orEmpty())?.totalDuration,
                mediaDuration = it.media.duration
            )
            if (contentUrl != null) {
                MediaPlayerController(
                    contentUrl,
                    mediaSession,
                    isPlaying,
                    onPlayPause,
                    start.totalSeconds,
                    start.positionSeconds,
                    it.media.chapters,
                    authToken = ApiClient.getToken(),
                    onCurrentTimeUpdate = { newTime ->
                        currentPlaybackTime = newTime
                    }
                )
            }
        } ?: Text(text = stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun loadBookDetails(
    context: Context,
    bookId: String,
    callback: (LibraryItemResponse?) -> Unit
) {
    val apiClient = ApiClient.getApiService()
    if (apiClient != null) {
        apiClient.getLibraryItem(bookId).enqueue(object : Callback<LibraryItemResponse> {
            override fun onResponse(
                call: Call<LibraryItemResponse>,
                response: Response<LibraryItemResponse>
            ) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    Toast.makeText(context, context.getString(R.string.failed_to_load_book_details), Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<LibraryItemResponse>, t: Throwable) {
                Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

private const val TAG = "BookPlayerActivity"
