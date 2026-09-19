package com.paulohenriquesg.fahrenheit.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/** A feed's publication date as dd/MM/yyyy, or as sent when it can't be read. */
fun formatPubDate(pubDate: String?): String {
    if (pubDate == null) return ""
    // Feeds write day and month names in English (RFC 822) whatever the TV's
    // language; parsing them with the device locale failed on e.g. Portuguese.
    val formats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("yyyy", Locale.US)
    )
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    for (format in formats) {
        try {
            return format.parse(pubDate)?.let { formatter.format(it) } ?: pubDate
        } catch (e: ParseException) {
            // Continue to the next format
        }
    }
    return pubDate
}
