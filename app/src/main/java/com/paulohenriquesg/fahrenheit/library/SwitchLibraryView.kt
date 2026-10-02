package com.paulohenriquesg.fahrenheit.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.StableKeys

/**
 * Picking a library is a big, rare choice, so it gets tiles rather than a list:
 * what is in it, and which one you are in.
 */
@Composable
fun SwitchLibraryView(
    libraries: List<Library>,
    currentId: String?,
    onSelect: (Library) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        Text(
            text = stringResource(R.string.switch_library),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (libraries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.switch_library_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(Space.gap),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            val keys = StableKeys.of(libraries) { it.id ?: it.name.orEmpty() }
            items(libraries.size, key = { keys[it] }) { index ->
                val library = libraries[index]
                val current = library.id != null && library.id == currentId
                Card(
                    onClick = { onSelect(library) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("library_${library.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Space.inset),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = library.name.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = library.mediaType.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (current) {
                            Text(
                                text = stringResource(R.string.switch_library_current),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("library_${library.id}_current")
                            )
                        }
                    }
                }
            }
        }
    }
}
