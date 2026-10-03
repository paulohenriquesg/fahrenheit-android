@file:OptIn(ExperimentalLayoutApi::class)

package com.paulohenriquesg.fahrenheit.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.podcast.Fact
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.utils.RichText

const val PRIMARY_ACTION_TAG = "detail_primary_action"
const val DESCRIPTION_TAG = "detail_full_description"
const val DETAIL_SCROLL_TAG = "detail_scroll"

/**
 * A book's screen: the header with the whole description, scrolling when the
 * description is longer than the screen. The room under the button is free on
 * a book, unlike a podcast, where the episodes follow.
 */
@Composable
fun BookDetailView(itemId: String, content: DetailHeaderContent, onPrimary: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .testTag(DETAIL_SCROLL_TAG)
    ) {
        DetailHeader(itemId = itemId, content = content, onPrimary = onPrimary, fullDescription = scroll)
    }
}

/**
 * The top of a book's or a podcast's screen (frame 3 of docs/mocks/screens.html):
 * cover, title, by-line, facts as chips, actions, then the description.
 *
 * The primary action takes focus on arrival, so one press continues listening;
 * nothing on a TV responds to the remote until something holds focus.
 *
 * @param actions drawn after the primary action, e.g. the admin's feed check.
 * @param fullDescription the scroll holding the header, to show the whole
 *   description in it; null for a three-line preview.
 */
@Composable
fun DetailHeader(
    itemId: String,
    content: DetailHeaderContent,
    onPrimary: () -> Unit,
    actions: @Composable () -> Unit = {},
    fullDescription: ScrollState? = null
) {
    val initialFocus = rememberInitialFocus(enabled = content.primary != null, itemId, content.primary)
    Row {
        CoverImage(itemId = itemId, contentDescription = content.title)
        Spacer(Modifier.width(Space.gap * 2))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            content.byline?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (content.chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    content.chips.forEach { FactChip(it) }
                }
            }
            // Wraps rather than squeezing: in a plain Row a long primary label
            // left the actions beside it zero width, and their text wrapped one
            // letter per line - a header taller than the screen.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.gap),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content.primary?.let { label ->
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .focusRequester(initialFocus)
                            .testTag(PRIMARY_ACTION_TAG)
                    ) {
                        // The phone Icon reads the phone theme's content colour, which is white
                        // on the white focused button; the TV button's own colour follows focus.
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        // An episode title can be any length.
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                actions()
            }
            content.description?.takeIf { it.isNotBlank() }?.let { description ->
                if (fullDescription != null) {
                    FullDescription(description, fullDescription)
                } else {
                    // Rendered, not stripped: emphasis survives (#57).
                    val text = remember(description) { RichText.fromHtml(DetailHeaderModel.previewOf(description)) }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * The whole description. Text cannot hold focus, so a description longer than
 * the screen would be unreachable past its first page: when it overflows it
 * becomes focusable, Down scrolls it, and Up at its top goes back to the button.
 *
 * Shared with the player's About panel (#107), which wants focus on it even
 * when it fits: [alwaysFocusable].
 */
@Composable
internal fun FullDescription(
    description: String,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    alwaysFocusable: Boolean = false
) {
    val text = remember(description) { RichText.fromHtml(description) }
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 160.dp.toPx() }
    // Focus scrolls the box in with its own padding and border just out of
    // view; that much hidden is still "at the top".
    val slack = with(LocalDensity.current) { 8.dp.toPx() }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // How much of the description's top is scrolled out of view: its clipped
    // bounds start below where it really starts.
    fun hiddenAbove(): Float = coordinates?.let { it.boundsInWindow().top - it.positionInWindow().y } ?: 0f
    var focused by remember { mutableStateOf(false) }
    val overflows = scroll.maxValue > 0

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .testTag(DESCRIPTION_TAG)
            .onGloballyPositioned { coordinates = it }
            .border(
                BorderStroke(
                    if (focused) 3.dp else 0.dp,
                    if (focused) MaterialTheme.colorScheme.primary else Color.Transparent
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(4.dp)
            .onFocusChanged { focused = it.isFocused }
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

/** One fact about an item, in a pill; [Fact.warn] for the ones worth acting on. */
@Composable
fun FactChip(fact: Fact) {
    val colour = if (fact.warn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = fact.text,
        style = MaterialTheme.typography.bodyMedium,
        color = colour,
        modifier = Modifier
            .border(
                BorderStroke(1.dp, if (fact.warn) colour else MaterialTheme.colorScheme.surfaceVariant),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
