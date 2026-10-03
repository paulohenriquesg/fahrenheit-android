package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * A shelf's heading on Home, at the mock's size (docs/mocks/screens.html,
 * `.shelf h4`): 16 sp bold. At the 24 sp headline it was, the heading
 * outweighed the covers it names and cost height on a 540dp screen.
 */
@Composable
fun ShelfHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
