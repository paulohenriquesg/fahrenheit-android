package com.paulohenriquesg.fahrenheit.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.ui.components.DetailHeader
import com.paulohenriquesg.fahrenheit.ui.components.ItemsGrid
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage

@Composable
fun CollectionDetailContent(
    collection: Collection,
    onBookClick: (com.paulohenriquesg.fahrenheit.api.LibraryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val booksCount = collection.books?.size ?: 0

        DetailHeader(
            imageContent = {
                // Show first book's cover as collection image
                collection.books?.firstOrNull()?.id?.let { firstBookId ->
                    CoverImage(
                        itemId = firstBookId,
                        contentDescription = collection.name
                    )
                }
            },
            title = collection.name,
            subtitle = "$booksCount ${if (booksCount == 1) "book" else "books"}",
            description = collection.description
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Books",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ItemsGrid(
            items = collection.books,
            columns = 4,
            onItemClick = onBookClick,
            emptyMessage = "No books found in this collection"
        )
    }
}
