package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithCache
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
 * Behind it all, [wash]: the cover's colour (see [CoverWash]), or nothing.
 * Over it all, [overlay]: an open panel ([SidePanel]).
 *
 * The description moves to the About panel (step 3 of #107).
 */
@Composable
fun PlayerScreen(
    nowPlaying: NowPlaying,
    currentTime: Double,
    wash: Color? = null,
    transport: @Composable () -> Unit,
    overlay: @Composable () -> Unit = {}
) {
    var titleFocused by remember { mutableStateOf(false) }
    val spans = remember(nowPlaying.chapters, nowPlaying.trackTotal) {
        ChapterClock.spans(nowPlaying.chapters, nowPlaying.trackTotal ?: 0.0)
    }
    val chapter = ChapterClock.at(spans, currentTime)?.title?.takeIf { it.isNotBlank() }

    val washed = Modifier.coverWash(wash)
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(washed)
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
                    EpisodeExtras(nowPlaying)
                }
            }
            Spacer(Modifier.height(28.dp))
            transport()
            // What Next plays, so the button is not a guess (#108).
            nowPlaying.next?.let { next ->
                Text(
                    text = next.length?.let { stringResource(R.string.up_next, next.title, PlaybackPosition.spoken(it)) }
                        ?: stringResource(R.string.up_next_title, next.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }
        // An open panel, over everything (frame "A, with a panel open").
        overlay()
    }
}

/**
 * Under an episode's title (#108): a badge when it is not a regular one, its
 * details line, and the first three lines of its notes. A book has none.
 */
@Composable
private fun EpisodeExtras(nowPlaying: NowPlaying) {
    if (nowPlaying.badge != null || nowPlaying.details != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            nowPlaying.badge?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            nowPlaying.details?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    nowPlaying.notes?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * "Go to podcast", for an episode: it sits beside the transport, where frame C
 * puts the actions. The episode plays on, as after Back (see
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

/**
 * The cover's colour behind a screen, fading into the background (frame C;
 * see [CoverWash]); the background alone when there is none, or under the
 * light theme. A colour that arrives after the screen showed fades in.
 * The player and its About (#134) share it.
 */
@Composable
fun Modifier.coverWash(wash: Color?): Modifier {
    val background = MaterialTheme.colorScheme.background
    val shown by animateColorAsState(
        wash?.takeIf { CoverWash.appliesOn(background) } ?: background,
        animationSpec = tween(700),
        label = "wash"
    )
    return drawWithCache {
        val glow = Brush.radialGradient(
            listOf(shown, Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.30f),
            radius = maxOf(size.width, size.height) * 0.75f
        )
        val fade = Brush.verticalGradient(listOf(shown.copy(alpha = 0.55f), background), endY = size.height * 0.75f)
        onDrawBehind {
            drawRect(fade)
            drawRect(glow)
        }
    }
}
