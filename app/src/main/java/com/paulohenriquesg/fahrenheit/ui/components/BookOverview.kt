package com.paulohenriquesg.fahrenheit.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.detail.FullDescription
import com.paulohenriquesg.fahrenheit.player.AboutFact
import com.paulohenriquesg.fahrenheit.player.SeriesBook
import com.paulohenriquesg.fahrenheit.player.SeriesBooks
import com.paulohenriquesg.fahrenheit.player.factLabel
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached

const val DESCRIPTION_BOX_TAG = "book_description_box"

/**
 * One book, set out for reading about it (#134; option 1 of
 * docs/mocks/book-screen.html): the book screen and the player's About.
 *
 * On the left the cover, the title, who wrote and reads it, and [actions].
 * On the right the description, in a wide box that scrolls with Down, and
 * under it the series - with this book marked and in view - beside the facts.
 *
 * @param askBeforeSwitching from the player, choosing another book of the
 *   series asks "Play <title> instead?" first; from the book screen it opens it.
 * @param landOnDescription focus lands on the description (About); otherwise
 *   the caller lands it on an action (the book screen's Resume).
 * @param top above the description: the book screen's Now playing (#159).
 */
@Composable
fun BookOverview(
    itemId: String,
    title: String,
    byline: String?,
    description: String?,
    facts: List<AboutFact>,
    series: SeriesBooks?,
    seriesName: String?,
    onSeriesBook: (SeriesBook) -> Unit,
    askBeforeSwitching: Boolean,
    landOnDescription: Boolean,
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit
) {
    val text = description?.takeIf { it.isNotBlank() }
    val descriptionFocus = rememberInitialFocus(enabled = landOnDescription && text != null)
    val thisBook = remember { FocusRequester() }
    val row = series?.takeIf { it.total > 1 }
    Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(
            Modifier
                .width(180.dp)
                // Right from the actions goes to the description: they sit under
                // the cover, below the description's box, where the remote's own
                // search would find the series row instead.
                .onPreviewKeyEvent { event ->
                    val toDescription = text != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight
                    if (toDescription) runCatching { descriptionFocus.requestFocus() }.isSuccess else false
                },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Room for three actions under a long title, at a larger font too.
            CoverImage(itemId = itemId, contentDescription = title, size = 180.dp)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            byline?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            actions()
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            top()
            text?.let {
                val scroll = rememberScrollState()
                Box(
                    Modifier
                        .fillMaxWidth()
                        // Gives way to what is above and below it: it scrolls.
                        .weight(1f, fill = false)
                        .heightIn(max = 230.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        // Down past the end of the description goes to this book in
                        // the series, not whichever cover sits under its middle.
                        .onPreviewKeyEvent { event ->
                            val toThisBook = row != null && event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionDown && !scroll.canScrollForward
                            if (toThisBook) runCatching { thisBook.requestFocus() }.isSuccess else false
                        }
                        .verticalScroll(scroll)
                        .testTag(DESCRIPTION_BOX_TAG)
                        .padding(12.dp)
                ) {
                    FullDescription(
                        it, scroll,
                        modifier = Modifier.focusRequester(descriptionFocus),
                        alwaysFocusable = true
                    )
                }
            }
            if (row != null || facts.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    row?.let {
                        Column(Modifier.weight(1f)) {
                            SeriesShelf(
                                series = it,
                                label = stringResource(R.string.series_count, seriesName ?: stringResource(R.string.about_series), it.total),
                                askBeforeSwitching = askBeforeSwitching,
                                onChoose = onSeriesBook,
                                landHere = Modifier.focusRequester(thisBook),
                                coverSize = 84.dp,
                                titles = true
                            )
                        }
                    }
                    if (facts.isNotEmpty()) FactsGrid(facts, Modifier.width(280.dp))
                }
            }
        }
    }
}

/**
 * A series as covers, this book marked and in view; choosing another opens
 * it, or asks "Play <title> instead?" first ([askBeforeSwitching]), where Back
 * or Cancel answers no and returns to that cover.
 *
 * @param landHere applied to this book's cover, for when focus lands on the row.
 */
@Composable
fun SeriesShelf(
    series: SeriesBooks,
    label: String,
    askBeforeSwitching: Boolean,
    onChoose: (SeriesBook) -> Unit,
    landHere: Modifier = Modifier,
    coverSize: Dp = 56.dp,
    titles: Boolean = false
) {
    var asking by remember { mutableStateOf<SeriesBook?>(null) }
    val covers = remember(series) { series.books.associate { it.itemId to FocusRequester() } }
    var returnTo by remember { mutableStateOf<SeriesBook?>(null) }
    LaunchedEffect(asking) {
        if (asking == null) returnTo?.let { covers[it.itemId]?.requestFocusWhenAttached() }
    }
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
    val question = asking
    if (question != null) {
        // Back answers the question, before it can close what holds it.
        BackHandler { returnTo = question; asking = null }
        val play = rememberInitialFocus(enabled = true, question)
        Text(stringResource(R.string.play_instead, question.title), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { onChoose(question) }, modifier = Modifier.focusRequester(play)) {
                Text(stringResource(R.string.play))
            }
            OutlinedButton(onClick = { returnTo = question; asking = null }) { Text(stringResource(R.string.cancel)) }
        }
        return
    }
    val keys = remember(series) { StableKeys.of(series.books) { it.itemId } }
    // Opens with this book in view, the one before it beside it for context.
    val state = rememberLazyListState(initialFirstVisibleItemIndex = ((series.current ?: 0) - 1).coerceAtLeast(0))
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        itemsIndexed(series.books, key = { index, _ -> keys[index] }) { index, book ->
            val isThis = index == (series.current ?: 0)
            Column(Modifier.width(coverSize)) {
                Surface(
                    onClick = {
                        when {
                            index == series.current -> Unit
                            askBeforeSwitching -> asking = book
                            else -> onChoose(book)
                        }
                    },
                    modifier = Modifier
                        .focusRequester(covers.getValue(book.itemId))
                        .then(if (isThis) landHere else Modifier)
                        .semantics { selected = index == series.current },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    border = ClickableSurfaceDefaults.border(
                        border = if (index == series.current) Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) else Border.None,
                        focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface))
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                ) {
                    CoverImage(itemId = book.itemId, contentDescription = book.title, size = coverSize)
                }
                if (titles) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == series.current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/** The facts as a list of labels and values (the mock's two columns). */
@Composable
private fun FactsGrid(facts: List<AboutFact>, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEach { fact ->
            Row {
                Text(
                    factLabel(fact.kind),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    fact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
