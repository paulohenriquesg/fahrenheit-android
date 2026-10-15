package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Narrator
import com.paulohenriquesg.fahrenheit.author.PersonPhoto
import com.paulohenriquesg.fahrenheit.author.count
import com.paulohenriquesg.fahrenheit.ui.CardFocus

/** A narrator in the Narrators grid (#230): as an author's card, with initials for the photo the server does not keep. */
@Composable
fun NarratorCard(narrator: Narrator, onClick: () -> Unit) {
    Card(
        scale = CardFocus.noGrowth,
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(240.dp),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersonPhoto(narrator.name, photoUrl = null, size = 120.dp)
            Text(
                text = narrator.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            narrator.numBooks?.let { books ->
                Text(
                    text = count(R.plurals.person_books, books),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
