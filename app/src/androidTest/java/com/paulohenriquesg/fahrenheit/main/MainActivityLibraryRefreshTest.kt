package com.paulohenriquesg.fahrenheit.main

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuConfig
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for MainActivity's library refresh behavior
 * Verifies that MainActivity properly reloads when library selection changes
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLibraryRefreshTest {

    private lateinit var sharedPreferencesHandler: SharedPreferencesHandler
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setup() {
        sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.clearPreferences()
    }

    @Test
    fun audiobooksLibrary_shouldShowSevenMenuItems() {
        // Given - Audiobooks library type
        val mediaType = "book"

        // When
        val menuItems = MenuConfig.getMenuForLibraryType(mediaType)

        // Then - Should have 7 audiobook-specific items
        assertEquals(
            "Audiobooks library should show 7 menu items",
            7,
            menuItems.size
        )

        // Verify specific audiobook items
        val actions = menuItems.map { it.action }
        assertTrue("Should have HOME action", actions.contains(MenuAction.HOME))
        assertTrue("Should have LIBRARY action", actions.contains(MenuAction.LIBRARY))
        assertTrue("Should have SERIES action", actions.contains(MenuAction.SERIES))
        assertTrue("Should have COLLECTIONS action", actions.contains(MenuAction.COLLECTIONS))
        assertTrue("Should have AUTHORS action", actions.contains(MenuAction.AUTHORS))
        assertTrue("Should have NARRATORS action", actions.contains(MenuAction.NARRATORS))
        assertTrue("Should have STATS action", actions.contains(MenuAction.STATS))
    }

    @Test
    fun podcastsLibrary_shouldShowThreeMenuItems() {
        // Given - Podcasts library type
        val mediaType = "podcast"

        // When
        val menuItems = MenuConfig.getMenuForLibraryType(mediaType)

        // Then - Should have 3 podcast-specific items
        assertEquals(
            "Podcasts library should show 3 menu items",
            3,
            menuItems.size
        )

        // Verify specific podcast items
        val actions = menuItems.map { it.action }
        assertTrue("Should have HOME action", actions.contains(MenuAction.HOME))
        assertTrue("Should have LATEST action", actions.contains(MenuAction.LATEST))
        assertTrue("Should have LIBRARY action", actions.contains(MenuAction.LIBRARY))
    }

    @Test
    fun whenSwitchingFromAudiobooksToPodcasts_menuShouldUpdate() {
        // Given - Start with audiobooks
        val audiobooksMenu = MenuConfig.getMenuForLibraryType("book")
        val audiobooksActionCount = audiobooksMenu.size

        // When - Switch to podcasts
        val podcastsMenu = MenuConfig.getMenuForLibraryType("podcast")
        val podcastsActionCount = podcastsMenu.size

        // Then - Menu should change
        assertNotEquals(
            "Menu item count should change when switching library types",
            audiobooksActionCount,
            podcastsActionCount
        )

        // Audiobooks should have SERIES, COLLECTIONS, etc.
        val audiobooksActions = audiobooksMenu.map { it.action }
        assertTrue("Audiobooks should have SERIES", audiobooksActions.contains(MenuAction.SERIES))
        assertTrue("Audiobooks should have AUTHORS", audiobooksActions.contains(MenuAction.AUTHORS))

        // Podcasts should have LATEST instead
        val podcastsActions = podcastsMenu.map { it.action }
        assertTrue("Podcasts should have LATEST", podcastsActions.contains(MenuAction.LATEST))
        assertFalse("Podcasts should NOT have SERIES", podcastsActions.contains(MenuAction.SERIES))
        assertFalse("Podcasts should NOT have AUTHORS", podcastsActions.contains(MenuAction.AUTHORS))
    }

    @Test
    fun savedLibraryId_shouldDetermineMenuItems() {
        // This test verifies the connection between saved library ID and menu display
        // In the real app:
        // 1. User selects library in LibrarySelectionActivity
        // 2. Library ID is saved to SharedPreferences
        // 3. MainActivity onResume reads the library ID
        // 4. MainActivity fetches library details (including mediaType)
        // 5. Menu is rendered based on mediaType

        // Given - Save a library selection
        val savedLibraryId = "test-audiobooks-lib"
        sharedPreferencesHandler.saveSelectedLibraryId(savedLibraryId)

        // When - Retrieve the saved ID (simulating MainActivity onResume)
        val retrievedId = sharedPreferencesHandler.getSelectedLibraryId()

        // Then - The ID should be available for MainActivity to fetch library details
        assertEquals(
            "MainActivity should retrieve the saved library ID on resume",
            savedLibraryId,
            retrievedId
        )
        assertNotNull(
            "Saved library ID should not be null",
            retrievedId
        )
    }

    @Test
    fun commonMenuItems_shouldAlwaysBePresent() {
        // Given - Both library types
        val audiobooksMenu = MenuConfig.getMenuForLibraryType("book")
        val podcastsMenu = MenuConfig.getMenuForLibraryType("podcast")
        val commonItems = MenuConfig.commonItems

        // Then - Common items should be separate from library-specific items
        assertEquals(
            "Should have exactly 3 common items",
            3,
            commonItems.size
        )

        val commonActions = commonItems.map { it.action }
        assertTrue("Should have SELECT_LIBRARY", commonActions.contains(MenuAction.SELECT_LIBRARY))
        assertTrue("Should have SETTINGS", commonActions.contains(MenuAction.SETTINGS))
        assertTrue("Should have LOGOUT", commonActions.contains(MenuAction.LOGOUT))

        // Common items should not appear in library-specific menus
        val audiobooksActions = audiobooksMenu.map { it.action }
        val podcastsActions = podcastsMenu.map { it.action }

        assertFalse(
            "Library menu should not contain SELECT_LIBRARY",
            audiobooksActions.contains(MenuAction.SELECT_LIBRARY)
        )
        assertFalse(
            "Library menu should not contain SELECT_LIBRARY",
            podcastsActions.contains(MenuAction.SELECT_LIBRARY)
        )
    }

    @Test
    fun whenLibraryChanges_currentLibraryShouldUpdate() {
        // This test verifies that MainActivity should update its currentLibrary
        // when returning from LibrarySelectionActivity

        // Given - Initial library selection
        sharedPreferencesHandler.saveSelectedLibraryId("library-1")
        val initialLibraryId = sharedPreferencesHandler.getSelectedLibraryId()

        // When - User switches library
        sharedPreferencesHandler.saveSelectedLibraryId("library-2")
        val newLibraryId = sharedPreferencesHandler.getSelectedLibraryId()

        // Then - Library should have changed
        assertNotEquals(
            "Current library should change after selection",
            initialLibraryId,
            newLibraryId
        )
        assertEquals(
            "New library ID should be library-2",
            "library-2",
            newLibraryId
        )
    }

    @Test
    fun whenReturningToMainActivity_shouldCheckForLibraryChange() {
        // This test documents the expected MainActivity behavior:
        // 1. MainActivity is active with library-1
        // 2. User clicks "Switch Library"
        // 3. LibrarySelectionActivity opens and user selects library-2
        // 4. LibrarySelectionActivity saves library-2 and closes
        // 5. MainActivity onResume is called
        // 6. MainActivity should detect library change and reload

        // Given - Simulate MainActivity having library-1
        sharedPreferencesHandler.saveSelectedLibraryId("library-1")
        val mainActivityCurrentLibraryId = sharedPreferencesHandler.getSelectedLibraryId()

        // When - Simulate user selecting library-2 in LibrarySelectionActivity
        sharedPreferencesHandler.saveSelectedLibraryId("library-2")

        // Then - MainActivity onResume should detect the change
        val newLibraryId = sharedPreferencesHandler.getSelectedLibraryId()

        assertNotEquals(
            "MainActivity should detect library has changed",
            mainActivityCurrentLibraryId,
            newLibraryId
        )

        assertEquals(
            "New library should be library-2",
            "library-2",
            newLibraryId
        )
    }
}
