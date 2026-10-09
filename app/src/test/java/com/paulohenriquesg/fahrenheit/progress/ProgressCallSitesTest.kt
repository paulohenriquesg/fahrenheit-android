package com.paulohenriquesg.fahrenheit.progress

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Screens read progress from the shared store, never from the server
 * themselves (#207). These are the places that did: Home's covers
 * (`fetchProgress`, GET /api/me on each return), Latest Episodes and the
 * podcast page (`me().mediaProgress`), and the book page (the item's
 * `userMediaProgress`, read again on each return).
 */
class ProgressCallSitesTest {

    private val screens = listOf(
        "main/MainScreen.kt",
        "main/MainHandler.kt",
        "podcast/LatestEpisodesView.kt",
        "detail/DetailActivity.kt"
    )

    private val fetches = listOf("fetchProgress", "mediaProgress", "userMediaProgress", "userGetMediaProgress")

    @Test
    fun `no screen fetches progress itself`() {
        val root = File("src/main/java/com/paulohenriquesg/fahrenheit")
        val found = screens.flatMap { path ->
            File(root, path).readLines().mapIndexedNotNull { index, line ->
                fetches.firstOrNull { line.contains(it) }?.let { "$path:${index + 1} $it" }
            }
        }

        assertEquals(emptyList<String>(), found)
    }
}
