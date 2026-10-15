package com.paulohenriquesg.fahrenheit.stats

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.main.StatsBrowseView
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Stats while they load (#225, from #226): grey boxes in the board's shape, not a loading line. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class StatsPlaceholderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `loading draws the board's shape under its title, still and unfocusable`() {
        compose.setContent { FahrenheitTheme { StatsBrowseView(stats = null, isLoading = true) } }
        compose.waitForIdle()

        compose.onNodeWithText("Listening statistics", ignoreCase = true).assertExists()
        compose.onNodeWithText("Loading", substring = true).assertDoesNotExist()
        compose.onNodeWithTag(StatsTags.PLACEHOLDER).assertExists()
        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.SHAPE).fetchSemanticsNodes().size >= 5)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).assertCountEquals(0)
    }
}
