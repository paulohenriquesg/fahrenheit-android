package com.paulohenriquesg.fahrenheit.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A feed's publication date, written the way the server asks, or as sent when it
 * cannot be read.
 *
 * @param serverFormat the server's configured date format, from the login
 *   response. Day-first when it sent none or sent one Java cannot use.
 */
fun formatPubDate(pubDate: String?, serverFormat: String? = null): String {
    if (pubDate == null) return ""
    // Feeds write day and month names in English (RFC 822) whatever the TV's
    // language; parsing them with the device locale failed on e.g. Portuguese.
    val formats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("yyyy", Locale.US)
    )
    val pattern = ServerDateFormat.pattern(serverFormat) ?: "dd/MM/yyyy"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    for (format in formats) {
        try {
            return format.parse(pubDate)?.let { formatter.format(it) } ?: pubDate
        } catch (e: ParseException) {
            // Continue to the next format
        }
    }
    return pubDate
}
