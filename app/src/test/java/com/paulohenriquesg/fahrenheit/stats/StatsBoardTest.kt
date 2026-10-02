package com.paulohenriquesg.fahrenheit.stats

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsBoardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val summary = StatsSummary(
        totalListened = 651_600.0,
        today = 1_440.0,
        booksTouched = 38,
        activeDays = 83,
        averagePerDay = 7_860.0,
        weekdays = listOf(
            WeekdayShare("Mon", 60.0, 0.5f),
            WeekdayShare("Tue", 0.0, 0f),
            WeekdayShare("Wed", 0.0, 0f),
            WeekdayShare("Thu", 0.0, 0f),
            WeekdayShare("Fri", 0.0, 0f),
            WeekdayShare("Sat", 120.0, 1f),
            WeekdayShare("Sun", 0.0, 0f)
        ),
        topBooks = listOf(
            BookTime("a", "The Calculating Stars", 75_600.0, 1f),
            BookTime("b", "John Dies at the End", 32_400.0, 0.43f)
        ),
        recentSessions = listOf(
            SessionRow("s1", "li_1", "The Calculating Stars", 2_280.0, 20),
            SessionRow("s2", "li_2", "John Dies at the End", 1_560.0, 10)
        )
    )

    @Test
    fun `every headline figure is on the screen with its own value`() {
        compose.setContent { StatsBoard(summary) }

        compose.onNodeWithText("Total listened").assertIsDisplayed()
        compose.onNodeWithText("181 h").assertIsDisplayed()
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("24 min").assertIsDisplayed()
        compose.onNodeWithText("Books touched").assertIsDisplayed()
        compose.onNodeWithText("38").assertIsDisplayed()
        compose.onNodeWithText("Days with activity").assertIsDisplayed()
        compose.onNodeWithText("83").assertIsDisplayed()
        compose.onNodeWithText("Average per day").assertIsDisplayed()
        compose.onNodeWithText("2 h 11 min").assertIsDisplayed()
    }

    @Test
    fun `the whole week is labelled, quiet days included`() {
        compose.setContent { StatsBoard(summary) }

        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            compose.onNodeWithText(day).assertIsDisplayed()
        }
    }

    @Test
    fun `the books and the sessions the old screen threw away are shown`() {
        compose.setContent { StatsBoard(summary) }

        compose.onNodeWithText("Most listened").assertIsDisplayed()
        compose.onNodeWithText("21 h").assertIsDisplayed()
        compose.onNodeWithText("Recent sessions").assertIsDisplayed()
        compose.onNodeWithText("38 min").assertIsDisplayed()
        // The same book is both the most listened and the latest session, which
        // is what listening to one book looks like.
        compose.onAllNodesWithText("The Calculating Stars").assertCountEquals(2)
    }

    @Test
    fun `each recent session shows its cover`() {
        compose.setContent { StatsBoard(summary) }

        compose.onAllNodesWithContentDescription("The Calculating Stars").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("John Dies at the End").assertCountEquals(1)
    }

    @Test
    fun `nothing is stranded off the bottom of a short screen`() {
        // What went wrong before: the last card fell past 1080px with no way to
        // reach it.
        compose.setContent {
            // Only the height is pinned: a width wider than the test window
            // would push the content out of it and prove nothing.
            Box(modifier = Modifier.fillMaxWidth().requiredHeight(300.dp)) { StatsBoard(summary) }
        }

        compose.onNodeWithText("Recent sessions").performScrollTo().assertIsDisplayed()
    }
}
