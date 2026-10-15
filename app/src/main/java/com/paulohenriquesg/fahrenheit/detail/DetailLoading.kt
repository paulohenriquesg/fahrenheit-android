package com.paulohenriquesg.fahrenheit.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.podcast.PODCAST_COLUMN_GAP
import com.paulohenriquesg.fahrenheit.podcast.PODCAST_COLUMN_WIDTH
import com.paulohenriquesg.fahrenheit.podcast.PodcastSlots
import com.paulohenriquesg.fahrenheit.ui.components.BookColumn
import com.paulohenriquesg.fahrenheit.ui.components.TITLE_TAG
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderShape

/*
 * The book and podcast pages while the item loads (#225; docs/mocks/
 * detail-loading.html): each page's own layout from the first frame, with
 * what the opening card showed - cover, title, a book's author - in its
 * final place, and still grey shapes for what the server has yet to say.
 * Nothing here can take focus: it lands where it always has once the page
 * is ready.
 */

object DetailLoadingTags {
    const val FRAME = "detail_loading"
    const val EPISODE_ROWS = "detail_loading_episodes"
}

/**
 * The page, as far as it can be drawn: failed, the loading frame, or [ready]
 * with the item and the screen's margin.
 *
 * @param podcast which page's frame to draw before the item says what it is.
 */
@Composable
fun DetailStage(
    itemId: String,
    preview: PlayerPreview?,
    podcast: Boolean,
    item: LibraryItemResponse?,
    failed: Boolean,
    ready: @Composable (LibraryItemResponse, PaddingValues) -> Unit
) {
    when {
        // The previous version showed an empty screen with a toast that
        // was gone by the time anyone looked at it.
        failed -> Text(
            text = stringResource(R.string.item_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        item == null -> DetailBody { margin -> DetailLoading(itemId, preview, podcast, margin) }
        else -> DetailBody { margin -> ready(item, margin) }
    }
}

/** A book's or a podcast's page before its item arrives. */
@Composable
fun DetailLoading(itemId: String, preview: PlayerPreview?, podcast: Boolean, margin: PaddingValues, slots: PodcastSlots = PodcastSlots.NONE) {
    val frame = Modifier.fillMaxSize().padding(margin).testTag(DetailLoadingTags.FRAME)
    if (podcast) PodcastFrame(itemId, preview, frame) else BookFrame(itemId, preview, frame)
}

/** As `BookOverview` lays a book out: the column, then the description over the series. */
@Composable
private fun BookFrame(itemId: String, preview: PlayerPreview?, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(BookColumn.gap)) {
        Column(Modifier.width(BookColumn.width), verticalArrangement = Arrangement.spacedBy(BookColumn.spacing)) {
            Cover(itemId, preview, BookColumn.width)
            Title(preview, maxLines = 3, Modifier.testTag(TITLE_TAG))
            preview?.byline?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } ?: PlaceholderShape(Modifier.fillMaxWidth(0.6f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ButtonPlaceholders()
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PlaceholderShape(Modifier.fillMaxWidth().weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlaceholderShape(Modifier.width(140.dp).height(14.dp))
                Row(Modifier.clipToBounds(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(SERIES_COVERS) { PlaceholderShape(Modifier.size(96.dp)) }
                }
            }
        }
    }
}

/** As `PodcastPage` lays a show out: the column, then the note, the tabs and the episodes. */
@Composable
private fun PodcastFrame(itemId: String, preview: PlayerPreview?, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(PODCAST_COLUMN_GAP)) {
        Column(Modifier.width(PODCAST_COLUMN_WIDTH).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // The cover gives way, as on the page.
            BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                Cover(itemId, preview, min(maxWidth, maxHeight))
            }
            Title(preview, maxLines = 2)
            PlaceholderShape(Modifier.fillMaxWidth(0.6f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ButtonPlaceholders()
        }
        Column(Modifier.weight(1f).fillMaxHeight().clipToBounds(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlaceholderShape(Modifier.fillMaxWidth().height(96.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { PlaceholderShape(Modifier.width(140.dp).height(40.dp)) }
            }
            Column(Modifier.testTag(DetailLoadingTags.EPISODE_ROWS), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(EPISODE_ROWS) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, ROW_SHAPE)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlaceholderShape(Modifier.size(64.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlaceholderShape(Modifier.fillMaxWidth(0.6f).height(18.dp))
                            PlaceholderShape(Modifier.fillMaxWidth(0.35f).height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/** The card's cover, or its title placeholder when it has none; a grey square when nothing was handed over. */
@Composable
private fun Cover(itemId: String, preview: PlayerPreview?, size: Dp) {
    when {
        preview == null -> PlaceholderShape(Modifier.size(size))
        preview.coverUrl == null -> CoverImage(itemId, contentDescription = preview.title, size = size, hasCover = false)
        else -> CoverImage(itemId, contentDescription = preview.title, size = size, url = preview.coverUrl)
    }
}

@Composable
private fun Title(preview: PlayerPreview?, maxLines: Int, modifier: Modifier = Modifier) {
    if (preview != null) {
        Text(
            text = preview.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaceholderShape(Modifier.fillMaxWidth().height(22.dp))
            PlaceholderShape(Modifier.fillMaxWidth(0.7f).height(22.dp))
        }
    }
}

/** Where Play/Resume and the next action go. */
@Composable
private fun ButtonPlaceholders() {
    PlaceholderShape(Modifier.fillMaxWidth().height(BUTTON_HEIGHT))
    PlaceholderShape(Modifier.fillMaxWidth(0.7f).height(BUTTON_HEIGHT))
}

/** The TV `Button`'s height. */
private val BUTTON_HEIGHT = 40.dp
private val ROW_SHAPE = RoundedCornerShape(8.dp)
private const val SERIES_COVERS = 6
private const val EPISODE_ROWS = 5
