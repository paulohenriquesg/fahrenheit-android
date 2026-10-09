package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.R
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
import com.paulohenriquesg.fahrenheit.favourites.EpisodeFavouriteButton
import com.paulohenriquesg.fahrenheit.ui.elements.RowButton
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
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

/**
 * The heart on the focused episode row (#180, frame 2), while the library has
 * a Favourites playlist: its name, this show's episodes in it, and a press.
 * One value, so a page laid out anew (#205) passes the same thing.
 */
data class EpisodeHearts(val playlist: String, val episodes: Set<String>, val onToggle: (Episode) -> Unit)

private val tabNames = mapOf(
    EpisodeTab.All to "All",
    EpisodeTab.OnServer to "On the server",
    EpisodeTab.NotDownloaded to "Not downloaded",
    EpisodeTab.Favourites to "Favourites"
)

/**
 * A podcast's episodes: every one in the feed for an admin, marked by whether
 * the server has it; the server's own for anyone else (#76).
 *
 * @param focusFirstRow whether the newest episode takes focus on arrival:
 *   false when the page's primary action already does.
 * @param date how a row writes its publication date.
 * @param top the head of the list, which scrolls away: on the podcast page,
 *   Now playing and the description (#205).
 * @param rowsLeft where Left from a row goes, rather than to the tab just
 *   above it, which sits a few dp further left (#205).
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
    onMark: (Episode, Boolean) -> Unit = { _, _ -> },
    coverItemId: String? = null,
    date: (EpisodeRow) -> String = { "" },
    focusFirstRow: Boolean = true,
    hearts: EpisodeHearts? = null,
    rowsLeft: FocusRequester = FocusRequester.Default,
    top: @Composable () -> Unit = {}
) {
    // One list, its head included, so moving down into the episodes pushes the
    // head off: under a fixed one only about 1.5 rows fitted in 540dp.
    val listState = rememberLazyListState()
    // Something must hold focus or the remote does nothing. Usually the
    // page's primary action has it; failing that, the newest episode.
    val firstKey = screen.rows.firstOrNull()?.key
    val initialFocus = rememberInitialFocus(enabled = focusFirstRow && firstKey != null, firstKey)

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("podcast_list")
    ) {
        item(key = "top") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                top()
                screen.note?.let { NoteBox(it) }
            }
        }
        // Pinned: the filter stays in view while the head is gone, and a tab
        // stays one Up away. The show's name is in the page's left column (#205).
        screen.tabs?.let { tabs ->
            stickyHeader(key = "pinned") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
        val keys = StableKeys.of(screen.rows) { it.key }
        items(screen.rows.size, key = { keys[it] }) { index ->
            val row = screen.rows[index]
            EpisodeRowCard(
                row = row,
                state = downloads[row.key],
                progress = row.onServer?.id?.let(progress::get),
                coverItemId = coverItemId,
                date = date(row),
                modifier = Modifier
                    .focusProperties { left = rowsLeft }
                    .then(if (index == 0) Modifier.focusRequester(initialFocus) else Modifier),
                onMark = row.onServer?.let { episode -> { finished: Boolean -> onMark(episode, finished) } },
                hearts = hearts,
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

/** Whether a row is drawn dimmed: finished, and not where focus is (#181, #205). */
val EpisodeRowDimmed = SemanticsPropertyKey<Boolean>("EpisodeRowDimmed")

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
    onMark: ((Boolean) -> Unit)?,
    hearts: EpisodeHearts?,
    onPress: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    // The row or its buttons: they show while any holds focus, so Right can
    // reach them and Left come back (#181).
    var rowFocused by remember { mutableStateOf(false) }
    val finished = progress == EpisodeProgress.Heard
    Row(
        modifier = Modifier.fillMaxWidth().onFocusChanged { rowFocused = it.hasFocus },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            scale = CardFocus.noGrowth,
            onClick = onPress,
            modifier = modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .testTag("episode_row_${row.key}")
                .semantics { this[EpisodeRowDimmed] = finished && !rowFocused }
                .onFocusChanged { focused = it.isFocused },
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
            )
        ) {
            Row(
                // Finished rows are dimmed, as the web app shows them, until focus
                // makes one the row being read.
                modifier = Modifier.padding(12.dp).alpha(if (finished && !rowFocused) FINISHED_ALPHA else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kept on every row so per-episode covers have a place to land.
                coverItemId?.let {
                    CoverImage(itemId = it, contentDescription = row.title, size = 64.dp)
                    Spacer(Modifier.width(Space.gap))
                }
                // A tick reads as "done", so it means heard. Being on the server is
                // what the play icon at the end of the row says.
                Box(modifier = Modifier.size(24.dp)) {
                    when {
                        finished -> Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Finished",
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
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (row.downloaded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        inProgress?.let { "${formatDuration(it.secondsLeft)} left" },
                        // Marked, not hidden or struck through (#78), in the web app's word (#181).
                        "finished".takeIf { finished }
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inProgress?.let { ProgressBar(it.fraction) }
                }
                Spacer(Modifier.width(Space.gap))
                val label = EpisodeRowLabel.of(row.downloaded, state, focused, progress)
                if (row.downloaded) {
                    // Playable: an icon, with the word ("Play" / "Resume") as its
                    // description, only where Center would play (#205). The space
                    // is kept, so focus moves nothing.
                    Box(Modifier.size(32.dp)) {
                        if (focused) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Kept on every row, so the button appearing moves nothing.
        Box(modifier = Modifier.width(MARK_SLOT), contentAlignment = Alignment.Center) {
            if (rowFocused && onMark != null) {
                MarkButton(
                    finished = finished,
                    onClick = { onMark(!finished) },
                    modifier = Modifier.testTag("episode_mark_${row.key}")
                )
            }
        }
        // After Mark finished, on an episode the server has: a playlist holds only those.
        if (hearts != null) {
            Box(modifier = Modifier.width(MARK_SLOT), contentAlignment = Alignment.Center) {
                val episode = row.onServer
                if (rowFocused && episode != null) {
                    EpisodeFavouriteButton(
                        filled = episode.id in hearts.episodes,
                        playlist = hearts.playlist,
                        onClick = { hearts.onToggle(episode) },
                        modifier = Modifier.testTag("episode_favourite_${row.key}")
                    )
                }
            }
        }
    }
}

/** Mark finished, or unfinished. */
@Composable
private fun MarkButton(finished: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) = RowButton(
    onClick = onClick,
    icon = Icons.Filled.Check,
    description = stringResource(if (finished) R.string.mark_unfinished else R.string.mark_finished),
    modifier = modifier
)

private const val FINISHED_ALPHA = 0.55f
private val MARK_SLOT = 56.dp
