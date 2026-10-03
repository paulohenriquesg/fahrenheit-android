package com.paulohenriquesg.fahrenheit.player

/**
 * What an episode says about itself under its title (#108; frame "C,
 * playing an episode"): a badge when it is not a regular one, a details line,
 * and the first lines of its notes.
 */
object EpisodeDetails {

    /** "Bonus" or "Trailer"; a regular ("full") episode has none. */
    fun badge(episodeType: String?): String? = when (episodeType?.trim()?.lowercase()) {
        "bonus" -> "Bonus"
        "trailer" -> "Trailer"
        else -> null
    }

    /** "Season S · Episode E · <length>", a missing part left out; null when nothing is known. */
    fun line(season: String?, episode: String?, length: Double?): String? = listOfNotNull(
        season?.takeIf { it.isNotBlank() }?.let { "Season ${it.trim()}" },
        episode?.takeIf { it.isNotBlank() }?.let { "Episode ${it.trim()}" },
        length?.takeIf { it > 0 }?.let { PlaybackPosition.spoken(it) }
    ).joinToString(" · ").takeIf { it.isNotEmpty() }

    /** The feed's subtitle where there is one, else the description as plain text. */
    fun notes(subtitle: String?, description: String?): String? =
        subtitle?.takeIf { it.isNotBlank() }?.trim()
            ?: description?.let(::plainText)?.takeIf { it.isNotBlank() }

    /** Tags out, paragraphs run together, the common entities read. */
    private fun plainText(html: String): String =
        html.replace(Regex("""(?i)</p\s*>|<br\s*/?>"""), " ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
