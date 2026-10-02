package com.paulohenriquesg.fahrenheit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * One set of colours, handed to both MaterialThemes.
 *
 * The TV library has no text field, progress indicator or icon, so those come
 * from the phone library - and the phone components read the phone
 * MaterialTheme. Leaving it unset gave them the default light scheme, which is
 * how the dark login screen came out with near-white slabs for fields and
 * white text on top of them.
 */
private class Palette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onPrimary: Color,
    val outline: Color
)

private val DarkPalette = Palette(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF4A3D5C),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFCAC4D0),
    onPrimary = Color(0xFFFFFFFF),
    outline = Color(0xFF8E8699)
)

private val LightPalette = Palette(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFE4DFEA),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF474152),
    onPrimary = Color(0xFFFFFFFF),
    outline = Color(0xFF6F6779)
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FahrenheitTheme(
    content: @Composable () -> Unit,
) {
    // The device's setting is the starting point, not the last word: what the
    // person chose in settings is held by ThemeManager, which resolved the two
    // at startup.
    val isDarkTheme by ThemeManager.isDarkTheme
    val palette = if (isDarkTheme) DarkPalette else LightPalette

    val tvColorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            onBackground = palette.onBackground,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.onSurfaceVariant,
            onPrimary = palette.onPrimary,
            onSecondary = Color(0xFFFFFFFF),
            onSecondaryContainer = Color(0xFFFFFFFF),
            onTertiary = Color(0xFFFFFFFF),
            onTertiaryContainer = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF4A4458),
            tertiaryContainer = Color(0xFF633B48),
            border = palette.outline
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            onBackground = palette.onBackground,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.onSurfaceVariant,
            onPrimary = palette.onPrimary,
            border = palette.outline
        )
    }

    val phoneColorScheme = if (isDarkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            onBackground = palette.onBackground,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.onSurfaceVariant,
            onPrimary = palette.onPrimary,
            outline = palette.outline
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            onBackground = palette.onBackground,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.onSurfaceVariant,
            onPrimary = palette.onPrimary,
            outline = palette.outline
        )
    }

    // Text that names no colour takes LocalContentColor, which defaults to
    // near-black in both libraries: on a dark background that is grey on black,
    // which is what the greeting was. Say what it should be once, here.
    MaterialTheme(
        colorScheme = tvColorScheme,
        typography = Typography
    ) {
        androidx.compose.material3.MaterialTheme(colorScheme = phoneColorScheme) {
            CompositionLocalProvider(
                androidx.tv.material3.LocalContentColor provides palette.onBackground,
                androidx.compose.material3.LocalContentColor provides palette.onBackground
            ) {
                content()
            }
        }
    }
}
