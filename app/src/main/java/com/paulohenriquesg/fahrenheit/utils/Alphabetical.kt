package com.paulohenriquesg.fahrenheit.utils

import java.text.Collator
import java.util.Locale

/**
 * Ordering titles the way a reader expects.
 *
 * Kotlin's natural String order compares character codes, so "apple" lands
 * after "Zebra" and "Ártico" (U+00C1) after every unaccented title there is.
 * A Collator uses the device's language rules instead.
 */
object Alphabetical {

    fun <T> byName(name: (T) -> String?): Comparator<T> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.SECONDARY // case does not decide the order
        }
        return Comparator { a, b ->
            val left = name(a)
            val right = name(b)
            when {
                // Something with no title at all belongs at the end, not the top.
                left == null && right == null -> 0
                left == null -> 1
                right == null -> -1
                else -> collator.compare(left, right)
            }
        }
    }
}
