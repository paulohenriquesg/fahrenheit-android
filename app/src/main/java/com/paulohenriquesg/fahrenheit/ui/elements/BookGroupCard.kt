package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.ui.CardFocus

/**
 * A series or a collection on the rail's Series and Collections sections: its
 * books' covers, its name and how many books it holds (#73, #149).
 *
 * The covers follow the server's web client: a collection shows its first two
 * side by side, each with its own placeholder when it has no cover ("Empty
 * collection" with no books); a series fans up to three of the books that have
 * a cover (just its name when none has). The books come with each group, and
 * a book without a cover is not asked for.
 *
 * @param bookCount null when the server sent no book list: no count is drawn.
 * @param books the group's books, in order; the first few are drawn.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BookGroupCard(
    name: String,
    bookCount: Int?,
    books: List<LibraryItem> = emptyList(),
    look: BookGroupLook = BookGroupLook.Series,
    onClick: () -> Unit
) {
    Card(
        scale = CardFocus.noGrowth,
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(CARD_HEIGHT),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val fanned = books.filter { it.media.coverPath != null }.take(FAN_SIZE)
            val showsCovers = look == BookGroupLook.Collection || fanned.isNotEmpty()
            if (showsCovers) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(COVERS_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    when (look) {
                        BookGroupLook.Collection -> SideBySide(books.take(2))
                        BookGroupLook.Series -> Fan(fanned)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = name,
                style = if (showsCovers) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = if (showsCovers) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            bookCount?.let { count ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$count ${if (count == 1) "book" else "books"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class BookGroupLook { Collection, Series }

/**
 * A collection's first two books side by side, as the web client's
 * CollectionCover draws them: each its own cover, or its own placeholder.
 * The covers say nothing to a screen reader; the card says the name.
 */
@Composable
private fun SideBySide(books: List<LibraryItem>) {
    if (books.isEmpty()) {
        Text(
            text = stringResource(R.string.empty_collection),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        books.forEach { book ->
            Box(Modifier.testTag(CoverTags.GROUP_COVER)) {
                CoverImage(
                    itemId = book.id,
                    contentDescription = null,
                    size = SIDE_BY_SIDE_COVER,
                    title = book.media.metadata.title,
                    author = LibraryItemDisplay.author(book),
                    hasCover = book.media.coverPath != null
                )
            }
        }
    }
}

/**
 * A series' covers fanned, the first in front and each next one further right
 * and behind it. The box is as wide as the fan, so it centres and nothing
 * runs past the card.
 */
@Composable
private fun Fan(books: List<LibraryItem>) {
    Box(Modifier.width(FANNED_COVER + FAN_STEP * (books.size - 1))) {
        books.forEachIndexed { index, book ->
            Box(
                Modifier
                    .offset(x = FAN_STEP * index)
                    .zIndex((books.size - index).toFloat())
                    .testTag(CoverTags.GROUP_COVER)
            ) {
                CoverImage(itemId = book.id, contentDescription = null, size = FANNED_COVER)
            }
        }
    }
}

private val CARD_HEIGHT = 260.dp
private val COVERS_HEIGHT = 150.dp
private val SIDE_BY_SIDE_COVER = 84.dp
private val FANNED_COVER = 120.dp
private val FAN_STEP = 24.dp
private const val FAN_SIZE = 3
