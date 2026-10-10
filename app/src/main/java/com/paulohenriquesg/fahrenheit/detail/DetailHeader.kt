@file:OptIn(ExperimentalLayoutApi::class)

package com.paulohenriquesg.fahrenheit.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.player.AboutFact
import com.paulohenriquesg.fahrenheit.player.ActionChip
import com.paulohenriquesg.fahrenheit.player.ChapterSpan
import com.paulohenriquesg.fahrenheit.player.ChaptersChip
import com.paulohenriquesg.fahrenheit.player.ChaptersPanel
import com.paulohenriquesg.fahrenheit.player.PlayerPanelHost
import com.paulohenriquesg.fahrenheit.player.SeriesBook
import com.paulohenriquesg.fahrenheit.player.SeriesBooks
import com.paulohenriquesg.fahrenheit.player.rememberPlayerPanels
import com.paulohenriquesg.fahrenheit.podcast.Fact
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.components.BookOverview
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.utils.RichText
import kotlinx.coroutines.launch

const val PRIMARY_ACTION_TAG = "detail_primary_action"
const val DESCRIPTION_TAG = "detail_full_description"

/**
 * A book's screen, as the one book layout ([BookOverview], #134): the book
 * and its actions on the left, the description, the series and the facts on
 * the right. Resume holds focus on arrival.
 *
 * Beside Resume (#105, frame 3): Mark finished, when [finished] is known, and
 * Chapters, when there are any - the player's own panel, opening on the
 * chapter at [at]; choosing one calls [onChapter] with its start.
 *
 * [padding] is the screen's margin, kept inside: the Chapters panel covers the
 * whole screen, edge to edge, as it does over the player.
 *
 * @param nowPlaying what plays, and Stop (#159): beside the cover, above the
 *   description, where it costs the actions under the cover no height.
 */
@Composable
fun BookDetailView(
    itemId: String,
    content: DetailHeaderContent,
    onPrimary: () -> Unit,
    chapters: List<ChapterSpan> = emptyList(),
    at: Double = 0.0,
    onChapter: (Double) -> Unit = {},
    finished: Boolean? = null,
    marking: Boolean = false,
    onMarkFinished: (Boolean) -> Unit = {},
    padding: PaddingValues = PaddingValues(),
    facts: List<AboutFact> = emptyList(),
    series: SeriesBooks? = null,
    seriesName: String? = null,
    onSeriesBook: (SeriesBook) -> Unit = {},
    nowPlaying: @Composable () -> Unit = {}
) {
    val panels = rememberPlayerPanels()
    // Keyed on whether there is a primary action, not its label: the label
    // changes after Mark finished ("Resume at…" to "Play"), and focus must not
    // jump off the button just pressed.
    val initialFocus = rememberInitialFocus(enabled = content.primary != null, itemId, content.primary != null)
    Box(Modifier.fillMaxSize()) {
        BookOverview(
            itemId = itemId,
            title = content.title,
            byline = content.byline,
            description = content.description,
            facts = facts,
            series = series,
            seriesName = seriesName,
            onSeriesBook = onSeriesBook,
            askBeforeSwitching = false,
            landOnDescription = false,
            modifier = Modifier.padding(padding),
            top = nowPlaying
        ) {
            content.primary?.let { label ->
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.focusRequester(initialFocus).testTag(PRIMARY_ACTION_TAG)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            finished?.let { done ->
                ActionChip(
                    text = stringResource(if (done) R.string.mark_unfinished else R.string.mark_finished),
                    onClick = { onMarkFinished(!done) },
                    icon = Icons.Filled.Check,
                    // One mark at a time: a book playing first waits for its closing report.
                    enabled = !marking
                )
            }
            if (chapters.isNotEmpty()) ChaptersChip(panels)
        }
        PlayerPanelHost(panels) {
            ChaptersPanel(chapters, at, onChoose = onChapter, onClose = panels::close)
        }
    }
}

/** Between the description box's focus border and its text, so the border never sits on the words (#170). */
private val DESCRIPTION_PADDING = 12.dp

/**
 * The whole description. Text cannot hold focus, so a description longer than
 * the screen would be unreachable past its first page: when it overflows it
 * becomes focusable, Down scrolls it, and Up at its top goes back to the button.
 *
 * Shared with the player's About panel (#107), which wants focus on it even
 * when it fits: [alwaysFocusable].
 *
 * @param heading opens the text in bold: a title too long for its own place (#194).
 */
@Composable
internal fun FullDescription(
    description: String,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    alwaysFocusable: Boolean = false,
    heading: String? = null
) {
    val headingColour = MaterialTheme.colorScheme.onSurface
    val text = remember(description, heading, headingColour) {
        val body = RichText.fromHtml(description)
        if (heading == null) body else buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = headingColour)) { append(heading) }
            append("\n\n")
            append(body)
        }
    }
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 160.dp.toPx() }
    // Focus scrolls the box in with its own padding and border just out of
    // view; that much hidden is still "at the top".
    val slack = with(LocalDensity.current) { (DESCRIPTION_PADDING + 4.dp).toPx() }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // How much of the description's top is scrolled out of view: its clipped
    // bounds start below where it really starts.
    fun hiddenAbove(): Float = coordinates?.let { it.boundsInWindow().top - it.positionInWindow().y } ?: 0f
    val overflows = scroll.maxValue > 0

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .testTag(DESCRIPTION_TAG)
            .onGloballyPositioned { coordinates = it }
            .padding(DESCRIPTION_PADDING)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> scroll.canScrollForward.also {
                        if (it) scope.launch { scroll.animateScrollBy(step) }
                    }
                    // Above the description's own top there is only the header
                    // again, which focus brings back into view by itself.
                    Key.DirectionUp -> hiddenAbove().let { hidden ->
                        (hidden > slack).also {
                            if (it) scope.launch { scroll.animateScrollBy(-minOf(step, hidden)) }
                        }
                    }
                    else -> false
                }
            }
            .then(if (overflows || alwaysFocusable) Modifier.focusable() else Modifier)
    )
}
