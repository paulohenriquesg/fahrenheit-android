package com.paulohenriquesg.fahrenheit.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.podcast.Fact
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.utils.RichText

const val PRIMARY_ACTION_TAG = "detail_primary_action"

/**
 * The top of a book's or a podcast's screen (frame 3 of docs/mocks/screens.html):
 * cover, title, by-line, facts as chips, actions, then the description.
 *
 * The primary action takes focus on arrival, so one press continues listening;
 * nothing on a TV responds to the remote until something holds focus.
 *
 * @param actions drawn after the primary action, e.g. the admin's feed check.
 */
@Composable
fun DetailHeader(
    itemId: String,
    content: DetailHeaderContent,
    onPrimary: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val initialFocus = rememberInitialFocus(enabled = content.primary != null, itemId, content.primary)
    Row {
        CoverImage(itemId = itemId, contentDescription = content.title)
        Spacer(Modifier.width(Space.gap * 2))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            content.byline?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (content.chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    content.chips.forEach { FactChip(it) }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content.primary?.let { label ->
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier
                            .focusRequester(initialFocus)
                            .testTag(PRIMARY_ACTION_TAG)
                    ) {
                        // The phone Icon reads the phone theme's content colour, which is white
                        // on the white focused button; the TV button's own colour follows focus.
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
                actions()
            }
            content.description?.takeIf { it.isNotBlank() }?.let { description ->
                // Rendered, not stripped: emphasis survives (#57).
                val text = remember(description) { RichText.fromHtml(description) }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** One fact about an item, in a pill; [Fact.warn] for the ones worth acting on. */
@Composable
fun FactChip(fact: Fact) {
    val colour = if (fact.warn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = fact.text,
        style = MaterialTheme.typography.bodyMedium,
        color = colour,
        modifier = Modifier
            .border(
                BorderStroke(1.dp, if (fact.warn) colour else MaterialTheme.colorScheme.surfaceVariant),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
