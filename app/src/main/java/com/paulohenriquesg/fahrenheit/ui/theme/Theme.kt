package com.paulohenriquesg.fahrenheit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FahrenheitTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Observe theme state from ThemeManager
    val isDarkTheme by ThemeManager.isDarkTheme

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = Purple80,
            secondary = PurpleGrey80,
            tertiary = Pink80,
            background = Color(0xFF121212), // Dark Gray
            surface = Color(0xFF1E1E1E), // Slightly lighter dark for cards
            onBackground = Color(0xFFFFFFFF), // White for text
            onSurface = Color(0xFFFFFFFF), // White for text on cards
            onPrimary = Color(0xFFBB86FC) // Light Purple for icons
        )
    } else {
        lightColorScheme(
            primary = Purple40,
            secondary = PurpleGrey40,
            tertiary = Pink40,
            background = Color.White, // White background
            surface = Color(0xFFF5F5F5), // Light gray for cards
            onBackground = Color(0xFF000000), // Black for text
            onSurface = Color(0xFF000000) // Black for text on cards
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}