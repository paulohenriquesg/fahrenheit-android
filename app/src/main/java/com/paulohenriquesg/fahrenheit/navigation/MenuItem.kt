package com.paulohenriquesg.fahrenheit.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a menu item in the navigation drawer
 */
data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: MenuAction
)

/**
 * Actions that can be triggered by menu items
 */
enum class MenuAction {
    HOME,           // Fetch personalized view
    LIBRARY,        // Show all items
    SERIES,         // Navigate to series browse (audiobooks only)
    COLLECTIONS,    // Navigate to collections (audiobooks only)
    AUTHORS,        // Navigate to authors browse (audiobooks only)
    NARRATORS,      // Navigate to narrators browse (audiobooks only)
    STATS,          // Navigate to stats screen (audiobooks only)
    LATEST,         // Show latest episodes (podcasts only)
    SETTINGS,       // Open settings activity
    LOGOUT,         // Clear prefs and logout
    SELECT_LIBRARY  // Open library selection screen
}
