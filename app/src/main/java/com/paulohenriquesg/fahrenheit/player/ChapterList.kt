package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus

/**
 * A book's chapters with where each starts, the one at [at] marked, focused
 * and scrolled to (#107; the list #105 wants on the details screen too).
 */
@Composable
fun ChapterList(spans: List<ChapterSpan>, at: Double, onChoose: (ChapterSpan) -> Unit, modifier: Modifier = Modifier) {
    val current = ChapterClock.at(spans, at)?.let(spans::indexOf)?.coerceAtLeast(0) ?: 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = current)
    val landing = rememberInitialFocus(enabled = true)
    // A chapter is its place in the book: titles repeat, and so can starts.
    LazyColumn(modifier = modifier, state = state) {
        itemsIndexed(spans, key = { index, _ -> index }) { index, span ->
            PanelOption(
                label = span.title.ifBlank { stringResource(R.string.chapter_number, index + 1) },
                detail = PlaybackPosition.spoken(span.start),
                selected = index == current,
                onClick = { onChoose(span) },
                modifier = if (index == current) Modifier.focusRequester(landing) else Modifier
            )
        }
    }
}

/** The Chapters panel: choosing one plays from its start, in whole-book seconds. */
@Composable
fun ChaptersPanel(spans: List<ChapterSpan>, at: Double, onChoose: (Double) -> Unit, onClose: () -> Unit) {
    SidePanel(stringResource(R.string.chapters), onClose) {
        ChapterList(
            spans = spans,
            at = at,
            onChoose = { onChoose(it.start); onClose() },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ChaptersChip(panels: PlayerPanels) = ActionChip(
    text = stringResource(R.string.chapters),
    onClick = { panels.open(PlayerPanel.Chapters) },
    modifier = Modifier.focusRequester(panels.opener(PlayerPanel.Chapters)),
    icon = Icons.AutoMirrored.Filled.List
)
