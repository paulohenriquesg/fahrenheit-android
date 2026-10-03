package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * The colour behind the player, taken from its cover (#107; the wash in
 * frame C of docs/mocks/player.html).
 *
 * Only a colour with some colour in it is used: a grey, black or white cover
 * gives no wash rather than a muddy one. The colour is darkened - never
 * lightened - until the screen's light text keeps [MIN_CONTRAST] against it.
 */
object CoverWash {
    const val MIN_CONTRAST = 4.5

    /**
     * The darkest text drawn over the wash, onSurfaceVariant: if it reads,
     * the lighter onSurface and primary read too.
     */
    private val TEXT = Color(0xFFCAC4D0)
    private const val MIN_SATURATION = 0.18f

    fun pick(candidates: List<Int?>): Color? =
        candidates.filterNotNull().firstOrNull { saturation(it) >= MIN_SATURATION }?.let(::tone)

    fun tone(argb: Int): Color {
        var colour = Color(argb).copy(alpha = 1f)
        repeat(40) {
            if (contrast(TEXT, colour) >= MIN_CONTRAST + 0.2) return colour
            colour = Color(colour.red * 0.9f, colour.green * 0.9f, colour.blue * 0.9f)
        }
        return colour
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (max(la, lb) / min(la, lb)).toDouble()
    }

    private fun saturation(argb: Int): Float {
        val c = Color(argb)
        val hi = maxOf(c.red, c.green, c.blue)
        val lo = minOf(c.red, c.green, c.blue)
        return if (hi == 0f) 0f else (hi - lo) / hi
    }
}
