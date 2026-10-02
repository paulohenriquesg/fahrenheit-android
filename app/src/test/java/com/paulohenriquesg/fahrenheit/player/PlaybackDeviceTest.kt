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

        assertEquals(BuildConfig.VERSION_NAME, info.clientVersion)
        assertEquals(Build.VERSION.SDK_INT, info.sdkVersion)
        assertEquals(Build.MANUFACTURER, info.manufacturer)
        assertEquals(Build.MODEL, info.model)
    }

    // Review: one shared id made two TVs on one account close each other's
    // sessions; the server closes a user's open sessions on the same device.
    @Test
    fun `each install has its own id, kept across calls`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = PlaybackDevice.info(context).deviceId

        assertEquals(first, PlaybackDevice.info(context).deviceId)
        org.junit.Assert.assertNotEquals("Fire Stick", first)
    }

    @Test
    fun `another install gets another id`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val here = PlaybackDevice.info(context).deviceId
        PlaybackDevice.forgetForTest(context)

        org.junit.Assert.assertNotEquals(here, PlaybackDevice.info(context).deviceId)
    }
}
