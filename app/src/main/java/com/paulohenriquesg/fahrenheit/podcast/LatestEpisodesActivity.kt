package com.paulohenriquesg.fahrenheit.podcast

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.RecentEpisodesResponse
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LatestEpisodesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val libraryId = intent.getStringExtra(EXTRA_LIBRARY_ID) ?: run {
            finish()
            return
        }

        setContent {
            FahrenheitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    LatestEpisodesScreen(libraryId = libraryId)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_LIBRARY_ID = "library_id"

        fun createIntent(context: Context, libraryId: String): Intent {
            return Intent(context, LatestEpisodesActivity::class.java).apply {
                putExtra(EXTRA_LIBRARY_ID, libraryId)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LatestEpisodesScreen(libraryId: String) {
    val context = LocalContext.current
    var episodes by remember { mutableStateOf<List<RecentPodcastEpisode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(libraryId) {
        val apiClient = ApiClient.getApiService()
        apiClient?.getRecentEpisodes(libraryId, limit = 50)?.enqueue(object : Callback<RecentEpisodesResponse> {
            override fun onResponse(
                call: Call<RecentEpisodesResponse>,
                response: Response<RecentEpisodesResponse>
            ) {
                if (response.isSuccessful) {
                    episodes = response.body()?.episodes ?: emptyList()
                }
                isLoading = false
            }

            override fun onFailure(call: Call<RecentEpisodesResponse>, t: Throwable) {
                isLoading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Latest Episodes",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading episodes...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent episodes found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(episodes) { recentEpisode ->
                    EpisodeCard(
                        episode = recentEpisode,
                        onClick = {
                            val intent = DetailActivity.createIntent(
                                context,
                                recentEpisode.libraryItemId
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episode: RecentPodcastEpisode,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Episode title
                Text(
                    text = episode.episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Podcast name
                Text(
                    text = episode.podcast.media.metadata.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Episode description
                episode.episode.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
