package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.paulohenriquesg.fahrenheit.api.Author

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuthorCard(author: Author, onClick: (Author) -> Unit) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .height(300.dp)
    ) {
        Card(
            onClick = { onClick(author) },
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
                AuthorImage(
                    authorId = author.id,
                    imagePath = author.imagePath,
                    contentDescription = author.name
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                author.numBooks?.let { numBooks ->
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
