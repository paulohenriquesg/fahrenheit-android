package com.paulohenriquesg.fahrenheit.navigation

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MenuConfig
 * Tests the data-driven menu system for different library types
 */
class MenuConfigTest {

    @Test
    fun `audiobooksMenu should have 7 items in correct order`() {
        // Given
        val mediaType = "book"

        // When
        val menu = MenuConfig.getMenuForLibraryType(mediaType)

        // Then
        assertEquals(7, menu.size)
        assertEquals("home", menu[0].id)
        assertEquals("library", menu[1].id)
        assertEquals("series", menu[2].id)
        assertEquals("collections", menu[3].id)
        assertEquals("authors", menu[4].id)
        assertEquals("narrators", menu[5].id)
        assertEquals("stats", menu[6].id)
    }

    @Test
    fun `podcastsMenu should have 3 items in correct order`() {
        // Given
        val mediaType = "podcast"

        // When
        val menu = MenuConfig.getMenuForLibraryType(mediaType)

        // Then
        assertEquals(3, menu.size)
        assertEquals("home", menu[0].id)
        assertEquals("latest", menu[1].id)
        assertEquals("library", menu[2].id)
    }

    @Test
    fun `unknown mediaType should return defaultMenu with 2 items`() {
        // Given
        val mediaType = "unknown"

        // When
        val menu = MenuConfig.getMenuForLibraryType(mediaType)

        // Then
        assertEquals(2, menu.size)
        assertEquals("home", menu[0].id)
        assertEquals("library", menu[1].id)
    }

    @Test
    fun `null mediaType should return defaultMenu`() {
        // Given
        val mediaType: String? = null

        // When
        val menu = MenuConfig.getMenuForLibraryType(mediaType)

        // Then
        assertEquals(2, menu.size)
        assertEquals("home", menu[0].id)
        assertEquals("library", menu[1].id)
    }

    @Test
    fun `audiobooksMenu should have correct actions`() {
        // Given
        val mediaType = "book"

        // When
        val menu = MenuConfig.getMenuForLibraryType(mediaType)

        // Then
        assertEquals(MenuAction.HOME, menu[0].action)
        assertEquals(MenuAction.LIBRARY, menu[1].action)
        assertEquals(MenuAction.SERIES, menu[2].action)
        assertEquals(MenuAction.COLLECTIONS, menu[3].action)
        assertEquals(MenuAction.AUTHORS, menu[4].action)
        assertEquals(MenuAction.NARRATORS, menu[5].action)
        assertEquals(MenuAction.STATS, menu[6].action)
    }

    @Test
    fun `podcastsMenu should have correct actions`() {
        // Given
        val mediaType = "podcast"

        // When
        val menu = MenuConfig.getMenuForLibraryType(mediaType)

        // Then
        assertEquals(MenuAction.HOME, menu[0].action)
        assertEquals(MenuAction.LATEST, menu[1].action)
        assertEquals(MenuAction.LIBRARY, menu[2].action)
    }

    @Test
    fun `commonItems should have 3 items`() {
        // When
        val commonItems = MenuConfig.commonItems

        // Then
        assertEquals(3, commonItems.size)
    }

    @Test
    fun `commonItems should have correct order and actions`() {
        // When
        val commonItems = MenuConfig.commonItems

        // Then
        assertEquals("select_library", commonItems[0].id)
        assertEquals(MenuAction.SELECT_LIBRARY, commonItems[0].action)

        assertEquals("settings", commonItems[1].id)
        assertEquals(MenuAction.SETTINGS, commonItems[1].action)

        assertEquals("logout", commonItems[2].id)
        assertEquals(MenuAction.LOGOUT, commonItems[2].action)
    }

    @Test
    fun `all menu items should have non-empty labels`() {
        // Given
        val mediaTypes = listOf("book", "podcast", null)

        // When & Then
        mediaTypes.forEach { mediaType ->
            val menu = MenuConfig.getMenuForLibraryType(mediaType)
            menu.forEach { menuItem ->
                assertTrue(
                    "Menu item ${menuItem.id} should have non-empty label",
                    menuItem.label.isNotEmpty()
                )
            }
        }

        MenuConfig.commonItems.forEach { menuItem ->
            assertTrue(
                "Common item ${menuItem.id} should have non-empty label",
                menuItem.label.isNotEmpty()
            )
        }
    }

    @Test
    fun `all menu items should have unique ids within their menu`() {
        // Given
        val mediaTypes = listOf("book", "podcast", null)

        // When & Then
        mediaTypes.forEach { mediaType ->
            val menu = MenuConfig.getMenuForLibraryType(mediaType)
            val ids = menu.map { it.id }
            val uniqueIds = ids.toSet()

            assertEquals(
                "Menu for $mediaType should have unique IDs",
                ids.size,
                uniqueIds.size
            )
        }

        val commonIds = MenuConfig.commonItems.map { it.id }
        val uniqueCommonIds = commonIds.toSet()
        assertEquals(
            "Common items should have unique IDs",
            commonIds.size,
            uniqueCommonIds.size
        )
    }

    @Test
    fun `audiobooks and podcasts should both have HOME and LIBRARY actions`() {
        // Given
        val audiobooksMenu = MenuConfig.getMenuForLibraryType("book")
        val podcastsMenu = MenuConfig.getMenuForLibraryType("podcast")

        // Then
        assertTrue(
            "Audiobooks menu should have HOME action",
            audiobooksMenu.any { it.action == MenuAction.HOME }
        )
        assertTrue(
            "Audiobooks menu should have LIBRARY action",
            audiobooksMenu.any { it.action == MenuAction.LIBRARY }
        )
        assertTrue(
            "Podcasts menu should have HOME action",
            podcastsMenu.any { it.action == MenuAction.HOME }
        )
        assertTrue(
            "Podcasts menu should have LIBRARY action",
            podcastsMenu.any { it.action == MenuAction.LIBRARY }
        )
    }
}
