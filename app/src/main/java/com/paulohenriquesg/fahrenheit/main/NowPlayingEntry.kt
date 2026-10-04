package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.player.rememberRailEntry
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import androidx.media3.common.Player
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.paulohenriquesg.fahrenheit.player.RAIL_POLL_MS
import kotlin.math.PI
import kotlin.math.cos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage

const val NOW_PLAYING_TAG = "rail_now_playing"
const val NOW_PLAYING_STOP_TAG = "rail_now_playing_stop"
const val NOW_PLAYING_EQUALISER_TAG = "now_playing_equaliser"
const val NOW_PLAYING_PAUSED_TAG = "now_playing_paused"

/**
 * The rail's way back to the player (#107; the rail frames of
 * docs/mocks/player.html). Closed: the cover, a ring for how far through the
 * book or episode, and the play state (bars while playing, a pause badge
 * while not; #170). Open: the title, and the chapter with
 * its time left (or what is left of an episode), and Stop under it. Choosing
 * the entry opens the player; Stop ends the listening session - Back from the
 * player keeps playing (#155). It is not a second set of transport controls.
 */
@Composable
fun NowPlayingEntry(entry: RailEntry, open: Boolean, onOpen: (RailEntry) -> Unit, onStop: (RailEntry) -> Unit, modifier: Modifier = Modifier) {
    // Widths of their own, as the sections have: the drawer gives its content
    // the whole screen to grow into, so nothing here may fill or wrap a width.
    val width = if (open) OPEN_WIDTH else CLOSED_WIDTH
    Column(modifier) {
        Surface(
            onClick = { onOpen(entry) },
            modifier = Modifier.width(width).testTag(NOW_PLAYING_TAG),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverWithRing(entry)
                if (open) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            leftLine(entry),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        // Only while open: a closed rail holds no focus, and has no room for it.
        if (open) {
            StopButton(onClick = { onStop(entry) }, modifier = Modifier.padding(start = 6.dp, top = 4.dp).testTag(NOW_PLAYING_STOP_TAG))
        }
        // The line between what is playing and the sections.
        Box(
            Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .width(width - 16.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

/**
 * The entry for what [player] has queued, or nothing. Reads the player
 * inside itself, so its changes redraw this entry and not the whole screen.
 *
 * @param onStopped after Stop has ended playback: the focused button is about to go.
 */
@Composable
fun NowPlayingSlot(
    player: Player,
    open: Boolean,
    chaptersOf: suspend (String) -> List<Chapter>?,
    onOpen: (RailEntry) -> Unit,
    onStopped: () -> Unit = {}
) {
    val entry = rememberRailEntry(player, chaptersOf) ?: return
    NowPlayingEntry(entry, open, onOpen, onStop = {
        Playback.end(player)
        onStopped()
    })
}

/** Stop, beside or under Now playing: ends the listening session (#155, #159). */
@Composable
internal fun StopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The label says it; the icon is decoration, not read out twice.
            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.rail_stop), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A book's chapters, for Now playing's time left; null when they could not be read. */
suspend fun queuedChapters(itemId: String): List<Chapter>? =
    ApiClient.getLibraryApi()?.let { LibraryRepository(it).item(itemId).getOrNull() }?.media?.chapters

/** "Chapter · N min left", or what is left of an episode or an unchaptered book. */
@Composable
internal fun leftLine(entry: RailEntry): String {
    val left = leftWords(entry.leftSeconds)
    val chapter = entry.chapter ?: entry.chapterNumber?.let { stringResource(R.string.chapter_number, it) }
    return chapter?.let { stringResource(R.string.rail_chapter_left, it, left) } ?: stringResource(R.string.time_left, left)
}

/** Whole minutes, as the mock writes it - the seconds would jump with each poll - and seconds under one. */
private fun leftWords(seconds: Double): String =
    if (seconds in 60.0..3599.0) "${(seconds / 60).toInt()} min" else PlaybackPosition.spoken(seconds)

private val CLOSED_WIDTH = 56.dp
private val OPEN_WIDTH = 240.dp

/** The cover inside a ring of progress, and the play state at its corner. */
@Composable
internal fun CoverWithRing(entry: RailEntry) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val done = MaterialTheme.colorScheme.primary
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(done, -90f, 360f * entry.progress, false, Offset(inset, inset), arc, style = Stroke(stroke))
        }
        CoverImage(itemId = entry.itemId, contentDescription = entry.title, size = 32.dp)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // The state, not the action (#170): the player's button and the
            // remote show the action, so a play icon while playing read as paused.
            if (entry.playing) {
                EqualiserBars(entry, Modifier.size(10.dp))
            } else {
                Icon(
                    Icons.Filled.Pause,
                    contentDescription = stringResource(R.string.rail_paused),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp).testTag(NOW_PLAYING_PAUSED_TAG)
                )
            }
        }
    }
}

/**
 * Three bars that move while playing. Each entry the slot hands over - one
 * per poll while playing - runs them for one poll's length and no longer, so
 * nothing animates forever: a stalled or idle entry comes to rest, and one off
 * screen is not composed at all.
 */
@Composable
private fun EqualiserBars(entry: RailEntry, modifier: Modifier = Modifier) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(entry) {
        // Whole cycles to a run: from where it is, the same pose, so no jump.
        phase.snapTo(phase.value % 1f)
        phase.animateTo(phase.value + 1f, tween(RAIL_POLL_MS.toInt(), easing = LinearEasing))
    }
    val colour = MaterialTheme.colorScheme.onPrimary
    val playing = stringResource(R.string.rail_playing)
    Canvas(
        modifier
            .semantics { contentDescription = playing }
            .testTag(NOW_PLAYING_EQUALISER_TAG)
    ) {
        val bar = size.width / 4
        val gap = (size.width - 3 * bar) / 2
        // Read here, so a moving bar redraws and recomposes nothing.
        Equaliser.levels(phase.value).forEachIndexed { index, level ->
            val height = size.height * level
            drawRect(colour, Offset(index * (bar + gap), size.height - height), Size(bar, height))
        }
    }
}

/** How tall Now playing's bars are through a run (#170), as fractions of full height. */
object Equaliser {
    /** No bar drops below this: it is always visibly a bar. */
    const val FLOOR = 0.25f

    // Whole cycles per run, so a run ends where it began; each bar its own
    // pace and starting point, so they never move as one block.
    private val cycles = intArrayOf(4, 6, 5)
    private val starts = floatArrayOf(0f, 0.35f, 0.7f)

    fun levels(phase: Float): List<Float> = cycles.indices.map { bar ->
        val wave = (1 - cos(2 * PI * (cycles[bar] * phase + starts[bar]))).toFloat() / 2
        FLOOR + (1 - FLOOR) * wave
    }
}
