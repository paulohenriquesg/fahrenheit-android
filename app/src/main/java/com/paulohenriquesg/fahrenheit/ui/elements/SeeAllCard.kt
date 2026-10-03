package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.CardFocus

/**
 * The last card in a Home shelf when the server holds more than the row shows
 * (#123). Sized by [modifier] to match the row's own cards, and focused like
 * them: the 3dp border, nothing grows.
 */
@Composable
fun SeeAllCard(total: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        scale = CardFocus.noGrowth,
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            border = Border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))),
            focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.see_all, total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
