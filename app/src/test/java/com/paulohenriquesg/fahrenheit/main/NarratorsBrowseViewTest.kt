package com.paulohenriquesg.fahrenheit.main

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.api.Narrator
import com.paulohenriquesg.fahrenheit.author.AuthorDetailActivity
import com.paulohenriquesg.fahrenheit.author.Person
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderTags
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Narrators section (#230): a grid like Authors, with round initials since
 * the server keeps no narrator photos; a narrator opens their page.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NarratorsBrowseViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @After
    fun drain() = shadowOf(Looper.getMainLooper()).idle()

    private fun show(load: suspend (String) -> Result<List<Narrator>>?) {
        compose.setContent { FahrenheitTheme { NarratorsBrowseView("lib", load) } }
        compose.waitForIdle()
    }

    @Test
    fun `the grid lists the narrators by name, with initials and how many books they read`() {
        show { Result.success(listOf(Narrator("Ug%3D%3D", "Second Voice", 1), Narrator("QSBSZWFkZXI%3D", "A Reader", 12))) }

        compose.onNodeWithText("Narrators").assertExists()
        compose.onNodeWithText("A Reader").assertExists()
        compose.onNodeWithText("AR").assertExists()
        compose.onNodeWithText("12 books").assertExists()
        compose.onNodeWithText("1 book").assertExists()
    }

    @Test
    fun `while they load, round grey cards`() {
        val never = CompletableDeferred<Result<List<Narrator>>>()
        show { never.await() }

        compose.onNodeWithText("Narrators").assertExists()
        assertTrue(compose.onAllNodesWithTag(PlaceholderTags.ROUND, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `a narrator opens their page`() {
        show { Result.success(listOf(Narrator("QSBSZWFkZXI%3D", "A Reader", 12))) }

        compose.onNodeWithText("A Reader").performSemanticsAction(SemanticsActions.OnClick)

        val started = shadowOf(compose.activity).nextStartedActivity
        assertEquals(AuthorDetailActivity::class.java.name, started.component?.className)
        assertEquals(Person.Narrator("lib", "A Reader"), AuthorDetailActivity.personOf(started))
        assertEquals("A Reader", AuthorDetailActivity.previewOf(started)?.title)
    }
}
