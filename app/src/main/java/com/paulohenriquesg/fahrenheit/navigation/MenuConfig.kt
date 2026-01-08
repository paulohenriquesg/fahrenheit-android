package com.paulohenriquesg.fahrenheit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*

/**
 * Configuration for menu items based on library type
 */
object MenuConfig {
    /**
     * Get menu items for a specific library type
     * @param mediaType The library media type ("book" or "podcast")
     * @return List of menu items appropriate for the library type
     */
    fun getMenuForLibraryType(mediaType: String?): List<MenuItem> {
        return when (mediaType) {
            "book" -> audiobooksMenu
            "podcast" -> podcastsMenu
            else -> defaultMenu
        }
    }

    /**
     * Menu items for audiobooks library
     */
    private val audiobooksMenu = listOf(
        MenuItem(
            id = "home",
            label = "Home",
            icon = Icons.Filled.Home,
            action = MenuAction.HOME
        ),
        MenuItem(
            id = "library",
            label = "Library",
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            action = MenuAction.LIBRARY
        ),
        MenuItem(
            id = "series",
            label = "Series",
            icon = Icons.Filled.FolderSpecial,
            action = MenuAction.SERIES
        ),
        MenuItem(
            id = "collections",
            label = "Collections",
            icon = Icons.Filled.Collections,
            action = MenuAction.COLLECTIONS
        ),
        MenuItem(
            id = "authors",
            label = "Authors",
            icon = Icons.Filled.Person,
            action = MenuAction.AUTHORS
        ),
        MenuItem(
            id = "narrators",
            label = "Narrators",
            icon = Icons.Filled.RecordVoiceOver,
            action = MenuAction.NARRATORS
        ),
        MenuItem(
            id = "stats",
            label = "Stats",
            icon = Icons.Filled.BarChart,
            action = MenuAction.STATS
        )
    )

    /**
     * Menu items for podcasts library
     */
    private val podcastsMenu = listOf(
        MenuItem(
            id = "home",
            label = "Home",
            icon = Icons.Filled.Home,
            action = MenuAction.HOME
        ),
        MenuItem(
            id = "latest",
            label = "Latest",
            icon = Icons.Filled.NewReleases,
            action = MenuAction.LATEST
        ),
        MenuItem(
            id = "library",
            label = "Library",
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            action = MenuAction.LIBRARY
        )
    )

    /**
     * Default menu items when library type is unknown
     */
    private val defaultMenu = listOf(
        MenuItem(
            id = "home",
            label = "Home",
            icon = Icons.Filled.Home,
            action = MenuAction.HOME
        ),
        MenuItem(
            id = "library",
            label = "Library",
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            action = MenuAction.LIBRARY
        )
    )

    /**
     * Common menu items shown for all library types
     * These appear after a separator below the library-specific items
     */
    val commonItems = listOf(
        MenuItem(
            id = "select_library",
            label = "Switch Library",
            icon = Icons.Filled.SwapHoriz,
            action = MenuAction.SELECT_LIBRARY
        ),
        MenuItem(
            id = "settings",
            label = "Settings",
            icon = Icons.Filled.Settings,
            action = MenuAction.SETTINGS
        ),
        MenuItem(
            id = "logout",
            label = "Logout",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            action = MenuAction.LOGOUT
        )
    )
}
