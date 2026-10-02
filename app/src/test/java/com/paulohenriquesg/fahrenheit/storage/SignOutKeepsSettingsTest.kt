package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Signing out is not the same as forgetting which server this is. Retyping a
 * host on a TV keyboard is the most expensive thing we can ask for, so the
 * session goes and the settings stay (#63).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignOutKeepsSettingsTest {

    private lateinit var context: Context
    private lateinit var handler: SharedPreferencesHandler

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        handler = SharedPreferencesHandler(context)
        handler.clearPreferences()
        handler.saveUserPreferences(
            UserPreferences(
                host = "http://books.example:13378",
                username = "admin",
                token = "access-token",
                refreshToken = "refresh-token",
                darkTheme = true,
                isRowLayout = false,
                selectedLibraryId = "lib-1"
            )
        )
    }

    @Test
    fun `signing out forgets both tokens`() {
        handler.clearSession()

        val after = handler.getUserPreferences()
        assertEquals("", after.token)
        assertNull(after.refreshToken)
    }

    @Test
    fun `signing out keeps the server address and the username`() {
        handler.clearSession()

        val after = handler.getUserPreferences()
        assertEquals("http://books.example:13378", after.host)
        assertEquals("admin", after.username)
    }

    @Test
    fun `signing out keeps the theme, chosen as well as its value`() {
        handler.clearSession()

        assertTrue(handler.getUserPreferences().darkTheme)
        // Without this the theme silently reverts to following the device.
        assertTrue(handler.hasChosenTheme())
    }

    @Test
    fun `signing out keeps the layout choice`() {
        handler.clearSession()

        assertEquals(false, handler.getUserPreferences().isRowLayout)
    }

    @Test
    fun `signing out forgets which library was open, which belongs to the account`() {
        handler.clearSession()

        assertNull(handler.getUserPreferences().selectedLibraryId)
    }
}
