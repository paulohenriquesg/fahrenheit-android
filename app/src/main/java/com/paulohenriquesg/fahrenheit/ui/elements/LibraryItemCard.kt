package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import com.paulohenriquesg.fahrenheit.ui.CardFocus
import com.paulohenriquesg.fahrenheit.utils.listeningLength

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryItemCard(
    item: LibraryItem,
    progress: CoverProgress.Started? = null,
    finished: Boolean = false,
    onLongClick: ((LibraryItem) -> Unit)? = null,
    onClick: (LibraryItem) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .height(300.dp)
    ) {
        Card(
            scale = CardFocus.noGrowth,
            onClick = { onClick(item) },
            onLongClick = onLongClick?.let { longPress -> { longPress(item) } },
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { isFocused = it.isFocused },
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val dimmed = LibraryItemDisplay.dimmed(item)
                // A finished episode is dimmed as the podcast page's rows are,
                // until focus makes it the card being read (#192).
                val faded = dimmed || (finished && !isFocused)
                Box(
                    modifier = Modifier
                        .alpha(if (faded) DIMMED_ALPHA else 1f)
                        .semantics { this[LibraryItemCardTags.Dimmed] = faded }
                ) {
                    CoverImage(
                        itemId = item.id,
                        contentDescription = item.media.metadata.title,
                        // Drawn on the placeholder when there is no cover (#149).
                        title = LibraryItemDisplay.title(item),
                        author = LibraryItemDisplay.author(item),
                        // The server says when there is none; asking anyway is
                        // a 404 on every drawing, never cached.
                        hasCover = item.media.coverPath != null
                    )
                    if (progress != null) {
                        CoverProgressBar(
                            fraction = progress.fraction,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                MarqueeText(
                    text = LibraryItemDisplay.title(item),
                    isFocused = isFocused,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                // One line under the title, so every card keeps its height: how
                // long is left once started (#104), rounded as on Latest Episodes, else the podcast's count
                // (#75), else the author.
                val episodeCount = LibraryItemDisplay.episodeCount(item)
                val secondLine = when {
                    progress != null -> stringResource(R.string.time_left, listeningLength(progress.secondsLeft))
                    episodeCount != null -> episodeCount
                    else -> LibraryItemDisplay.author(item)
                }
                if (finished) {
                    // The finished mark leads the line it shares (#192).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FinishedTick(Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = secondLine.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (secondLine != null) {
                    Text(
                        text = secondLine,
                        style = MaterialTheme.typography.bodyMedium,
                        // The warning colour of the feed facts on the podcast's screen.
                        color = if (dimmed && progress == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        val unfinishedBadge = LibraryItemDisplay.unfinishedBadge(item)
        if (unfinishedBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // The focused Card raises itself and drew over the badge,
                    // hiding the count on exactly the card being looked at.
                    .zIndex(1f)
                    .size(24.dp) // Set a fixed size for the badge
                    .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center // Center the text within the badge
            ) {
                Text(
                    text = unfinishedBadge,
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** The mock's bar along the bottom of the art: a dark track, filled in primary. */
@Composable
private fun CoverProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(Color.Black.copy(alpha = 0.55f))
            .testTag(LibraryItemCardTags.PROGRESS)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/** How far an empty podcast's cover fades on the grid (#75). */
private const val DIMMED_ALPHA = 0.45f

object LibraryItemCardTags {
    const val PROGRESS = "cover-progress"

    /** Whether the cover is drawn dimmed. */
    val Dimmed = SemanticsPropertyKey<Boolean>("Dimmed")
}
