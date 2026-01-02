package com.paulohenriquesg.fahrenheit.series

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
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.ui.components.DetailHeader
import com.paulohenriquesg.fahrenheit.ui.components.ItemsGrid
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage

@Composable
fun SeriesDetailContent(
    series: Series,
    onBookClick: (com.paulohenriquesg.fahrenheit.api.LibraryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val booksCount = series.books?.size ?: 0

        DetailHeader(
            imageContent = {
                // Show first book's cover as series image
                series.books?.firstOrNull()?.id?.let { firstBookId ->
                    CoverImage(
                        itemId = firstBookId,
                        contentDescription = series.name
                    )
                }
            },
            title = series.name,
            subtitle = "$booksCount ${if (booksCount == 1) "book" else "books"}",
            description = series.description
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Books",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ItemsGrid(
            items = series.books,
            columns = 4,
            onItemClick = onBookClick,
            emptyMessage = "No books found in this series"
        )
    }
}
