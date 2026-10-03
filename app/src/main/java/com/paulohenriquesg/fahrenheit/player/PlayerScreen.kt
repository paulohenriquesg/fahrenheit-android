package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.MarqueeText

const val GO_TO_PODCAST_TAG = "player_go_to_podcast"

/**
 * The one player screen, for a book or an episode, in layout C (#107; frame
 * "C…" of docs/mocks/player.html): a large cover with what you read beside
 * it - the series or the show, the title, who wrote and reads it, the chapter
 * playing - and [transport] under them, across the full width.
 *
 * The description moves to the About panel (step 3 of #107).
 */
@Composable
fun PlayerScreen(
    nowPlaying: NowPlaying,
    currentTime: Double,
    transport: @Composable () -> Unit
) {
    var titleFocused by remember { mutableStateOf(false) }
    val spans = remember(nowPlaying.chapters, nowPlaying.trackTotal) {
        ChapterClock.spans(nowPlaying.chapters, nowPlaying.trackTotal ?: 0.0)
    }
    val chapter = ChapterClock.at(spans, currentTime)?.title?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 60.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            CoverImage(itemId = nowPlaying.itemId, contentDescription = nowPlaying.title, size = 250.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nowPlaying.kicker?.let {
                    Text(
                        text = it.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MarqueeText(
                    text = nowPlaying.title,
                    isFocused = titleFocused,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.onFocusChanged { titleFocused = it.isFocused }
                )
                nowPlaying.byline?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                chapter?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        transport()
    }
}

/**
 * "Go to podcast", for an episode: it sits beside the transport, where frame C
 * puts the actions. Leaving for the podcast stops playback (see
 * [PlayerActivity.leaveForPodcast]).
 */
@Composable
fun GoToPodcastButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.testTag(GO_TO_PODCAST_TAG)) {
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
