package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.ui.components.BookOverview
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import com.paulohenriquesg.fahrenheit.utils.RichText

const val PODCAST_LEFT_TAG = "podcast_left"
const val PODCAST_PRIMARY_TAG = "podcast_primary"
const val PODCAST_FACTS_TAG = "podcast_facts"
const val PODCAST_DOWNLOADS_TAG = "podcast_downloads"
const val PODCAST_DESCRIPTION_TAG = "podcast_description"

/** The book page's left column (`BookOverview`), so the two screens line up. */
private val COLUMN_WIDTH = 180.dp
private val COLUMN_GAP = 40.dp
private val DESCRIPTION_SHAPE = RoundedCornerShape(12.dp)
private val DESCRIPTION_FADE = 32.dp

/**
 * A podcast's page (#205) in the book page's layout: a fixed left column with
 * the cover, the title, the actions stacked and the facts at its foot; on the
 * right one list that scrolls - Now playing and the description, then the
 * tabs, which stay pinned, then the episodes.
 *
 * Stateless but for what is the UI's own (#208): the Downloads panel and the
 * full description being open, and focus.
 *
 * @param margin the screen's margin, kept inside: the panel and the full
 *   description cover the whole screen.
 */
@OptIn(ExperimentalComposeUiApi::class) // focusProperties.exit
@Composable
fun PodcastPage(
    state: PodcastUiState,
    margin: PaddingValues,
    onPrimary: () -> Unit,
    onTab: (EpisodeTab) -> Unit,
    onPlay: (Episode) -> Unit,
    onDownload: (EpisodeRow) -> Unit,
    onMark: (Episode, Boolean) -> Unit,
    onCheckFeed: () -> Unit,
    onChangeDownloads: (DownloadChange) -> Unit,
    onDownloadsClosed: () -> Unit,
    onFavourite: (Episode) -> Unit = {},
    date: (EpisodeRow) -> String = { "" },
    nowPlaying: @Composable () -> Unit = {}
) {
    // Keyed on whether there is a primary action, not its label: the label
    // changes as episodes are heard, and focus must not jump onto it.
    val hasPrimary = state.primary != null
    val primaryFocus = rememberInitialFocus(enabled = hasPrimary, state.itemId, hasPrimary)
    var downloadsOpen by remember { mutableStateOf(false) }
    var downloadsOpened by remember { mutableStateOf(false) }
    val downloadsButton = remember { FocusRequester() }
    var aboutOpen by remember { mutableStateOf(false) }
    var aboutOpened by remember { mutableStateOf(false) }
    val descriptionCard = remember { FocusRequester() }
    // Back to what opened them.
    LaunchedEffect(downloadsOpen) {
        if (!downloadsOpen && downloadsOpened) downloadsButton.requestFocusWhenAttached()
    }
    LaunchedEffect(aboutOpen) {
        if (!aboutOpen && aboutOpened) descriptionCard.requestFocusWhenAttached()
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().padding(margin), horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP)) {
            LeftColumn(
                state = state,
                primary = Modifier.focusRequester(primaryFocus),
                downloads = Modifier.focusRequester(downloadsButton),
                onPrimary = onPrimary,
                onCheckFeed = onCheckFeed,
                onDownloads = { downloadsOpen = true; downloadsOpened = true }
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Left out of the list lands on the primary, from any row:
                    // Resume is always one Left away.
                    .focusProperties {
                        exit = { direction ->
                            if (direction == FocusDirection.Left && hasPrimary) primaryFocus else FocusRequester.Default
                        }
                    }
                    .focusGroup()
            ) {
                PodcastEpisodesView(
                    screen = state.screen,
                    tab = state.tab,
                    onTab = onTab,
                    downloads = state.downloads,
                    onPlay = onPlay,
                    onDownload = onDownload,
                    progress = state.progress,
                    onMark = onMark,
                    coverItemId = state.itemId,
                    date = date,
                    focusFirstRow = !hasPrimary,
                    rowsLeft = if (hasPrimary) primaryFocus else FocusRequester.Default,
                    hearts = state.favouritesPlaylist?.let { name ->
                        // Kept while nothing it shows changes, so the rows are not redrawn on every poll.
                        remember(name, state.favourites, onFavourite) { EpisodeHearts(name, state.favourites, onFavourite) }
                    },
                    top = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            nowPlaying()
                            state.description?.takeIf { it.isNotBlank() }?.let {
                                DescriptionCard(
                                    it,
                                    onOpen = { aboutOpen = true; aboutOpened = true },
                                    modifier = Modifier.focusRequester(descriptionCard)
                                )
                            }
                        }
                    }
                )
            }
        }
        if (downloadsOpen && state.autoDownloads != null) {
            DownloadsPanel(
                settings = state.autoDownloads,
                failed = state.downloadsFailed,
                onChange = onChangeDownloads,
                onClose = {
                    downloadsOpen = false
                    onDownloadsClosed()
                }
            )
        }
        if (aboutOpen) {
            FullScreenDescription(state, margin, onClose = { aboutOpen = false })
        }
    }
}

@Composable
private fun LeftColumn(
    state: PodcastUiState,
    primary: Modifier,
    downloads: Modifier,
    onPrimary: () -> Unit,
    onCheckFeed: () -> Unit,
    onDownloads: () -> Unit
) {
    Column(Modifier.width(COLUMN_WIDTH).fillMaxHeight().testTag(PODCAST_LEFT_TAG)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // The cover gives way, never an action: three buttons, two of them two
            // lines, under a long title do not fit beside a whole 180dp cover.
            BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                CoverImage(itemId = state.itemId, contentDescription = state.title, size = min(maxWidth, maxHeight))
            }
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                state.byline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            state.primary?.let { label ->
                Button(onClick = onPrimary, modifier = primary.testTag(PODCAST_PRIMARY_TAG)) {
                    // The phone Icon reads the phone theme's content colour; the TV button's follows focus.
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    // An episode title can be any length.
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            state.feedCheck?.let { FeedCheckButton(state = it, onCheck = onCheckFeed) }
            state.autoDownloads?.let { settings ->
                Button(onClick = onDownloads, modifier = downloads.testTag(PODCAST_DOWNLOADS_TAG)) {
                    Icon(Icons.Outlined.Download, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Automatic downloads", maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            settings.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Column(
            Modifier.padding(top = 10.dp).testTag(PODCAST_FACTS_TAG),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.screen.facts.forEach { fact ->
                Text(
                    fact.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fact.warn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A few lines of the description, fading out where there is more; Center opens all of it. */
@Composable
private fun DescriptionCard(description: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    // Rendered, not stripped: emphasis survives (#57).
    val text = remember(description) { RichText.fromHtml(description) }
    var cut by remember(description) { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().testTag(PODCAST_DESCRIPTION_TAG),
        shape = ClickableSurfaceDefaults.shape(DESCRIPTION_SHAPE),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Clip,
            onTextLayout = { cut = it.hasVisualOverflow },
            modifier = Modifier
                .padding(16.dp)
                // Fades out rather than cutting mid-line, which read as clipped (#178).
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (cut) {
                        val from = size.height - DESCRIPTION_FADE.toPx()
                        drawRect(
                            Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = from, endY = size.height),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        )
    }
}

/**
 * The whole description, full screen, in the book layout as About is: Back
 * returns to the page, and focus cannot wander to the page underneath.
 */
@OptIn(ExperimentalComposeUiApi::class) // focusProperties.exit
@Composable
private fun FullScreenDescription(state: PodcastUiState, margin: PaddingValues, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup()
            .padding(margin)
    ) {
        BookOverview(
            itemId = state.itemId,
            title = state.title,
            byline = state.byline,
            description = state.description,
            facts = emptyList(),
            series = null,
            seriesName = null,
            onSeriesBook = {},
            askBeforeSwitching = false,
            landOnDescription = true
        ) {}
    }
}
