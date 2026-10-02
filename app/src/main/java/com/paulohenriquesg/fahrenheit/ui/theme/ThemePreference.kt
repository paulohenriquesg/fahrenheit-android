package com.paulohenriquesg.fahrenheit.ui.theme

/**
 * What the person asked for: a theme, or the device's.
 *
 * Stored as a nullable boolean, where absent means "follow the device" - which
 * is what a fresh install does, and what a switch could never get back to.
 */
enum class ThemePreference {
    System,
    Light,
    Dark;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        System -> systemIsDark
        Light -> false
        Dark -> true
    }

    /** null when following the device, so the stored choice can be removed. */
    fun stored(): Boolean? = when (this) {
        System -> null
        Light -> false
        Dark -> true
    }

    companion object {
        fun of(stored: Boolean?): ThemePreference = when (stored) {
            null -> System
            true -> Dark
            false -> Light
        }
    }
}
