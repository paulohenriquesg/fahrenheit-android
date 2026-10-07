package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The tilted, drifting wall covers the whole screen at every point of its drift (#172),
 * moved as a layer and turned only in its drawing (#176).
 */
class WallFrameTest {

    // A Fire TV screen in px, the wall's drift and tilt as drawn.
    private val width = 1920f
    private val height = 1080f
    private val driftX = 240f
    private val driftY = 160f
    private val tilt = -8f

    private val corners = listOf(Offset(0f, 0f), Offset(width, 0f), Offset(width, height), Offset(0f, height))

    /** Whether [point] lies inside a [wall] centred at [centre], turned by [degrees] round it. */
    private fun inside(point: Offset, wall: Size, centre: Offset, degrees: Float): Boolean {
        val a = Math.toRadians(degrees.toDouble())
        val dx = (point.x - centre.x).toDouble()
        val dy = (point.y - centre.y).toDouble()
        // Back into the wall's own axes: the inverse of a turn by [degrees].
        val x = dx * cos(a) + dy * sin(a)
        val y = -dx * sin(a) + dy * cos(a)
        return abs(x) <= wall.width / 2 + 0.01 && abs(y) <= wall.height / 2 + 0.01
    }

    // The layer is not turned (#176): it only moves, so it is checked unturned.
    private fun covers(layer: Size): Boolean = (0..100).all { step ->
        val t = step / 100f
        val shift = WallFrame.shift(t, driftX, driftY)
        val centre = Offset(width / 2 + shift.x, height / 2 + shift.y)
        corners.all { inside(it, layer, centre, 0f) }
    }

    @Test
    fun `all four screen corners lie inside the moving layer over the drift's full range`() =
        assertTrue(covers(WallFrame.size(width, height, driftX, driftY)))

    @Test
    fun `the layer is no bigger than it needs to be`() {
        val layer = WallFrame.size(width, height, driftX, driftY)
        assertFalse(covers(Size(layer.width * 0.97f, layer.height)))
        assertFalse(covers(Size(layer.width, layer.height * 0.97f)))
    }

    // The tilt is in the drawing (#176): a grid turned round the layer's centre must cover it.
    private fun gridCovers(layer: Size, grid: Size): Boolean {
        val centre = Offset(layer.width / 2, layer.height / 2)
        val layerCorners = listOf(Offset(0f, 0f), Offset(layer.width, 0f), Offset(layer.width, layer.height), Offset(0f, layer.height))
        return layerCorners.all { inside(it, grid, centre, tilt) }
    }

    @Test
    fun `the turned grid covers all four corners of the layer`() {
        val layer = WallFrame.size(width, height, driftX, driftY)
        assertTrue(gridCovers(layer, WallFrame.grid(layer, tilt)))
    }

    @Test
    fun `the turned grid is no bigger than it needs to be`() {
        val layer = WallFrame.size(width, height, driftX, driftY)
        val grid = WallFrame.grid(layer, tilt)
        assertFalse(gridCovers(layer, Size(grid.width * 0.97f, grid.height)))
        assertFalse(gridCovers(layer, Size(grid.width, grid.height * 0.97f)))
    }

    @Test
    fun `the drift goes as far each way from the middle`() {
        val start = WallFrame.shift(0f, driftX, driftY)
        val end = WallFrame.shift(1f, driftX, driftY)
        assertTrue(abs(start.x + end.x) < 0.01f && abs(start.y + end.y) < 0.01f)
        assertTrue(abs(start.x - end.x - driftX) < 0.01f && abs(start.y - end.y - driftY) < 0.01f)
    }
}
