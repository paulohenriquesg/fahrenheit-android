package com.paulohenriquesg.fahrenheit.ui

/**
 * Keys for lazy lists, unique by construction.
 *
 * A lazy list needs stable keys so focus and scroll survive a refresh, but
 * Compose throws if two items share one. Server ids are not safe on their own:
 * a podcast library's shelves list one entry per episode in progress, so the
 * same library item id can appear several times in a single row (#55).
 *
 * The first use of an id keeps it; later ones carry their occurrence number,
 * so keys stay the same between refreshes of the same list.
 */
object StableKeys {

    fun <T> of(items: List<T>, id: (T) -> String): List<String> {
        val seen = mutableMapOf<String, Int>()
        return items.map { item ->
            val raw = id(item)
            val count = (seen[raw] ?: 0) + 1
            seen[raw] = count
            if (count == 1) raw else "$raw#$count"
        }
    }
}
