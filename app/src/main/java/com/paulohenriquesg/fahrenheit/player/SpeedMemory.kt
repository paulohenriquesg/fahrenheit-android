package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import androidx.core.content.edit

/**
 * The speed each book or show was last listened at, on this device (#107).
 *
 * Keyed by item id: a show's episodes share their show's id, so a podcast
 * keeps one speed for all of them. A value that is not one of
 * [ListeningSpeed.STEPS] reads as normal speed.
 */
class SpeedMemory(context: Context) {
    private val prefs = context.getSharedPreferences("player_speeds", Context.MODE_PRIVATE)

    fun of(itemId: String): Float =
        prefs.getFloat(itemId, ListeningSpeed.NORMAL).takeIf { it in ListeningSpeed.STEPS } ?: ListeningSpeed.NORMAL

    fun remember(itemId: String, speed: Float) = prefs.edit { putFloat(itemId, speed) }
}
