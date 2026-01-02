package com.paulohenriquesg.fahrenheit.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Application-wide constants to avoid magic numbers and ensure consistency.
 */
object Constants {

    /**
     * UI and Layout constants
     */
    object UI {
        /** Standard image size for covers, author photos, etc. */
        val IMAGE_SIZE: Dp = 200.dp

        /** Default number of columns in grid layouts */
        const val GRID_COLUMNS = 4

        /** Small padding between related elements */
        val PADDING_SMALL: Dp = 8.dp

        /** Standard padding for screens and containers */
        val PADDING_STANDARD: Dp = 16.dp

        /** Maximum number of lines for description text before ellipsis */
        const val MAX_DESCRIPTION_LINES = 5

        /** Spacing between major sections */
        val SPACING_SECTION: Dp = 16.dp
    }

    /**
     * Time and duration constants
     */
    object Time {
        /** Interval for updating media progress (5 seconds) */
        const val PROGRESS_UPDATE_INTERVAL_MS = 5000L
    }

    /**
     * Date format patterns
     */
    object DateFormat {
        /** Standard date format for displaying dates: day/month/year */
        const val DISPLAY_FORMAT = "dd/MM/yyyy"

        /** RFC 822 date format for parsing podcast pub dates */
        const val RFC_822_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z"

        /** Year-only format for parsing simple year values */
        const val YEAR_ONLY_FORMAT = "yyyy"
    }
}
