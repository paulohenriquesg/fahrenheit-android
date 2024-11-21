package com.paulohenriquesg.fahrenheit.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemCard

@Composable
fun LibraryItemsRow(libraryItems: List<LibraryItem>, listState: LazyListState) {
    val context = LocalContext.current

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        items(libraryItems) { item ->
            LibraryItemCard(item) { clickedItem ->
                val intent = DetailActivity.createIntent(context, clickedItem.id)
                context.startActivity(intent)
            }
        }
    }
}