package com.paulohenriquesg.fahrenheit.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The theme was a switch, so "follow the device" could be the state you started
 * in but never one you could choose again. ThemeChoice.resolve has always taken
 * a null for it; only the setting could not say it.
 */
@RunWith(RobolectricTestRunner::class)
class ThemePreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearStoredChoice() {
        SharedPreferencesHandler(context).clearPreferences()
    }

    @Test
    fun `no stored choice reads as following the device`() {
        assertEquals(ThemePreference.System, ThemePreference.of(stored = null))
    }

    @Test
    fun `a stored choice reads as that choice`() {
        assertEquals(ThemePreference.Dark, ThemePreference.of(stored = true))
        assertEquals(ThemePreference.Light, ThemePreference.of(stored = false))
    }

    @Test
    fun `following the device means dark on a dark TV and light on a light one`() {
        assertEquals(true, ThemePreference.System.isDark(systemIsDark = true))
        assertEquals(false, ThemePreference.System.isDark(systemIsDark = false))
    }

    @Test
    fun `a chosen theme ignores what the TV is set to`() {
        assertEquals(true, ThemePreference.Dark.isDark(systemIsDark = false))
        assertEquals(false, ThemePreference.Light.isDark(systemIsDark = true))
    }

    @Test
    @Config(qualifiers = "night")
    fun `choosing dark then going back to System forgets the choice`() {
        ThemeManager.apply(context, ThemePreference.Light)
        assertEquals(false, ThemeManager.isDarkTheme.value)

        ThemeManager.apply(context, ThemePreference.System)

        // Forgotten, not stored as a third value: the device decides again.
        assertFalse(SharedPreferencesHandler(context).hasChosenTheme())
        assertEquals(true, ThemeManager.isDarkTheme.value)
    }

    @Test
    @Config(qualifiers = "night")
    fun `what settings shows is what was stored`() {
        ThemeManager.apply(context, ThemePreference.Light)

        assertEquals(ThemePreference.Light, ThemeManager.preference(context))

        ThemeManager.apply(context, ThemePreference.System)

        assertEquals(ThemePreference.System, ThemeManager.preference(context))
    }
}
