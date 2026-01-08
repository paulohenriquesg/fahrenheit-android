package com.paulohenriquesg.fahrenheit.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for SharedPreferencesHandler
 * Tests library persistence and user preferences management with real Android context
 */
@RunWith(AndroidJUnit4::class)
class SharedPreferencesHandlerTest {

    private lateinit var handler: SharedPreferencesHandler

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        handler = SharedPreferencesHandler(context)
        // Clear preferences before each test
        handler.clearPreferences()
    }

    @Test
    fun saveSelectedLibraryId_savesAndRetrievesLibraryId() {
        // Given
        val libraryId = "library-123"

        // When
        handler.saveSelectedLibraryId(libraryId)

        // Then
        val retrieved = handler.getSelectedLibraryId()
        assertEquals(libraryId, retrieved)
    }

    @Test
    fun getSelectedLibraryId_returnsNullWhenNotSet() {
        // When
        val result = handler.getSelectedLibraryId()

        // Then
        assertNull(result)
    }

    @Test
    fun saveSelectedLibraryId_overwritesPreviousValue() {
        // Given
        handler.saveSelectedLibraryId("library-1")

        // When
        handler.saveSelectedLibraryId("library-2")

        // Then
        val result = handler.getSelectedLibraryId()
        assertEquals("library-2", result)
    }

    @Test
    fun saveUserPreferences_savesAllFieldsIncludingSelectedLibraryId() {
        // Given
        val userPreferences = UserPreferences(
            host = "https://example.com",
            username = "testuser",
            token = "test-token",
            darkTheme = true,
            lastUpdateCheck = 123456789L,
            skipVersion = "1.0.0",
            updateCheckEnabled = false,
            selectedLibraryId = "library-789"
        )

        // When
        handler.saveUserPreferences(userPreferences)

        // Then
        val retrieved = handler.getUserPreferences()
        assertEquals("https://example.com", retrieved.host)
        assertEquals("testuser", retrieved.username)
        assertEquals("test-token", retrieved.token)
        assertEquals(true, retrieved.darkTheme)
        assertEquals(123456789L, retrieved.lastUpdateCheck)
        assertEquals("1.0.0", retrieved.skipVersion)
        assertEquals(false, retrieved.updateCheckEnabled)
        assertEquals("library-789", retrieved.selectedLibraryId)
    }

    @Test
    fun getUserPreferences_returnsEmptyValuesWhenNotSet() {
        // When
        val result = handler.getUserPreferences()

        // Then
        assertEquals("", result.host)
        assertEquals("", result.username)
        assertEquals("", result.token)
        assertEquals(false, result.darkTheme)
        assertEquals(0L, result.lastUpdateCheck)
        assertNull(result.skipVersion)
        assertEquals(true, result.updateCheckEnabled) // default is true
        assertNull(result.selectedLibraryId)
    }

    @Test
    fun clearPreferences_removesAllStoredData() {
        // Given
        handler.saveSelectedLibraryId("library-123")
        val userPreferences = UserPreferences(
            host = "https://example.com",
            username = "testuser",
            token = "test-token",
            darkTheme = true,
            lastUpdateCheck = 123456789L,
            skipVersion = "1.0.0",
            updateCheckEnabled = false,
            selectedLibraryId = "library-456"
        )
        handler.saveUserPreferences(userPreferences)

        // When
        handler.clearPreferences()

        // Then
        val libraryId = handler.getSelectedLibraryId()
        val prefs = handler.getUserPreferences()

        assertNull(libraryId)
        assertEquals("", prefs.host)
        assertEquals("", prefs.username)
        assertEquals("", prefs.token)
        assertNull(prefs.selectedLibraryId)
    }

    @Test
    fun selectedLibraryId_persistsAcrossMultipleReads() {
        // Given
        val libraryId = "library-persistent"
        handler.saveSelectedLibraryId(libraryId)

        // When - Read multiple times
        val read1 = handler.getSelectedLibraryId()
        val read2 = handler.getSelectedLibraryId()
        val read3 = handler.getSelectedLibraryId()

        // Then
        assertEquals(libraryId, read1)
        assertEquals(libraryId, read2)
        assertEquals(libraryId, read3)
    }
}
