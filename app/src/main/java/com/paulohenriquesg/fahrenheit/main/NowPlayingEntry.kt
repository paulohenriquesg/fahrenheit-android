package com.paulohenriquesg.fahrenheit.main

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
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
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage

const val NOW_PLAYING_TAG = "rail_now_playing"

/**
 * The rail's way back to the player (#107; the rail frames of
 * docs/mocks/player.html). Closed: the cover, a ring for how far through the
 * book or episode, and the play state. Open: the title, and the chapter with
 * its time left (or what is left of an episode). Choosing it opens the player;
 * it is not a second set of controls.
 */
@Composable
fun NowPlayingEntry(entry: RailEntry, open: Boolean, onOpen: (RailEntry) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onOpen(entry) },
        modifier = modifier.testTag(NOW_PLAYING_TAG),
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
                Column {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val left = PlaybackPosition.spoken(entry.leftSeconds)
                    Text(
                        entry.chapter?.let { stringResource(R.string.rail_chapter_left, it, left) }
                            ?: stringResource(R.string.time_left, left),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** The cover inside a ring of progress, and the play state at its corner. */
@Composable
private fun CoverWithRing(entry: RailEntry) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val done = MaterialTheme.colorScheme.primary
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(done, -90f, 360f * entry.progress, false, Offset(inset, inset), arc, style = Stroke(stroke))
        }
        CoverImage(itemId = entry.itemId, contentDescription = entry.title, size = 36.dp)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (entry.playing) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = stringResource(if (entry.playing) R.string.rail_playing else R.string.rail_paused),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
