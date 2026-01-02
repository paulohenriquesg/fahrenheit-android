package com.paulohenriquesg.fahrenheit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemCard

/**
 * Shared header component for detail screens (book/podcast, author, series).
 * Displays an image on the left and information column on the right.
 *
 * @param imageContent Composable for the image (cover, author photo, etc.)
 * @param title Main title text
 * @param subtitle Secondary information (e.g., "5 books", "Author")
 * @param description Optional description text
 * @param imageSize Size of the image (default 200.dp)
 */
@Composable
fun DetailHeader(
    imageContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    description: String? = null,
    imageSize: Dp = 200.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        imageContent()

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.height(imageSize)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Shared grid component for displaying library items.
 * Used in author detail and series detail screens.
 *
 * @param items List of library items to display
 * @param columns Number of grid columns (default 4)
 * @param onItemClick Callback when an item is clicked
 * @param emptyMessage Message to show when list is empty
 */
@Composable
fun ItemsGrid(
    items: List<LibraryItem>?,
    columns: Int = 4,
    onItemClick: (LibraryItem) -> Unit,
    emptyMessage: String = "No items found"
) {
    items?.let { itemList ->
        if (itemList.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(itemList) { item ->
                    LibraryItemCard(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        } else {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
