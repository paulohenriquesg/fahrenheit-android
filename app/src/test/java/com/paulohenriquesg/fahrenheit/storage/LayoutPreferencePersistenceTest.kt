package com.paulohenriquesg.fahrenheit.storage

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * LayoutManager stores the row/grid choice through UserPreferences, so the field
 * has to actually reach SharedPreferences - otherwise the toggle silently resets
 * to its default on every launch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LayoutPreferencePersistenceTest {
    private lateinit var context: Context
    private lateinit var handler: SharedPreferencesHandler

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        handler = SharedPreferencesHandler(context)
        handler.clearPreferences()
    }

    @Test
    fun `row layout choice survives a round trip`() {
        handler.saveUserPreferences(
            UserPreferences(
                host = "http://abs.local",
                username = "testuser",
                token = "token",
                darkTheme = false,
                isRowLayout = false
            )
        )

        assertFalse(handler.getUserPreferences().isRowLayout)
    }
}
