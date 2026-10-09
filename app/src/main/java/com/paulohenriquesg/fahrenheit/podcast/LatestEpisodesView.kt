package com.paulohenriquesg.fahrenheit.podcast

import android.app.Activity
import androidx.compose.runtime.remember
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
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
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.elements.MarqueeText
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import com.paulohenriquesg.fahrenheit.utils.RichText
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.paulohenriquesg.fahrenheit.ui.CardFocus
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LatestEpisodesView(
    libraryId: String,
    load: suspend (String) -> Result<List<RecentPodcastEpisode>> = { id ->
        ApiClient.getBrowseApi()?.let { BrowseRepository(it).recentEpisodes(id) }
            ?: Result.failure(IllegalStateException("not signed in"))
    },
    /** What has been heard (#78), by episode id, from the shared store (#207). */
    heard: Map<String, EpisodeProgress> = emptyMap()
) {
    val context = LocalContext.current
    var episodes by remember { mutableStateOf<List<RecentPodcastEpisode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    // The server decides how a date is written and says so at login.
    val serverDateFormat = remember { SharedPreferencesHandler(context).getUserPreferences().dateFormat }
    LaunchedEffect(libraryId) {
        load(libraryId)
            .onSuccess { episodes = it; loadFailed = false }
            // Without this the screen said "no recent episodes found" whether
            // the library was empty or the response could not be read at all.
            .onFailure { episodes = emptyList(); loadFailed = true }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        ScreenTitle(
            text = stringResource(R.string.latest_episodes),
            modifier = Modifier.padding(bottom = Space.gap)
        )

        Box(modifier = Modifier.fillMaxSize()) {
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
                // The server lists only unfinished episodes, so an empty list is
                // usually a listener who is up to date (#109): not an error, and
                // "No recent episodes found" read like one.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.readingH),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.latest_caught_up),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Space.inset))
                    Text(
                        text = stringResource(R.string.latest_caught_up_why),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
                                progress = heard[recentEpisode.id],
                                onClick = {
                                    context.startActivity(latestEpisodeIntent(context, recentEpisode))
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
    serverDateFormat: String? = null,
    progress: EpisodeProgress? = null
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
            val inProgress = progress as? EpisodeProgress.InProgress
            // How far in rides along the bottom of the art, where the eye
            // already is (#109), as on Home's covers.
            Box {
                CoverImage(
                    itemId = episode.libraryItemId,
                    contentDescription = EpisodeRowDisplay.coverDescription(episode),
                    size = 88.dp
                )
                if (inProgress != null) {
                    ArtProgress(
                        fraction = inProgress.fraction.toFloat(),
                        modifier = Modifier.align(Alignment.BottomStart).width(88.dp)
                    )
                }
            }

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
                    // One line, so the description fits beneath in the fixed
                    // height; the marquee shows the rest on focus.
                    maxLines = 1
                )

                // Podcast name, and when the episode came out: without a date
                // a new episode looked exactly like one from March.
                val published = EpisodeRowDisplay.published(episode, serverFormat = serverDateFormat)
                val isNew = EpisodeRowDisplay.isNew(episode, progress, System.currentTimeMillis())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        inProgress != null -> Chip(
                            stringResource(R.string.episode_percent_in, EpisodeRowDisplay.percentIn(inProgress)),
                            filled = false
                        )
                        isNew -> Chip(stringResource(R.string.episode_new), filled = true)
                    }
                    Text(
                        text = listOfNotNull(
                            episode.podcast?.metadata?.title?.takeIf { it.isNotBlank() },
                            published.takeIf { it.isNotBlank() },
                            // Once started, what is left matters more than the
                            // length; both without seconds from ten minutes on.
                            if (inProgress != null) {
                                stringResource(R.string.time_left, EpisodeRowDisplay.length(inProgress.secondsLeft))
                            } else {
                                episode.duration?.takeIf { it > 0 }?.let { EpisodeRowDisplay.length(it) }
                            }
                        ).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Marked, not hidden or struck through (#78).
                    if (progress == EpisodeProgress.Heard) {
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Heard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                episode.description?.let { desc ->
                    Text(
                        text = RichText.fromHtml(desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** A short label ahead of the row's facts: New, filled; how far in, outlined. */
@Composable
private fun Chip(text: String, filled: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .then(
                if (filled) {
                    Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                } else {
                    Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant), RoundedCornerShape(50))
                }
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
    Spacer(Modifier.width(8.dp))
}

/** The bar along the bottom of an episode's art: a dark track, filled in primary. */
@Composable
private fun ArtProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .background(Color.Black.copy(alpha = 0.55f))
            .testTag(EpisodeCardTags.PROGRESS)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Choosing an episode here plays it, as it does on a Home shelf. It used to open
 * the podcast's screen, leaving the viewer to find the episode again.
 */
fun latestEpisodeIntent(context: Context, episode: RecentPodcastEpisode): Intent =
    PlayerActivity.createIntent(context, episode.libraryItemId, episode.id, autoPlay = true)

object EpisodeCardTags {
    const val PROGRESS = "episode-progress"
}
