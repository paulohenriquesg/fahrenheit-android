package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.MarqueeText
import com.paulohenriquesg.fahrenheit.utils.RichText

const val GO_TO_PODCAST_TAG = "player_go_to_podcast"

/**
 * The one player screen, for a book or an episode (#73; frames 4 and 4b of
 * docs/mocks/screens.html). What differs is in [NowPlaying]: the line under
 * the title, the chapter marks, and - for an episode - a way to its podcast.
 *
 * @param transport the playback controls and scrubber.
 */
@Composable
fun PlayerScreen(
    nowPlaying: NowPlaying,
    currentTime: Double,
    onGoToPodcast: () -> Unit,
    transport: @Composable () -> Unit
) {
    var titleFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screenH, vertical = Space.gap)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            CoverImage(itemId = nowPlaying.itemId, contentDescription = nowPlaying.title, size = 200.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MarqueeText(
                    text = nowPlaying.title,
                    isFocused = titleFocused,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.onFocusChanged { titleFocused = it.isFocused }
                )
                nowPlaying.subtitle(currentTime).takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (nowPlaying.goToPodcast) {
                    Button(onClick = onGoToPodcast, modifier = Modifier.testTag(GO_TO_PODCAST_TAG)) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Go to podcast")
                    }
                }
                nowPlaying.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = RichText.fromHtml(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Space.gap))
        transport()
    }
}
