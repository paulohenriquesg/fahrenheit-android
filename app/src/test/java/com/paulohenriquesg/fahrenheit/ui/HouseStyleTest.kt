package com.paulohenriquesg.fahrenheit.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The house rules from docs/ui-style-guide.md that can be checked by reading the
 * source. Each one is here because breaking it shipped a bug.
 */
class HouseStyleTest {

    private val sources: List<File> = File("src/main/java/com/paulohenriquesg/fahrenheit")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    private fun where(file: File, lineNumber: Int) =
        "${file.relativeTo(File("src/main/java"))}:$lineNumber"

    @Test
    fun `the sources are actually being read`() {
        // Without this, a wrong path would make every rule below pass silently.
        assert(sources.size > 50) { "only found ${sources.size} Kotlin files to check" }
    }

    @Test
    fun `every lazy list item carries a key`() {
        // Duplicate keys crash Compose, and no key at all means focus and scroll
        // position jump when the list changes (#55).
        val offenders = mutableListOf<String>()
        // A lazy list's items(...) opens a statement; a repository function
        // called items() does not.
        val itemsCall = Regex("""^(items|itemsIndexed)\s*\(""")
        sources.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (!itemsCall.containsMatchIn(line.trim())) return@forEachIndexed
                // The key may sit on a following line of the same call.
                val call = lines.drop(index).take(4).joinToString(" ")
                if (!call.contains("key =") && !call.contains("key=")) {
                    offenders += where(file, index + 1)
                }
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `screens use the TV components where the TV library has one`() {
        // Mixing the two libraries is what put near-white slabs on the dark
        // login screen: the phone components read a MaterialTheme of their own.
        // The TV library has no text field, progress indicator or icon, so
        // those stay; Text, Button, Card and Surface have TV equivalents built
        // for focus.
        val phoneOnly = Regex("""import androidx\.compose\.material3\.(Text|Surface|Button)$""")
        val offenders = mutableListOf<String>()
        sources.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (phoneOnly.containsMatchIn(line.trim())) offenders += where(file, index + 1)
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `the sections on the rail take their title from ScreenTitle`() {
        // Each one styled and placed its own, and they sat in nine different
        // places (#82). A title-sized headline in one of these files is a title
        // that has gone back to doing that; headlineSmall is a figure on a tile.
        val titleStyle = Regex("""typography\.headline(Large|Medium)""")
        val railSections = listOf(
            "main/MainScreen.kt",
            "main/BrowseViews.kt",
            "stats/StatsBoard.kt",
            "podcast/LatestEpisodesView.kt",
            "settings/SettingsView.kt",
            "library/SwitchLibraryView.kt"
        )
        val root = File("src/main/java/com/paulohenriquesg/fahrenheit")
        val offenders = mutableListOf<String>()
        railSections.forEach { path ->
            val file = File(root, path)
            assert(file.isFile) { "$path has moved; point this rule at its new place" }
            file.readLines().forEachIndexed { index, line ->
                if (titleStyle.containsMatchIn(line)) offenders += where(file, index + 1)
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `every String format passes a Locale`() {
        // The default locale renders digits in the device's own numerals, so a
        // Persian device would read "۲ h ۱۱ min".
        val offenders = mutableListOf<String>()
        sources.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.contains("String.format(") && !line.contains("Locale.")) {
                    offenders += where(file, index + 1)
                }
            }
        }
        assertEquals(emptyList<String>(), offenders)
    }
}
