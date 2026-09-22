package com.paulohenriquesg.fahrenheit.api

import org.junit.Assert.assertEquals
import com.paulohenriquesg.fahrenheit.auth.StoredCredentials
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.auth.AuthSession
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Plain JUnit: session state is not an Android concern.
 *
 * This used to need Robolectric purely because SessionManager took a concrete
 * SharedPreferencesHandler - about 34x slower per test, and invisible to
 * coverage without a JaCoCo workaround.
 */
class SessionManagerTest {

    private fun managerWith(
        credentials: StoredCredentials = StoredCredentials.EMPTY
    ): Pair<SessionManager, FakeTokenStore> {
        val store = FakeTokenStore(credentials)
        return SessionManager(store) to store
    }

    @Test
    fun `persisting a session stores both tokens and the identity`() {
        val (manager, store) = managerWith()

        manager.persist("http://abs.local", AuthSession("access", "refresh", "testuser"))

        assertEquals("http://abs.local", store.read().host)
        assertEquals("testuser", store.read().username)
        assertEquals("access", store.read().accessToken)
        assertEquals("refresh", store.read().refreshToken)
    }

    @Test
    fun `the refresh token is readable for the 401 retry path`() {
        val (manager, _) = managerWith()
        manager.persist("http://abs.local", AuthSession("access", "refresh", "testuser"))

        assertEquals("refresh", manager.refreshToken())
    }

    @Test
    fun `a server that issues no refresh token leaves nothing to refresh with`() {
        val (manager, _) = managerWith()

        manager.persist("http://abs.local", AuthSession("legacy-non-expiring", null, "testuser"))

        assertNull(manager.refreshToken())
    }

    @Test
    fun `an empty stored token reads as no token rather than an empty string`() {
        val (manager, _) = managerWith(StoredCredentials.EMPTY)

        assertNull(manager.accessToken())
    }

    @Test
    fun `host is reported as stored`() {
        val (manager, _) = managerWith()
        manager.persist("https://abs.example.com", AuthSession("a", "r", "testuser"))

        assertEquals("https://abs.example.com", manager.host())
    }

    @Test
    fun `persisting writes once`() {
        val (manager, store) = managerWith()

        manager.persist("http://abs.local", AuthSession("access", "refresh", "testuser"))

        assertEquals(1, store.writes)
    }
}
