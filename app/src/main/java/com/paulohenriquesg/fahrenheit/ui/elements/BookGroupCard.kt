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
 * side by side ("Empty collection" with none), a series up to three fanned
 * (just its name with none). The books come with each group; no new requests.
 *
 * @param bookCount null when the server sent no book list: no count is drawn.
 * @param coverIds the group's books, in order; the first few are drawn.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BookGroupCard(
    name: String,
    bookCount: Int?,
    coverIds: List<String> = emptyList(),
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
            val showsCovers = coverIds.isNotEmpty() || look == BookGroupLook.Collection
            if (showsCovers) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(COVERS_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    GroupCovers(name, coverIds, look)
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

/** The first covers of a group, laid out as the web client does. */
@Composable
private fun GroupCovers(name: String, coverIds: List<String>, look: BookGroupLook) {
    when (look) {
        BookGroupLook.Collection -> {
            if (coverIds.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_collection),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                coverIds.take(2).forEach { id ->
                    Box(Modifier.testTag(CoverTags.GROUP_COVER)) {
                        CoverImage(itemId = id, contentDescription = name, size = SIDE_BY_SIDE_COVER)
                    }
                }
            }
        }
        BookGroupLook.Series -> {
            // A fan: the first book in front, each next one further right
            // and behind it.
            val fanned = coverIds.take(3)
            Box {
                fanned.forEachIndexed { index, id ->
                    Box(
                        Modifier
                            .offset(x = FAN_STEP * index)
                            .zIndex((fanned.size - index).toFloat())
                            .testTag(CoverTags.GROUP_COVER)
                    ) {
                        CoverImage(itemId = id, contentDescription = name, size = FANNED_COVER)
                    }
                }
            }
        }
    }
}

private val CARD_HEIGHT = 260.dp
private val COVERS_HEIGHT = 150.dp
private val SIDE_BY_SIDE_COVER = 84.dp
private val FANNED_COVER = 120.dp
private val FAN_STEP = 24.dp
