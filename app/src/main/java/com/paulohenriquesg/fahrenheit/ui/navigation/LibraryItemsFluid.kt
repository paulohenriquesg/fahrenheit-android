package com.paulohenriquesg.fahrenheit.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryItemsFluid(libraryItems: List<LibraryItem>) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        items(libraryItems.size) { index ->
            val item = libraryItems[index]

            LibraryItemCard(
                item = item,
                onClick = { clickedItem ->
                    val intent = DetailActivity.createIntent(context, clickedItem.id)
                    context.startActivity(intent)
                }
            )
        }
    }
}