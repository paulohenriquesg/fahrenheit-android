package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.Series

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesCard(series: Series, onClick: (Series) -> Unit) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .height(300.dp)
    ) {
        Card(
            onClick = { onClick(series) },
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Show the first book's cover if available
                series.books?.firstOrNull()?.id?.let { firstBookId ->
                    CoverImage(
                        itemId = firstBookId,
                        contentDescription = series.name
                    )
                } ?: run {
                    // Show placeholder if no books
                    PlaceholderImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                series.books?.size?.let { numBooks ->
                    Text(
                        text = "$numBooks ${if (numBooks == 1) "book" else "books"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
