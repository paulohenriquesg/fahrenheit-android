package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesIgnoreStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = PreferencesIgnoreStore(context)

    @Before
    fun setUp() {
        SharedPreferencesHandler(context).clearPreferences()
    }

    @Test
    fun `nothing is ignored on a fresh install`() {
        assertNull(store.ignoredVersionCode())
    }

    @Test
    fun `an ignored version survives being read back`() {
        store.ignore(12)

        assertEquals(12, PreferencesIgnoreStore(context).ignoredVersionCode())
    }

    @Test
    fun `ignoring a newer version replaces the older one`() {
        store.ignore(12)
        store.ignore(13)

        assertEquals(13, store.ignoredVersionCode())
    }

    // The one-day snooze (#229 removed it) may still be on a device. It must
    // not read as an ignored version, and must not crash anything.
    @Test
    fun `a snooze saved by the old code is not an ignore, and is dropped`() {
        // Robolectric has no keystore, so the handler falls back to this file.
        val raw = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        raw.edit {
            putString("username", "listener")
            putInt("update_snooze_version_code", 12)
            putLong("update_snooze_at", 1_700_000_000_000L)
        }
        val handler = SharedPreferencesHandler(context)
        // The same file the handler reads, or this proves nothing.
        assertEquals("listener", handler.getUserPreferences().username)

        assertNull(store.ignoredVersionCode())
        handler.saveUserPreferences(handler.getUserPreferences())

        assertFalse(raw.contains("update_snooze_version_code"))
        assertFalse(raw.contains("update_snooze_at"))
    }
}
