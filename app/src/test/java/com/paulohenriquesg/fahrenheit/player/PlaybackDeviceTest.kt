package com.paulohenriquesg.fahrenheit.player

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** How the device introduces itself when it opens a listening session (#92). */
@RunWith(AndroidJUnit4::class)
class PlaybackDeviceTest {

    @Test
    fun `the device says what it really is`() {
        val info = PlaybackDevice.info(ApplicationProvider.getApplicationContext())

        // Unchanged, so the server's device records stay as they were.
        assertEquals("Fire Stick", info.deviceId)
        assertEquals(BuildConfig.VERSION_NAME, info.clientVersion)
        assertEquals(Build.VERSION.SDK_INT, info.sdkVersion)
        assertEquals(Build.MANUFACTURER, info.manufacturer)
        assertEquals(Build.MODEL, info.model)
    }
}
