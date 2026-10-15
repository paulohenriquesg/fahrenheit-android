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

    /**
     * Screens whose loading state is still a bare loading line or spinner, each
     * with the issue that converts it. That lane takes its entry off. A key is
     * a whole file, or a file and the one string it may still show.
     */
    private val bareLoadingAllowed = mapOf(
        "com/paulohenriquesg/fahrenheit/detail/DetailActivity.kt" to "#225",
        "com/paulohenriquesg/fahrenheit/author/AuthorDetailScreen.kt" to "#225",
        "com/paulohenriquesg/fahrenheit/player/PlayerActivity.kt" to "#223",
        // Stats was not among #224's screens; it needs an issue of its own.
        "com/paulohenriquesg/fahrenheit/main/BrowseViews.kt R.string.loading_stats" to "none yet"
    )

    /** Each `R.string.loading*` [lines] use, as "<path> <string>" and its line. */
    private fun loadingTexts(path: String, lines: List<String>): List<Pair<String, Int>> {
        val loading = Regex("""R\.string\.loading\w*""")
        return lines.flatMapIndexed { index, line ->
            loading.findAll(line).map { "$path ${it.value}" to index + 1 }.toList()
        }
    }

    private fun bareLoadingOffenders(path: String, lines: List<String>): List<String> =
        loadingTexts(path, lines)
            .filter { (use, _) -> path !in bareLoadingAllowed && use !in bareLoadingAllowed }
            .map { (use, line) -> "$use:$line" }

    @Test
    fun `no screen waits behind a bare loading line`() {
        // A loading line on an empty screen gave the screen no shape until
        // everything arrived at once (#224). A screen draws its own layout
        // from the first frame, with still placeholders where data is to come;
        // see "Loading" in docs/ui-style-guide.md.
        val offenders = sources.flatMap { file ->
            bareLoadingOffenders(file.relativeTo(File("src/main/java")).path, file.readLines())
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `the loading rule catches a screen that brings one back`() {
        val screen = listOf(
            "if (isLoading) {",
            "    Text(text = stringResource(R.string.loading_series))",
            "}"
        )
        assertEquals(
            listOf("com/paulohenriquesg/fahrenheit/main/SomeView.kt R.string.loading_series:2"),
            bareLoadingOffenders("com/paulohenriquesg/fahrenheit/main/SomeView.kt", screen)
        )
        // An allowed file's own exception covers that string only.
        assertEquals(
            listOf("com/paulohenriquesg/fahrenheit/main/BrowseViews.kt R.string.loading:1"),
            bareLoadingOffenders("com/paulohenriquesg/fahrenheit/main/BrowseViews.kt", listOf("Text(stringResource(R.string.loading))"))
        )
        // A string that merely ends in "loading" is not one of them.
        assertEquals(
            emptyList<String>(),
            bareLoadingOffenders("com/paulohenriquesg/fahrenheit/main/SomeView.kt", listOf("Notice(stringResource(R.string.favourites_loading))"))
        )
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
