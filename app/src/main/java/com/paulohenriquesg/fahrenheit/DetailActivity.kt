package com.paulohenriquesg.fahrenheit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class DetailActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)

        setContent {
            FahrenheitTheme {
                if (itemId != null) {
                    DetailScreen(itemId)
                } else {
                    Toast.makeText(this, "Item ID is missing", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    @Composable
    fun DetailScreen(itemId: String) {
        var itemDetail by remember { mutableStateOf<LibraryItemResponse?>(null) }
        var coverImage by remember { mutableStateOf<Bitmap?>(null) }
        var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }

        val context = LocalContext.current

        LaunchedEffect(itemId) {
            loadItemDetails(context, itemId) { response ->
                itemDetail = response
                episodes = response?.media?.episodes.orEmpty()
            }
            loadCoverImage(context, itemId) { bitmap ->
                coverImage = bitmap
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                coverImage?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = itemDetail?.media?.metadata?.title ?: "Cover Image",
                        modifier = Modifier
                            .size(200.dp) // Adjust the size to keep the image big
                            .padding(end = 16.dp)
                    )
                }
                Column {
                    itemDetail?.let {
                        Text(text = it.media.metadata.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        val description = it.media.metadata.description ?: "No description available"
                        val annotatedDescription = remember(description) {
                            buildAnnotatedString {
                                append(
                                    HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                                )
                            }
                        }
                        Text(text = annotatedDescription, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    } ?: Text(text = "Loading...", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            itemDetail?.media?.episodes?.let { episodes ->
                if (episodes.isNotEmpty()) {
                    Text(text = "Episodes", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    LazyColumn {
                        items(episodes) { episode ->
                            EpisodeCard(episode, coverImage)
                        }
                    }
                }
            }
        }
    }

    private fun loadItemDetails(
        context: Context,
        itemId: String,
        callback: (LibraryItemResponse?) -> Unit
    ) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val host = sharedPreferences.getString("host", null)
        val token = sharedPreferences.getString("token", null)

        if (host != null && token != null) {
            val apiService = ApiClient.create(host, token)
            apiService.getLibraryItem(itemId).enqueue(object : Callback<LibraryItemResponse> {
                override fun onResponse(
                    call: Call<LibraryItemResponse>,
                    response: Response<LibraryItemResponse>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        Toast.makeText(context, "Failed to load item details", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<LibraryItemResponse>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    private suspend fun loadCoverImage(
        context: Context,
        itemId: String,
        callback: (Bitmap?) -> Unit
    ) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val host = sharedPreferences.getString("host", null)
        val token = sharedPreferences.getString("token", null)

        if (host != null && token != null) {
            val apiService = ApiClient.create(host, token)
            withContext(Dispatchers.IO) {
                val response = apiService.getItemCover(itemId).execute()
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val inputStream = responseBody.byteStream()
                        callback(BitmapFactory.decodeStream(inputStream))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load cover image", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    @Composable
    fun EpisodeCard(episode: Episode, coverImage: Bitmap?) {
        val context = LocalContext.current

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent = PlayerActivity.createIntent(context, episode.libraryItemId, episode.id)
                    context.startActivity(intent)
                },
            onClick = {
                val intent = PlayerActivity.createIntent(context, episode.libraryItemId, episode.id)
                context.startActivity(intent)
            }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                coverImage?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Podcast Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .padding(end = 16.dp)
                    )
                }
                Column {
                    Text(text = episode.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatPubDate(episode.pubDate),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    private fun formatPubDate(pubDate: String): String {
        val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return parser.parse(pubDate)?.let { formatter.format(it) } ?: pubDate
    }

    companion object {
        private const val EXTRA_ITEM_ID = "item_id"

        fun createIntent(context: Context, itemId: String): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
            }
        }
    }
}