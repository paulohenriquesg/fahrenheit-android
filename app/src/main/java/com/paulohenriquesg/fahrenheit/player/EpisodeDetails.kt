package com.paulohenriquesg.fahrenheit.player

import androidx.core.text.HtmlCompat
import com.paulohenriquesg.fahrenheit.utils.listeningLength

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

    /** "Season S · Episode E · <length>" (a length as Latest Episodes writes it, #170), a missing part left out; null when nothing is known. */
    fun line(season: String?, episode: String?, length: Double?): String? = listOfNotNull(
        season?.takeIf { it.isNotBlank() }?.let { "Season ${it.trim()}" },
        episode?.takeIf { it.isNotBlank() }?.let { "Episode ${it.trim()}" },
        length?.takeIf { it > 0 }?.let { listeningLength(it) }
    ).joinToString(" · ").takeIf { it.isNotEmpty() }

    /** The feed's subtitle where there is one, else the description as plain text. */
    fun notes(subtitle: String?, description: String?): String? =
        subtitle?.takeIf { it.isNotBlank() }?.trim()
            ?: description?.let(::plainText)?.takeIf { it.isNotBlank() }

    /** As the text a feed's HTML renders to: tags out, entities read, blocks run together. */
    private fun plainText(html: String): String =
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
            .replace('\uFFFC', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
}
