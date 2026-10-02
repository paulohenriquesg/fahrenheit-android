package com.paulohenriquesg.fahrenheit.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The date format the server was configured with, as something Java can use.
 *
 * Audiobookshelf sends it at login as a date-fns pattern. Most of them coincide
 * with Java's, but the ordinal day ("do" - 1st, 2nd) has no equivalent and would
 * render as stray letters if left in.
 */
object ServerDateFormat {

    fun pattern(serverFormat: String?): String? {
        val trimmed = serverFormat?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val java = trimmed.replace(Regex("""\bdo\b"""), "d")
        // Anything Java cannot parse is refused here rather than thrown from
        // whichever screen happens to format a date first.
        return runCatching { SimpleDateFormat(java, Locale.getDefault()) }
            .map { java }
            .getOrNull()
    }
}
