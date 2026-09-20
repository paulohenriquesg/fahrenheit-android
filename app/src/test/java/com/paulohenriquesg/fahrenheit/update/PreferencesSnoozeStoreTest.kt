package com.paulohenriquesg.fahrenheit.update

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesSnoozeStoreTest {

    private val store = PreferencesSnoozeStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `nothing is snoozed on a fresh install`() {
        assertNull(store.snoozedVersionCode())
        assertNull(store.snoozedAt())
    }

    @Test
    fun `a snooze survives being read back`() {
        store.snooze(versionCode = 12, at = 1_700_000_000_000L)

        assertEquals(12, store.snoozedVersionCode())
        assertEquals(1_700_000_000_000L, store.snoozedAt())
    }

    @Test
    fun `snoozing a newer version replaces the older snooze`() {
        store.snooze(versionCode = 12, at = 1L)
        store.snooze(versionCode = 13, at = 2L)

        assertEquals(13, store.snoozedVersionCode())
        assertEquals(2L, store.snoozedAt())
    }
}
