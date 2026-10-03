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

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @org.junit.After
    fun forget() = PlaybackDevice.forgetForTest(context)

    @Test
    fun `an unnamed device is called by its model`() {
        assertEquals(Build.MODEL, PlaybackDevice.name(context))
    }

    // Audiobookshelf builds the line it shows for an Android client from
    // manufacturer and model, and overwrites deviceName with the same; so the
    // name has to travel as the model to be seen.
    @Test
    fun `a named device is shown on the server by its name`() {
        PlaybackDevice.setName(context, "Living room TV")

        assertEquals("Living room TV", PlaybackDevice.name(context))
        assertEquals("Living room TV", PlaybackDevice.info(context).model)
        assertEquals(Build.MANUFACTURER, PlaybackDevice.info(context).manufacturer)
    }

    @Test
    fun `renaming keeps the id, so the history stays together`() {
        val before = PlaybackDevice.info(context).deviceId

        PlaybackDevice.setName(context, "Bedroom TV")

        assertEquals(before, PlaybackDevice.info(context).deviceId)
    }

    @Test
    fun `a blank name goes back to the model`() {
        PlaybackDevice.setName(context, "Bedroom TV")
        PlaybackDevice.setName(context, "   ")

        assertEquals(Build.MODEL, PlaybackDevice.name(context))
    }

    @Test
    fun `a name is trimmed and kept to a line`() {
        PlaybackDevice.setName(context, "  Kitchen  ")
        assertEquals("Kitchen", PlaybackDevice.name(context))

        PlaybackDevice.setName(context, "x".repeat(100))
        assertEquals(PlaybackDevice.MAX_NAME_LENGTH, PlaybackDevice.name(context).length)
    }
}
