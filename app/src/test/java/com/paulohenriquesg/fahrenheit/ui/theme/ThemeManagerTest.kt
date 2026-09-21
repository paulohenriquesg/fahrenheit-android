package com.paulohenriquesg.fahrenheit.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(qualifiers = "night")
    fun `a device set to dark opens dark on a fresh install`() {
        ThemeManager.initialize(context)

        assertEquals(true, ThemeManager.isDarkTheme.value)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `a device set to light opens light on a fresh install`() {
        ThemeManager.initialize(context)

        assertEquals(false, ThemeManager.isDarkTheme.value)
    }

    @Test
    @Config(qualifiers = "night")
    fun `choosing light in settings survives a dark device`() {
        ThemeManager.setTheme(context, isDark = false)

        ThemeManager.initialize(context)

        assertEquals(false, ThemeManager.isDarkTheme.value)
    }

    @Test
    fun `a choice is remembered for next time`() {
        ThemeManager.setTheme(context, isDark = true)

        assertEquals(true, SharedPreferencesHandler(context).getUserPreferences().darkTheme)
        assertEquals(true, SharedPreferencesHandler(context).hasChosenTheme())
    }
}
