package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.BorderStroke
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.IconButton as TvIconButton
import androidx.tv.material3.IconButtonDefaults as TvIconButtonDefaults
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.LocalContentColor as TvLocalContentColor
import androidx.compose.runtime.CompositionLocalProvider

import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import androidx.compose.ui.focus.focusRequester
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import kotlinx.coroutines.delay

/**
 * The transport: skip, play/pause, stop, scrubber and times, for whatever
 * [player] is playing, in whole-book time (#16).
 *
 * It holds no playback of its own. The player is the screen's MediaController,
 * so this draws what the service is doing and sends it commands; positions
 * cross file boundaries through [playback].
 */
const val CHAPTER_BAR_TAG = "player_chapter_bar"
const val BOOK_BAR_TAG = "player_book_bar"

@Composable
fun MediaPlayerController(
    player: Player,
    playback: BookPlayback,
    totalTime: Double,
    chapters: List<Chapter>? = null,
    onCurrentTimeUpdate: (Double) -> Unit = {},
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    var isPlaying by remember(player) { mutableStateOf(player.playWhenReady) }
    var failed by remember(player) { mutableStateOf(player.playerError != null) }
    var currentTime by remember(player) { mutableDoubleStateOf(playback.bookPosition()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            // Play/pause follows what was asked for, so it answers a press at
            // once rather than after buffering.
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { isPlaying = playWhenReady }
            override fun onPlayerErrorChanged(error: PlaybackException?) { failed = error != null }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Polls only while playing, as before: a loop that never ends keeps
    // Compose from ever being idle, which also hangs its UI tests.
    LaunchedEffect(player, isPlaying) {
        currentTime = playback.bookPosition()
        onCurrentTimeUpdate(currentTime)
        while (isPlaying) {
            delay(1000L)
            currentTime = playback.bookPosition()
            onCurrentTimeUpdate(currentTime)
        }
    }

    fun seekTo(seconds: Double) {
        playback.seekToBookTime(seconds)
        currentTime = seconds
        onCurrentTimeUpdate(seconds)
    }

    val spans = remember(chapters, totalTime) { ChapterClock.spans(chapters, totalTime) }
    val chapter = ChapterClock.at(spans, currentTime)

    // Something must hold focus or no D-pad key reaches this screen at all;
    // play, so the remote's centre button does the obvious thing (frame C).
    val playFocus = rememberInitialFocus(enabled = true, player)

    Column(modifier = Modifier.fillMaxWidth()) {
        // The bar you seek with is the chapter's; the book's sits under it
        // (#107). A book without chapters, or an episode, has the one bar.
        PlaybackBar(
            fraction = chapter?.fraction(currentTime) ?: PlaybackPosition.fraction(currentTime, totalTime),
            modifier = Modifier.fillMaxWidth().testTag(CHAPTER_BAR_TAG),
            onSeekBy = { by -> seekTo(PlaybackPosition.skip(currentTime, by, totalTime)) }
        )
        when {
            failed -> TimesRow(stringResource(R.string.playback_failed), null, error = true)
            chapter != null -> TimesRow(
                PlaybackPosition.spoken(chapter.elapsed(currentTime)),
                stringResource(R.string.time_left_in_chapter, PlaybackPosition.spoken(chapter.left(currentTime)))
            )
            else -> TimesRow(
                PlaybackPosition.spoken(currentTime),
                stringResource(
                    R.string.time_left_of,
                    PlaybackPosition.spoken(PlaybackPosition.left(currentTime, totalTime)),
                    PlaybackPosition.spoken(totalTime)
                )
            )
        }
        if (chapter != null) {
            PlaybackBar(
                fraction = PlaybackPosition.fraction(currentTime, totalTime),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag(BOOK_BAR_TAG),
                ticks = PlaybackPosition.chapterMarks(chapters, totalTime).map { it / 100f },
                thick = false
            )
            TimesRow(
                stringResource(R.string.time_of, PlaybackPosition.spoken(currentTime), PlaybackPosition.spoken(totalTime)),
                stringResource(R.string.time_left, PlaybackPosition.spoken(PlaybackPosition.left(currentTime, totalTime))),
                small = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (chapter != null) {
                TransportButton(
                    onClick = { ChapterClock.previousTarget(spans, currentTime)?.let(::seekTo) },
                    size = 48.dp,
                    container = TvMaterialTheme.colorScheme.secondaryContainer,
                    content = TvMaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.previous_chapter))
                }
            }
            TransportButton(
                onClick = { seekTo(PlaybackPosition.skip(currentTime, -SKIP_SECONDS, totalTime)) },
                size = 48.dp,
                container = TvMaterialTheme.colorScheme.secondaryContainer,
                content = TvMaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.skip_back_30_seconds))
            }
            TransportButton(
                onClick = {
                    when {
                        // After an error the player is idle where it failed;
                        // preparing again retries from there.
                        failed -> { player.prepare(); player.play() }
                        isPlaying -> player.pause()
                        else -> player.play()
                    }
                },
                size = 60.dp,
                container = TvMaterialTheme.colorScheme.primary,
                content = TvMaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.focusRequester(playFocus)
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            TransportButton(
                onClick = { seekTo(PlaybackPosition.skip(currentTime, SKIP_SECONDS, totalTime)) },
                size = 48.dp,
                container = TvMaterialTheme.colorScheme.secondaryContainer,
                content = TvMaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.skip_forward_30_seconds))
            }
            if (chapter != null) {
                TransportButton(
                    // Nothing after the last chapter: the press does nothing
                    // rather than leaving a button that cannot take focus.
                    onClick = { ChapterClock.nextTarget(spans, currentTime)?.let(::seekTo) },
                    size = 48.dp,
                    container = TvMaterialTheme.colorScheme.secondaryContainer,
                    content = TvMaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.next_chapter))
                }
            }
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/** A line of times under a bar: what has passed on the left, what is left on the right. */
@Composable
private fun TimesRow(left: String, right: String?, small: Boolean = false, error: Boolean = false) {
    val style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
    val color = when {
        error -> MaterialTheme.colorScheme.error
        small -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(text = left, style = style, color = color, modifier = Modifier.weight(1f))
        right?.let { Text(text = it, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** How far the skip buttons jump. */
private const val SKIP_SECONDS = 30.0

/**
 * A transport button: the TV IconButton, which takes focus by key as well as
 * by touch and shows it - the phone ones this replaced did neither on the
 * stick. Focus inverts the colours and adds a 3dp primary border; nothing grows.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TransportButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    TvIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        scale = TvIconButtonDefaults.scale(focusedScale = 1f),
        colors = TvIconButtonDefaults.colors(
            containerColor = container,
            contentColor = content,
            // Inverted on focus, as every TV button in the app is: a primary
            // border on the primary play button could not be seen.
            focusedContainerColor = TvMaterialTheme.colorScheme.onSurface,
            focusedContentColor = TvMaterialTheme.colorScheme.surface
        ),
        border = TvIconButtonDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TvMaterialTheme.colorScheme.primary))
        )
    ) {
        // The icons are phone Icons, which read the phone content colour and
        // would not follow the TV button's focus colours.
        CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides TvLocalContentColor.current) {
            icon()
        }
    }
}
