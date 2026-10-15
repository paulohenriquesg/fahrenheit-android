package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.MePermissions
import com.paulohenriquesg.fahrenheit.auth.SessionRole
import com.paulohenriquesg.fahrenheit.auth.StoredCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The signed-in user's type and whether they may change items (#225): known
 * before a page loads, so the podcast page's loading frame can keep an admin's
 * buttons' places. Only those two, nothing else of the account.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionRoleStorageTest {

    private lateinit var handler: SharedPreferencesHandler

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        handler = SharedPreferencesHandler(context)
        handler.clearPreferences()
    }

    @Test
    fun `nothing stored reads as no role`() {
        assertNull(handler.getSessionRole())
    }

    @Test
    fun `the role survives a round trip`() {
        handler.saveSessionRole(SessionRole("admin", mayUpdate = true))

        assertEquals(SessionRole("admin", mayUpdate = true), handler.getSessionRole())
    }

    @Test
    fun `signing out forgets the role`() {
        handler.saveSessionRole(SessionRole("root", mayUpdate = true))

        handler.clearSession()

        assertNull(handler.getSessionRole())
    }

    @Test
    fun `signing in stores the role with the tokens, and another account replaces it`() {
        val store = SharedPreferencesTokenStore(handler)
        store.write(StoredCredentials("http://abs.local", "first", "a", "r", role = SessionRole("admin", mayUpdate = true)))
        handler.clearSession()
        store.write(StoredCredentials("http://abs.local", "second", "b", "s", role = SessionRole("user", mayUpdate = false)))

        assertEquals(SessionRole("user", mayUpdate = false), handler.getSessionRole())
    }

    @Test
    fun `a token refresh that says nothing of the role keeps the stored one`() {
        handler.saveSessionRole(SessionRole("admin", mayUpdate = true))

        SharedPreferencesTokenStore(handler).write(StoredCredentials("http://abs.local", "first", "a2", "r2"))

        assertEquals(SessionRole("admin", mayUpdate = true), handler.getSessionRole())
    }

    @Test
    fun `the launch's own read of the user gives its type and update permission`() {
        assertEquals(SessionRole("admin", mayUpdate = true), SessionRole.of(Me(type = "admin", permissions = MePermissions(update = true))))
        assertEquals(SessionRole("user", mayUpdate = false), SessionRole.of(Me(type = "user", permissions = null)))
    }
}
