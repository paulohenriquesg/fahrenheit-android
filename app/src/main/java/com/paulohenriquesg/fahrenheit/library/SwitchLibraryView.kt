package com.paulohenriquesg.fahrenheit.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryStats
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.CardFocus
import com.paulohenriquesg.fahrenheit.ui.Border
import com.paulohenriquesg.fahrenheit.ui.Radius
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon

/**
 * Picking a library is a big, rare choice, so it gets tiles rather than a list:
 * what is in it, and which one you are in.
 */
@Composable
fun SwitchLibraryView(
    libraries: List<Library>,
    currentId: String?,
    onSelect: (Library) -> Unit,
    modifier: Modifier = Modifier,
    stats: Map<String, LibraryStats> = emptyMap()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        ScreenTitle(stringResource(R.string.switch_library))

        if (libraries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.switch_library_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(Space.gap),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            val keys = StableKeys.of(libraries) { it.id ?: it.name.orEmpty() }
            items(libraries.size, key = { keys[it] }) { index ->
                val library = libraries[index]
                val current = library.id != null && library.id == currentId
                Card(
                    scale = CardFocus.noGrowth,
                    onClick = { onSelect(library) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("library_${library.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Space.inset),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = iconFor(library.mediaType),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = library.name.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = tileLine(library, library.id?.let { stats[it] }),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (current) {
                            Text(
                                text = stringResource(R.string.switch_library_current),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("library_${library.id}_current")
                            )
                        }
                    }
                }
            }
            item(key = "library_add") { AddLibraryTile() }
        }
    }
}

/** Libraries are made on the server; this only says so, so it takes no focus. */
@Composable
private fun AddLibraryTile() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .alpha(0.55f)
            .clip(Radius.panel)
            .border(Border.rest, MaterialTheme.colorScheme.surfaceVariant, Radius.panel)
            .padding(Space.inset)
            .testTag("library_add"),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.switch_library_add),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.switch_library_add_where),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * What is in a library and how much: shows and episodes for podcasts, books
 * and hours of audio otherwise. Until the counts arrive, the kind of library.
 */
@Composable
private fun tileLine(library: Library, stats: LibraryStats?): String {
    val podcast = library.mediaType == "podcast"
    val items = stats?.totalItems ?: return stringResource(
        if (podcast) R.string.switch_library_kind_podcast else R.string.switch_library_kind_book
    )
    if (podcast) {
        val episodes = stats.numAudioTracks ?: 0
        return stringResource(
            R.string.switch_library_tile_line,
            pluralStringResource(R.plurals.switch_library_shows, items, items),
            pluralStringResource(R.plurals.switch_library_episodes, episodes, episodes)
        )
    }
    val books = pluralStringResource(R.plurals.switch_library_books, items, items)
    val seconds = stats.totalDuration ?: 0.0
    if (seconds <= 0.0) return books
    val hours = (seconds / 3600).roundToInt()
    val duration = if (hours >= 1) {
        pluralStringResource(R.plurals.switch_library_hours, hours, hours)
    } else {
        // Rounding a short library to "0 hours" would say it is empty.
        val minutes = (seconds / 60).roundToInt().coerceAtLeast(1)
        pluralStringResource(R.plurals.switch_library_minutes, minutes, minutes)
    }
    return stringResource(R.string.switch_library_tile_line, books, duration)
}

/** The old picker showed one; a wall of identical tiles tells you nothing. */
private fun iconFor(mediaType: String?): ImageVector = when (mediaType) {
    "podcast" -> Icons.Filled.Podcasts
    else -> Icons.Filled.Book
}
