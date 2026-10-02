package com.paulohenriquesg.fahrenheit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage

/**
 * A named group of books: a series or a collection.
 *
 * The two screens showing them were the same file with the words swapped - the
 * diff was empty once "series" and "collection" were neutralised - so they are
 * one screen taking this.
 */
data class BookGroup(
    val name: String,
    val description: String?,
    val books: List<LibraryItem>?
) {
    companion object {
        fun of(series: Series) = BookGroup(series.name, series.description, series.books)
        fun of(collection: Collection) =
            BookGroup(collection.name, collection.description, collection.books)
    }
}

@Composable
fun BookGroupDetailContent(
    group: BookGroup,
    emptyMessage: String,
    onBookClick: (LibraryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val booksCount = group.books?.size ?: 0

        DetailHeader(
            imageContent = {
                // The first book's cover stands for the group; neither a series
                // nor a collection has artwork of its own on the server.
                group.books?.firstOrNull()?.id?.let { firstBookId ->
                    CoverImage(itemId = firstBookId, contentDescription = group.name)
                }
            },
            title = group.name,
            subtitle = "$booksCount ${if (booksCount == 1) "book" else "books"}",
            description = group.description
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.books),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ItemsGrid(
            items = group.books,
            columns = 4,
            onItemClick = onBookClick,
            emptyMessage = emptyMessage
        )
    }
}
