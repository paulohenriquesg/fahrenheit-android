package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

/** How far one D-pad press seeks on a playback bar; holding the key goes further. */
object SeekStep {
    fun seconds(repeatCount: Int): Double = when {
        repeatCount >= 10 -> 60.0
        repeatCount >= 3 -> 30.0
        else -> 10.0
    }
}

/**
 * A playback bar for the TV: the chapter bar you seek with, or the slim book
 * bar under it (#107, frame C in docs/mocks/player.html).
 *
 * With [onSeekBy] it takes focus, and Left/Right seek by [SeekStep]; every
 * other key is left alone, so Up and Down still move focus. Without it, it is
 * a picture and takes no focus.
 *
 * @param ticks chapter boundaries, as fractions of the bar.
 */
@Composable
fun PlaybackBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    ticks: List<Float> = emptyList(),
    thick: Boolean = true,
    onSeekBy: ((Double) -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val track = colors.onSurface.copy(alpha = if (thick) 0.16f else 0.10f)
    val fill = if (thick) colors.primary else colors.primary.copy(alpha = 0.55f)
    val tick = colors.onSurface.copy(alpha = 0.45f)
    val ring = colors.primary

    val seeking = if (onSeekBy == null) Modifier else Modifier
        .onFocusChanged { focused = it.isFocused }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val step = SeekStep.seconds(event.nativeKeyEvent.repeatCount)
            when (event.key) {
                Key.DirectionLeft -> { onSeekBy(-step); true }
                Key.DirectionRight -> { onSeekBy(step); true }
                else -> false
            }
        }
        .focusable()

    Canvas(modifier.then(seeking).fillMaxWidth().height(if (thick) 16.dp else 8.dp)) {
        val barHeight = (if (thick) 7.dp else 3.dp).toPx()
        val top = (size.height - barHeight) / 2
        val radius = CornerRadius(barHeight / 2)
        val at = fraction.coerceIn(0f, 1f) * size.width
        drawRoundRect(track, Offset(0f, top), Size(size.width, barHeight), radius)
        drawRoundRect(fill, Offset(0f, top), Size(at, barHeight), radius)
        ticks.filter { it in 0f..1f }.forEach { t ->
            val x = t * size.width
            drawRect(tick, Offset(x - 1.dp.toPx(), top - 3.dp.toPx()), Size(2.dp.toPx(), barHeight + 6.dp.toPx()))
        }
        if (thick) {
            val knob = 8.dp.toPx()
            drawCircle(fill, knob, Offset(at, size.height / 2))
            if (focused) drawCircle(ring.copy(alpha = 0.45f), knob + 5.dp.toPx(), Offset(at, size.height / 2), style = Stroke(3.dp.toPx()))
        }
    }
}
