package com.paulohenriquesg.fahrenheit.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * The title of a section reached from the rail. See "Safe area" in
 * docs/ui-style-guide.md: it is the first child of a container padded
 * `Space.screenH` by `Space.gap`, outside any scroller, in every state.
 *
 * [trailing] sits beside it on the same line, such as Library's item count.
 */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Leaves the trailing slot its room when the title is too long.
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
