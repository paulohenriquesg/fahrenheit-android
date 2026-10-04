package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * A slightly tilted wall of covers, drifting slowly (docs/mocks/login-style.html;
 * the screensaver's wall, #156). The covers repeat to fill it; the caller dims
 * and shades it.
 *
 * The wall is drawn once; each frame only moves its layer, so a Fire TV Stick
 * is not redrawing a hundred covers while someone types a password.
 *
 * It moves for as long as it is drawn, so only draw it with covers to show: an
 * endless animation keeps a Compose test from ever going idle.
 */
@Composable
fun CoverWall(covers: List<ImageBitmap>, modifier: Modifier = Modifier, alpha: Float = 0.32f) {
    if (covers.isEmpty()) return
    val drift = rememberInfiniteTransition(label = "wall").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Box(modifier.clipToBounds()) {
        Spacer(
            Modifier
                .fillMaxSize()
                // Read in the layer, not the drawing: a frame moves, nothing redraws.
                .graphicsLayer {
                    translationX = -DRIFT_X.toPx() * drift.value
                    translationY = -DRIFT_Y.toPx() * drift.value
                }
                .drawWithCache {
                    val cell = CELL.toPx()
                    val step = cell + GAP.toPx()
                    val side = cell.toInt()
                    val corner = CornerRadius(6.dp.toPx())
                    // Past every edge, so neither the tilt nor the drift shows a corner.
                    val margin = step * 2
                    val columns = ((size.width + margin * 2 + DRIFT_X.toPx()) / step).toInt() + 1
                    val rows = ((size.height + margin * 2 + DRIFT_Y.toPx()) / step).toInt() + 1
                    val cells = (0 until rows).flatMap { row ->
                        (0 until columns).map { column ->
                            val x = column * step
                            val y = row * step
                            // Offset by row, so one cover does not line up down a column.
                            Triple(x, y, covers[(row * 5 + column) % covers.size])
                        }
                    }
                    val frames = cells.map { (x, y, _) ->
                        Path().apply { addRoundRect(RoundRect(x, y, x + cell, y + cell, corner)) }
                    }
                    onDrawBehind {
                        rotate(TILT_DEGREES) {
                            translate(-margin, -margin) {
                                cells.forEachIndexed { i, (x, y, cover) ->
                                    // The middle square of a cover of another shape, not a squashed whole.
                                    val crop = min(cover.width, cover.height)
                                    clipPath(frames[i]) {
                                        drawImage(
                                            cover,
                                            srcOffset = IntOffset((cover.width - crop) / 2, (cover.height - crop) / 2),
                                            srcSize = IntSize(crop, crop),
                                            dstOffset = IntOffset(x.toInt(), y.toInt()),
                                            dstSize = IntSize(side, side),
                                            alpha = alpha
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        )
    }
}

private val CELL = 95.dp
private val GAP = 12.dp
private val DRIFT_X = 120.dp
private val DRIFT_Y = 80.dp
private const val TILT_DEGREES = -8f
private const val DRIFT_MS = 70_000
