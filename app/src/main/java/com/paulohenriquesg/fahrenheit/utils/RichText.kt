package com.paulohenriquesg.fahrenheit.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml

/**
 * A server description, rendered rather than flattened.
 *
 * Podcast descriptions arrive as HTML and were printed as they came, tags and
 * numeric entities included (#57). Rendering them keeps what the publisher
 * emphasised, which stripping would have thrown away.
 */
object RichText {

    fun fromHtml(source: String?): AnnotatedString {
        if (source.isNullOrBlank()) return AnnotatedString("")
        return trimmed(AnnotatedString.fromHtml(source.replace(WRITTEN_NEWLINE, "<br>")))
    }

    /**
     * Some descriptions hold the two characters backslash and n where a line
     * break was meant, and showed them as written (#194). Two in a row are a
     * paragraph break.
     */
    private val WRITTEN_NEWLINE = Regex("""(\\r)?\\n""")

    private fun trimmed(rendered: AnnotatedString): AnnotatedString {
        // Html leaves the paragraph's trailing newlines on the end.
        val end = rendered.text.indexOfLast { !it.isWhitespace() } + 1
        val start = rendered.text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
        return if (start == 0 && end == rendered.text.length) {
            rendered
        } else {
            rendered.subSequence(start, end)
        }
    }
}
