package com.paulohenriquesg.fahrenheit.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.book.BookPlayerActivity
import com.paulohenriquesg.fahrenheit.podcast.PlayerActivity
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class DetailActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)

        setContent {
            FahrenheitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    if (itemId != null) {
                        DetailScreen(itemId)
                    } else {
                        Toast.makeText(this, "Item ID is missing", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    fun DetailScreen(itemId: String) {
        var itemDetail by remember { mutableStateOf<LibraryItemResponse?>(null) }
        var expanded by remember { mutableStateOf(false) }

        val context = LocalContext.current

        LaunchedEffect(itemId) {
            loadItemDetails(context, itemId) { response ->
                itemDetail = response
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                CoverImage(
                    itemId = itemId,
                    contentDescription = itemDetail?.media?.metadata?.title ?: "Cover Image"
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    itemDetail?.let {
                        Text(
                            text = it.media.metadata.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            val description =
                                it.media.metadata.description ?: "No description available"
                            val annotatedDescription = remember(description) {
                                buildAnnotatedString {
                                    append(
                                        HtmlCompat.fromHtml(
                                            description,
                                            HtmlCompat.FROM_HTML_MODE_COMPACT
                                        ).toString()
                                    )
                                }
                            }
                            Text(
                                text = if (expanded) annotatedDescription else buildAnnotatedString {
                                    append(
                                        annotatedDescription.text.take(100)
                                    )
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (expanded) Int.MAX_VALUE else 5,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (expanded) "View Less" else "View More",
                            color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clickable { expanded = !expanded }
                                    .padding(top = 8.dp)
                            )
                        }
                } ?: Text(text = "Loading...", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (itemDetail?.mediaType == "book") {
                Button(
                    onClick = {
                        val intent = BookPlayerActivity.createIntent(context, itemId)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                Text(text = "Play Book", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                itemDetail?.media?.episodes?.sortedByDescending { it.publishedAt }
                    ?.let { episodes ->
                        if (episodes.isNotEmpty()) {
                        Text(text = "Episodes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            LazyColumn {
                                items(episodes) { episode ->
                                    EpisodeCard(episode)
                                }
                            }
                        } else {
                            Text(text = "No Episodes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
            apiClient.getLibraryItem(itemId).enqueue(object : Callback<LibraryItemResponse> {
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

    @Composable
    fun EpisodeCard(episode: Episode) {
        val context = LocalContext.current

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent =
                        PlayerActivity.createIntent(context, episode.libraryItemId, episode.id)
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
                CoverImage(
                    itemId = episode.libraryItemId,
                    contentDescription = "Podcast Logo",
                    size = 64.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
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
        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault()),
            SimpleDateFormat("yyyy", Locale.getDefault())
        )
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        for (format in formats) {
            try {
                return format.parse(pubDate)?.let { formatter.format(it) } ?: pubDate
            } catch (e: ParseException) {
                // Continue to the next format
            }
        }
        return pubDate // Return the original date string if no format matches
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