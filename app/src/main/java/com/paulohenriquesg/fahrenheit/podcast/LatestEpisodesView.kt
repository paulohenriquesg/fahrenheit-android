package com.paulohenriquesg.fahrenheit.podcast

import android.app.Activity
import androidx.compose.runtime.remember
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.RecentEpisodesResponse
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.components.BrowseTopBar
import com.paulohenriquesg.fahrenheit.ui.elements.MarqueeText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.utils.RichText
import com.paulohenriquesg.fahrenheit.stats.shortDuration
import com.paulohenriquesg.fahrenheit.ui.CardFocus
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LatestEpisodesView(libraryId: String) {
    val context = LocalContext.current
    var episodes by remember { mutableStateOf<List<RecentPodcastEpisode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    // The server decides how a date is written and says so at login.
    val serverDateFormat = remember { SharedPreferencesHandler(context).getUserPreferences().dateFormat }

    LaunchedEffect(libraryId) {
        val api = ApiClient.getBrowseApi()
        if (api == null) {
            loadFailed = true
            isLoading = false
            return@LaunchedEffect
        }

        BrowseRepository(api).recentEpisodes(libraryId)
            .onSuccess { episodes = it; loadFailed = false }
            // Without this the screen said "no recent episodes found" whether
            // the library was empty or the response could not be read at all.
            .onFailure { episodes = emptyList(); loadFailed = true }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.latest_episodes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 48.dp, top = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.loading_episodes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (loadFailed) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.recent_episodes_load_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_recent_episodes_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Under day headings: a flat list with no dates read as one pile,
                // so a new episode was invisible among old ones.
                val groups = remember(episodes) { EpisodeGroups.of(episodes, System.currentTimeMillis()) }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groups.forEach { group ->
                        // Sticky: the heading stays put while its own episodes
                        // scroll under it, so you always know which day you are
                        // looking at.
                        stickyHeader(key = "group_${group.label}") {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 8.dp)
                            )
                        }
                        val keys = StableKeys.of(group.episodes) { e -> e.id }
                        items(group.episodes.size, key = { "${group.label}_${keys[it]}" }) { index ->
                            val recentEpisode = group.episodes[index]
                            EpisodeCard(
                                episode = recentEpisode,
                                serverDateFormat = serverDateFormat,
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
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episode: RecentPodcastEpisode,
    onClick: () -> Unit,
    serverDateFormat: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        scale = CardFocus.noGrowth,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .onFocusChanged { isFocused = it.isFocused },
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
            // The podcast's cover: a list of recent episodes is a list of
            // different podcasts, and the cover is what tells them apart at a
            // glance. Episodes have no art of their own on the server yet.
            CoverImage(
                itemId = episode.libraryItemId,
                contentDescription = EpisodeRowDisplay.coverDescription(episode),
                size = 88.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Episode title with marquee
                MarqueeText(
                    text = episode.title.orEmpty(),
                    isFocused = isFocused,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                // Podcast name, and when the episode came out: without a date
                // a new episode looked exactly like one from March.
                val published = EpisodeRowDisplay.published(episode, serverFormat = serverDateFormat)
                Text(
                    text = listOfNotNull(
                        episode.podcast?.metadata?.title?.takeIf { it.isNotBlank() },
                        published.takeIf { it.isNotBlank() },
                        episode.duration?.takeIf { it > 0 }?.let { shortDuration(it) }
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Episode description
                episode.description?.let { desc ->
                    Text(
                        text = RichText.fromHtml(desc),
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
