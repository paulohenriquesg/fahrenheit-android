package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home puts the tile only on shelves that lead somewhere matching (#123). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class HomeSeeAllTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val book: LibraryItem = Gson().fromJson(
        """{"id":"b1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"An Invented Book"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    @Test
    fun `the series shelf's tile opens the Series tab, and discover has none`() {
        var opened: MenuAction? = null
        val series = Shelf(
            id = "recent-series", label = "Recent Series", labelStringKey = "", type = "series",
            seriesEntities = listOf(Series(id = "s1", name = "A Series")), total = 12
        )
        val discover = Shelf(
            id = "discover", label = "Discover", labelStringKey = "", type = "book",
            bookEntities = listOf(book), total = 165
        )

        compose.setContent {
            FahrenheitTheme { PersonalizedHomeView(listOf(series, discover), "lib", onSeeAll = { opened = it }) }
        }
        compose.waitForIdle()

        compose.onNodeWithText("See all 165").assertDoesNotExist()
        compose.onNodeWithText("See all 12").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(MenuAction.SERIES, opened)
    }
}
