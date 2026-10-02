package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.focus.focusRequester
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import com.paulohenriquesg.fahrenheit.detail.FactChip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.ui.CardFocus
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.utils.RichText
import com.paulohenriquesg.fahrenheit.utils.formatDuration

private val tabNames = mapOf(
    EpisodeTab.All to "All",
    EpisodeTab.OnServer to "On the server",
    EpisodeTab.NotDownloaded to "Not downloaded"
)

/**
 * A podcast's episodes: every one in the feed for an admin, marked by whether
 * the server has it; the server's own for anyone else (#76).
 *
 * @param focusFirstRow whether the newest episode takes focus on arrival:
 *   false when the header's primary action already does.
 * @param date how a row writes its publication date.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PodcastEpisodesView(
    screen: PodcastScreen,
    tab: EpisodeTab,
    onTab: (EpisodeTab) -> Unit,
    downloads: Map<String, DownloadState>,
    onPlay: (Episode) -> Unit,
    onDownload: (EpisodeRow) -> Unit,
    progress: Map<String, EpisodeProgress> = emptyMap(),
    coverItemId: String? = null,
    date: (EpisodeRow) -> String = { "" },
    focusFirstRow: Boolean = true,
    title: String = "",
    header: @Composable () -> Unit = {}
) {
    // One list, header included, so moving down into the episodes pushes the
    // header off: under a fixed one only about 1.5 rows fitted in 540dp.
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    // Something must hold focus or the remote does nothing. Usually the
    // header's primary action has it; failing that, the newest episode.
    val firstKey = screen.rows.firstOrNull()?.key
    val initialFocus = rememberInitialFocus(enabled = focusFirstRow && firstKey != null, firstKey)

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("podcast_list")
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                header()
                screen.note?.let { NoteBox(it) }
            }
        }
        // Pinned: which podcast, and which filter, stay in view while the
        // header is gone - and a tab stays one Up away.
        stickyHeader(key = "pinned") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (scrolled && title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("pinned_title")
                    )
                }
                screen.tabs?.let { tabs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        tabs.forEach { (each, count) ->
                            FilterChip(
                                selected = each == tab,
                                onClick = { onTab(each) },
                                modifier = Modifier.testTag("episode_tab_${each.name}")
                            ) {
                                Text("${tabNames.getValue(each)} · $count")
                            }
                        }
                    }
                }
            }
        }
        val keys = StableKeys.of(screen.rows) { it.key }
        items(screen.rows.size, key = { keys[it] }) { index ->
            val row = screen.rows[index]
            EpisodeRowCard(
                row = row,
                state = downloads[row.key],
                progress = row.onServer?.id?.let(progress::get),
                coverItemId = coverItemId,
                date = date(row),
                modifier = if (index == 0) Modifier.focusRequester(initialFocus) else Modifier,
                onPress = {
                    val episode = row.onServer
                    when {
                        episode != null -> onPlay(episode)
                        EpisodeRowLabel.mayDownload(downloads[row.key]) -> onDownload(row)
                    }
                }
            )
        }
    }
}

/** What the right-hand end of a row says. */
object EpisodeRowLabel {

    fun mayDownload(state: DownloadState?): Boolean = state == null || state == DownloadState.Failed

    fun of(downloaded: Boolean, state: DownloadState?, focused: Boolean, progress: EpisodeProgress? = null): String = when {
        downloaded -> if (progress is EpisodeProgress.InProgress) "Resume" else "Play"
        state == DownloadState.Requested -> "Requested…"
        // The server reports a place in its queue, not a percentage.
        state is DownloadState.Waiting -> if (state.ahead == 0) "Waiting · next" else "Waiting · ${state.ahead} ahead"
        state == DownloadState.Downloading -> "Downloading…"
        state == DownloadState.Failed -> "Download failed"
        // Spelled out only where a press would do it, so a press never
        // fetches something the viewer did not see offered.
        focused -> "Download to server"
        else -> "Not downloaded"
    }
}

/** How far into a half-heard episode the listener is. */
@Composable
internal fun ProgressBar(fraction: Double) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth(0.5f)
            .height(4.dp)
            // Not surfaceVariant: that is also the focused row's fill, and the
            // track vanished into it.
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f))
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun NoteBox(note: Note) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(10.dp))
            .padding(Space.inset)
    ) {
        note.title?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(note.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EpisodeRowCard(
    row: EpisodeRow,
    state: DownloadState?,
    progress: EpisodeProgress?,
    coverItemId: String?,
    date: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        scale = CardFocus.noGrowth,
        onClick = onPress,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("episode_row_${row.key}")
            .onFocusChanged { focused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Kept on every row so per-episode covers have a place to land.
            coverItemId?.let {
                CoverImage(itemId = it, contentDescription = row.title, size = 64.dp)
                Spacer(Modifier.width(Space.gap))
            }
            // A tick reads as "done", so it means heard. Being on the server is
            // what the play icon at the end of the row says.
            Box(modifier = Modifier.size(24.dp)) {
                when {
                    progress == EpisodeProgress.Heard -> Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Heard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    !row.downloaded -> Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Not on the server",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(Space.gap))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (row.downloaded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Marked, not hidden or struck through (#78).
                    if (progress == EpisodeProgress.Heard) {
                        Spacer(Modifier.width(12.dp))
                        FactChip(Fact("Heard"))
                    }
                }
                row.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = RichText.fromHtml(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val inProgress = progress as? EpisodeProgress.InProgress
                val meta = listOfNotNull(
                    date.takeIf { it.isNotEmpty() },
                    row.durationSeconds?.takeIf { it > 0 }?.let { formatDuration(it) },
                    inProgress?.let { "${formatDuration(it.secondsLeft)} left" }
                ).joinToString(" • ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inProgress?.let { ProgressBar(it.fraction) }
            }
            Spacer(Modifier.width(Space.gap))
            val label = EpisodeRowLabel.of(row.downloaded, state, focused, progress)
            if (row.downloaded) {
                // Playable: an icon, with the word ("Play" / "Resume") as its description.
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = label,
                    tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
