package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A slightly tilted wall of covers, drifting slowly (docs/mocks/login-style.html;
 * the screensaver's wall, #156). The covers repeat to fill it; the caller dims
 * and shades it.
 *
 * The wall is drawn once; each frame only moves its layer, so a Fire TV Stick
 * is not redrawing a hundred covers while someone types a password.
 *
 * It moves for as long as it is drawn, so only draw it with covers to show: an
 * endless animation keeps a Compose test from ever going idle. It needs a
 * bounded size, as both callers give it with fillMaxSize: it is sized from it.
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
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        // The layer is the whole wall, turned and moved as one, so the covers
        // reach its edges from inside: a TV drops what a layer draws past its
        // bounds, which left the edges of the screen bare (#172).
        val wall = WallFrame.forScreen(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), density)
        Spacer(
            Modifier
                .requiredSize(with(density) { DpSize(wall.width.toDp(), wall.height.toDp()) })
                .testTag(CoverWallTags.LAYER)
                // Read in the layer, not the drawing: a frame moves, nothing redraws.
                .graphicsLayer {
                    val shift = WallFrame.shift(drift.value, DRIFT_X.toPx(), DRIFT_Y.toPx())
                    translationX = shift.x
                    translationY = shift.y
                    rotationZ = TILT_DEGREES
                }
                .drawWithCache {
                    val cell = CELL.toPx()
                    val step = cell + GAP.toPx()
                    val side = cell.toInt()
                    val corner = CornerRadius(6.dp.toPx())
                    val columns = (size.width / step).toInt() + 1
                    val rows = (size.height / step).toInt() + 1
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
        )
    }
}

private val CELL = 95.dp
private val GAP = 12.dp
private val DRIFT_X = 120.dp
private val DRIFT_Y = 80.dp
private const val TILT_DEGREES = -8f
private const val DRIFT_MS = 70_000

object CoverWallTags {
    const val LAYER = "cover-wall-layer"
}

/**
 * The wall's size and place (#172): turned round its own centre and drifting
 * half its drift each way from the middle of the screen, it covers the whole
 * screen, edge to edge, at every point of the drift.
 */
internal object WallFrame {
    /**
     * The smallest wall that does it: every screen corner is at most half the
     * screen plus half the drift from the wall's centre along each axis, and
     * turning that box by the tilt widens it by the other side's share.
     */
    fun size(width: Float, height: Float, driftX: Float, driftY: Float, tiltDegrees: Float): Size {
        val a = Math.toRadians(tiltDegrees.toDouble())
        val halfX = (width + driftX) / 2
        val halfY = (height + driftY) / 2
        val c = abs(cos(a)).toFloat()
        val s = abs(sin(a)).toFloat()
        return Size(2 * (halfX * c + halfY * s), 2 * (halfX * s + halfY * c))
    }

    /** The wall's centre from the screen's, at [t] from 0 to 1 along the drift. */
    fun shift(t: Float, driftX: Float, driftY: Float): Offset =
        Offset((0.5f - t) * driftX, (0.5f - t) * driftY)

    /** As drawn: this wall's drift and tilt. */
    fun forScreen(width: Float, height: Float, density: Density): Size =
        with(density) { size(width, height, DRIFT_X.toPx(), DRIFT_Y.toPx(), TILT_DEGREES) }
}
