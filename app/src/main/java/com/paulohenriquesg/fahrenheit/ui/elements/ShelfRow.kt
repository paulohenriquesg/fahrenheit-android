package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf

@Composable
fun ShelfRow(
    shelf: Shelf,
    progress: CoverProgress = CoverProgress.None,
    seeAllTotal: Int? = null,
    onSeeAll: () -> Unit = {},
    onItemLongClick: ((LibraryItem) -> Unit)? = null,
    onItemClick: (LibraryItem) -> Unit
) {
    val keys = shelf.entities?.let { entities -> StableKeys.of(entities) { e -> e.id } }
    val row = rememberLazyListState()
    var holdsFocus by remember { mutableStateOf(false) }
    // A reload that puts something new first (#197): the keyed row would keep
    // the old first card in view and leave the new one off-screen to its left.
    // A row focus is in keeps its focused card instead.
    LaunchedEffect(keys?.firstOrNull()) {
        if (!holdsFocus) row.scrollToItem(0)
    }
    Column {
        ShelfHeading(shelf.label)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            state = row,
            modifier = Modifier.onFocusChanged { holdsFocus = it.hasFocus },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            shelf.entities?.let { entities ->
                items(entities.size, key = { keys!![it] }) { index ->
                    val item = entities[index]
                    LibraryItemCard(item = item, progress = progress.of(item), finished = progress.finished(item), onLongClick = onItemLongClick, onClick = onItemClick)
                }
            }
            if (seeAllTotal != null) {
                // Last, after everything the server sent (#123).
                item(key = "see-all") {
                    SeeAllCard(seeAllTotal, onSeeAll, Modifier.padding(8.dp).width(200.dp).height(300.dp))
                }
            }
        }
    }
}
