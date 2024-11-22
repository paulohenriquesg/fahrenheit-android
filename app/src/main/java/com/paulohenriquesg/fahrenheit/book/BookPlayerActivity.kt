// BookPlayerActivity.kt
package com.paulohenriquesg.fahrenheit.book

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.paulohenriquesg.fahrenheit.GlobalMediaPlayer
import com.paulohenriquesg.fahrenheit.MediaPlayerController
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class BookPlayerActivity : ComponentActivity() {
    private var isPlaying by mutableStateOf(false)
    private var mediaProgress by mutableStateOf<MediaProgressResponse?>(null)
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
                        startProgressUpdateCoroutine(bookId ?: "")
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
                if (bookId != null) {
                    fetchMediaProgress(bookId)

                    BookPlayerScreen(bookId, mediaSession, isPlaying, mediaProgress) { newIsPlaying ->
                        isPlaying = newIsPlaying
                    }
                } else {
                    Toast.makeText(this, "Book ID is missing", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun startProgressUpdateCoroutine(bookId: String) {
        val apiClient = ApiClient.getApiService()
        val mediaPlayer = GlobalMediaPlayer.getInstance()

        lifecycleScope.launch {
            while (isPlaying) {
                delay(5000L)
                val currentTimeState = mediaPlayer.currentPosition / 1000f
                val totalTime = mediaPlayer.duration / 1000f
                val request = MediaProgressRequest(currentTime = currentTimeState, duration = totalTime)
                apiClient?.userCreateOrUpdateMediaProgress(bookId, request= request)
                    ?.enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (!response.isSuccessful) {
                                Toast.makeText(this@BookPlayerActivity, "Failed to update media progress", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Toast.makeText(this@BookPlayerActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

    private fun fetchMediaProgress(libraryItemId: String) {
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
            apiClient.userGetMediaProgress(libraryItemId= libraryItemId)
                .enqueue(object : Callback<MediaProgressResponse> {
                    override fun onResponse(
                        call: Call<MediaProgressResponse>,
                        response: Response<MediaProgressResponse>
                    ) {
                        if (response.isSuccessful) {
                            mediaProgress = response.body()
                        } else if (response.code() == 404) {
                            val request = MediaProgressRequest(currentTime = 0f, duration = 0f)
                            apiClient.userCreateOrUpdateMediaProgress(libraryItemId= libraryItemId, request = request)
                                .enqueue(object : Callback<Void> {
                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                        if (response.isSuccessful) {
                                            // Handle successful creation
                                        } else {
                                            Toast.makeText(this@BookPlayerActivity, "Failed to create media progress", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        Toast.makeText(this@BookPlayerActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            Toast.makeText(this@BookPlayerActivity, "Failed to load media progress", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<MediaProgressResponse>, t: Throwable) {
                        Toast.makeText(this@BookPlayerActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
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
    onPlayPause: (Boolean) -> Unit
) {
    var bookDetail by remember { mutableStateOf<LibraryItemResponse?>(null) }
    var coverImage by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(bookId) {
        loadBookDetails(context, bookId) { response ->
            bookDetail = response
        }
        loadCoverImage(context, bookId) { bitmap ->
            coverImage = bitmap
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        coverImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = bookDetail?.media?.metadata?.title ?: "Cover Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        bookDetail?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it.media.metadata.title,
        style = MaterialTheme.typography.titleLarge
                )
                it.media.metadata.seriesName.let { seriesName ->
                    Text(
                        text = "($seriesName)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            it.media.metadata.authorName?.let { authorName ->
                Text(text = "Author: $authorName", style = MaterialTheme.typography.bodyMedium)
            }
            it.media.metadata.narratorName?.let { narratorName ->
                Text(text = "Narrator: $narratorName", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val contentUrl = it.media.tracks.firstOrNull()?.contentUrl?.let { url -> ApiClient.generateFullUrl(url) }
            if (contentUrl != null) {
                MediaPlayerController(
                    contentUrl,
                    mediaSession,
                    isPlaying,
                    onPlayPause,
                    mediaProgress?.duration ?: bookDetail?.media?.duration?.toFloat() ?: 0f,
                    mediaProgress?.currentTime ?: 0f,
                    it.media.chapters
                )
            }
        } ?: Text(text = "Loading...")
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
                    Toast.makeText(context, "Failed to load book details", Toast.LENGTH_SHORT).show()
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
    bookId: String,
    callback: (Bitmap?) -> Unit
) {
    val apiClient = ApiClient.getApiService()
    if (apiClient != null) {
        withContext(Dispatchers.IO) {
            val response = apiClient.getItemCover(bookId).execute()
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