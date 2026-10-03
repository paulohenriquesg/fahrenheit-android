package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import androidx.core.content.edit

/**
 * The player's own settings, on this device: "Play the next episode
 * automatically" (#108), off by default. Kept apart from the account's
 * preferences, as [SpeedMemory] is.
 */
class PlayerSettings(context: Context) {
    private val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)

    var playNextEpisode: Boolean
        get() = prefs.getBoolean(PLAY_NEXT_EPISODE, false)
        set(value) = prefs.edit { putBoolean(PLAY_NEXT_EPISODE, value) }

    private companion object {
        const val PLAY_NEXT_EPISODE = "play_next_episode"
    }
}
