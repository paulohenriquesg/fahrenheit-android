package com.paulohenriquesg.fahrenheit.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.CoverPlaceholderTone

object PlayerLoadingTags {
    const val FRAME = "player_loading"
    const val COVER_PLACEHOLDER = "player_loading_cover"
    const val LINE_PLACEHOLDER = "player_loading_line"
    const val SPINNER = "player_loading_spinner"
}

/**
 * Which frame the player draws (#223): the failure message; the loading frame
 * ([PlayerLoading]) until the controller is connected and the item ready;
 * then [ready], in the same places.
 *
 * @param animated false draws the loading frame still, for tests.
 */
@Composable
fun PlayerStage(
    state: PlayerUiState,
    itemId: String,
    preview: PlayerPreview?,
    animated: Boolean = true,
    ready: @Composable (NowPlaying, Player) -> Unit
) {
    val playing = state.nowPlaying
    val connected = state.player
    when {
        state.failed || state.connectFailed -> Text(
            text = stringResource(R.string.item_load_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        playing == null || connected == null || !state.ready -> PlayerLoading(itemId, preview, animated)
        else -> ready(playing, connected)
    }
}

/**
 * The player's layout while the item loads (#223; frames 1 and 3 of
 * docs/mocks/player-loading.html), so opening it is not a black screen. Laid
 * out as [PlayerScreen] lays out the loaded item: the cover, and beside it
 * what [preview] - the opening screen - already knew, with grey lines where
 * the description goes; with no preview, a placeholder cover and grey title
 * lines. Under them the bar, sweeping instead of filling, and a spinner with
 * "Starting…" where the buttons go. Nothing here takes focus: the buttons
 * arrive with the item.
 *
 * The sweep and the spinner never end by themselves; they last while this
 * frame is on screen, and go with it.
 *
 * @param animated false draws them still: a test holding this frame would
 *   otherwise never be idle.
 */
@Composable
fun PlayerLoading(itemId: String, preview: PlayerPreview?, animated: Boolean = true) {
    val motion = loadingMotion(animated)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(PlayerLayout.padding)
            .testTag(PlayerLoadingTags.FRAME),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PlayerLayout.coverGap)
        ) {
            if (preview != null) {
                // The opening screen's own request: same URL, so Coil may already hold it.
                CoverImage(
                    itemId = itemId,
                    contentDescription = preview.title,
                    size = PlayerLayout.cover,
                    title = preview.title,
                    hasCover = preview.coverUrl != null,
                    url = preview.coverUrl
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(PlayerLayout.cover)
                        .background(CoverPlaceholderTone.of(MaterialTheme.colorScheme))
                        .testTag(PlayerLoadingTags.COVER_PLACEHOLDER),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = PlayerLayout.textColumn(this), verticalArrangement = Arrangement.spacedBy(PlayerLayout.lineGap)) {
                if (preview != null) {
                    // The kicker's line, kept when empty as the player keeps it.
                    Text(
                        text = preview.kicker?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = preview.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // The byline and the chapter line's places, so the title stays put on ready.
                    preview.byline?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } ?: GreyLine(0.7f, 18.dp)
                    GreyLine(0.55f, 22.dp)
                } else {
                    GreyLine(0.4f, 14.dp)
                    GreyLine(0.9f, 32.dp)
                    GreyLine(0.6f, 32.dp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        SweepBar(motion)
        // Where the times go, so the row below sits where the buttons will.
        Text(" ", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
        Row(
            modifier = Modifier.padding(top = 18.dp).height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spinner(motion)
            Text(
                text = stringResource(R.string.player_starting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One clock for the sweep and the spinner, 0..1 each lap; still at 0.3 when not [animated]. */
@Composable
private fun loadingMotion(animated: Boolean): State<Float> =
    if (animated) {
        rememberInfiniteTransition(label = "loading").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(LAP_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "lap"
        )
    } else {
        remember { mutableFloatStateOf(STILL_AT) }
    }

/** Slow, as the mock asks: the bar is waiting, not busy. */
private const val LAP_MS = 1600
private const val STILL_AT = 0.3f

@Composable
private fun GreyLine(fraction: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .testTag(PlayerLoadingTags.LINE_PLACEHOLDER)
    )
}

/** [PlaybackBar]'s thick track, with a band crossing it instead of a fill. */
@Composable
private fun SweepBar(motion: State<Float>) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val band = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val barHeight = 7.dp.toPx()
        val top = (size.height - barHeight) / 2
        val radius = CornerRadius(barHeight / 2)
        drawRoundRect(track, Offset(0f, top), Size(size.width, barHeight), radius)
        val width = size.width * BAND
        // From fully off the left edge to fully off the right.
        val left = -width + (size.width + width) * motion.value
        // Inside the track's rounded ends.
        clipPath(Path().apply { addRoundRect(RoundRect(0f, top, size.width, top + barHeight, radius)) }) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, band, Color.Transparent), startX = left, endX = left + width),
                Offset(left, top),
                Size(width, barHeight)
            )
        }
    }
}

private const val BAND = 0.22f

@Composable
private fun Spinner(motion: State<Float>) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val arc = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(32.dp).testTag(PlayerLoadingTags.SPINNER)) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(arc, motion.value * 360f * SPINS - 90f, 90f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** Turns of the spinner per lap of the sweep. */
private const val SPINS = 2
