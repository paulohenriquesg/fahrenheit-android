package com.paulohenriquesg.fahrenheit.ui

import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale

/**
 * How a card shows focus.
 *
 * The TV card grows by a tenth when focused, which the lazy row or grid holding
 * it then clips - the focused cover is cut off by its own container. It also
 * sits oddly beside everything else in the app, where focus is a 3dp border and
 * nothing moves (docs/ui-style-guide.md).
 *
 * So: no growth. The border does the talking.
 */
object CardFocus {
    val noGrowth: CardScale
        get() = CardDefaults.scale(focusedScale = 1f)
}
