package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.LibraryItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryItemCard(item: LibraryItem, onClick: (LibraryItem) -> Unit) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .height(300.dp)
    ) {
        Card(
            onClick = { onClick(item) },
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
                CoverImage(
                    itemId = item.id,
                    contentDescription = item.media?.metadata?.title ?: "Cover"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.media?.metadata?.title ?: item.id,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (item.mediaType == "podcast" && item.numEpisodesIncomplete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp) // Set a fixed size for the badge
                    .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center // Center the text within the badge
            ) {
                Text(
                    text = if (item.numEpisodesIncomplete > 99) "99+" else item.numEpisodesIncomplete.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}