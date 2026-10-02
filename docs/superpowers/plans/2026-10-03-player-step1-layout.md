# Player step 1: layout C, stacked bars, chapter skip - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The player screen in layout C. A 250 dp cover sits beside the series line, title, byline and current chapter. Under them are a chapter bar with chapter-relative times, a slim book bar with ticks and the book times, and a five-button transport with chapter skip. Room is left on the same line for actions (#107, step 1).

**Architecture:**
- **New pure models:** `NowPlaying` gains `kicker` and `byline`, replacing the `line` lambda. `ChapterClock` places a position in its chapter and decides where chapter skip goes. `SeekStep` sizes D-pad seeks.
- **New composable:** `PlaybackBar`, a focusable TV bar that seeks with Left/Right.
- **`MediaPlayerController`** is rebuilt around the bars and the five-button transport, and takes a `trailing` slot for actions.
- **`PlayerScreen`** is laid out as frame C. "Go to podcast" moves into the trailing slot.

**Tech Stack:** Kotlin, Compose TV Material, Media3, JUnit4 + Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-03-player-revamp-design.md` (step 1); mocks `docs/mocks/player.html`, frames "C…" and "C, playing an episode".

## Global Constraints

- Branch `feat/player-revamp` (has the spec and mocks).
- Test-first throughout; Gradle runs capture exit codes (zsh: `pipefail`, never `PIPESTATUS`).
- Commits: no bot-attribution trailer; `git commit -F <file>`.
- Imports are pruned by the compiler or lint, never by text search.
- TV components only for focusable controls (`androidx.tv.material3`); phone components refuse focus on the stick.
- Lengths always via `PlaybackPosition.spoken`.
- Public repo: fixtures use invented titles only.
- **Not in step 1:**
  - speed (times assume 1×), Sleep, Chapters, About, the episode details line, episode previous/next, the rail entry, the cover-coloured background;
  - **the description leaves the player screen** and returns in About in step 3.

## Review Focus

1. **A book without chapters:** one bar (the book), no chapter buttons, times as in #93. Pinned in Task 4 (`without chapters there is one bar and no chapter buttons`).
2. **Chapters whose `end` is missing, or that are out of order:** the current chapter is still found, and skip still moves forward. Pinned in Task 2 (`chapters without an end run to the next start`, `chapters out of order are sorted`).
3. **A position past the last chapter's end:** it reads as the last chapter, with nothing left, and Next is a no-op. Pinned in Task 2 (`past the last chapter's end is still the last chapter`).
4. **Up from the transport:** focus reaches the chapter bar, and Left/Right there seek. Pinned in Task 4 (`up from play reaches the chapter bar, which seeks`).
5. **An episode:** one bar, no chapter buttons, and Go to podcast in the trailing slot. Pinned in Task 5 (`an episode offers its podcast beside the transport`).

---

### Task 1: `NowPlaying` says what it is: kicker and byline

**Files:**
- Modify `player/NowPlaying.kt`.
- Tests:
  - `NowPlayingTest.kt`;
  - every test that builds a `NowPlaying` with `line` (`PlaybackQueueTest`, `PlaybackServiceTest`, `PlaybackReportingTest`, `PlayerStartTest`, `TransportTest`, `TransportFocusTest`, `BookPlaybackTest`, `PlayerScreenTest`).

**Interfaces:**
- Produces `NowPlaying(itemId, title, timeline, mediaDuration, chapters, episodeId, goToPodcast, description, kicker: String? = null, byline: String? = null)`.
- `line` and `subtitle()` are removed.

- [ ] **Step 1: Failing tests.**
  - In `NowPlayingTest`, replace the two `subtitle` tests with the tests below.
  - Add a fixture `seriesBook`: as `book`, but with `"series":[{"id":"s1","name":"The Long Way","sequence":"2"}]`, `"narratorName":"A Reader"` and `"genres":["Science fiction"]`.
  - Add `standalone`: as `book`, with `"genres":["Science fiction","Thriller"]` and no series.

```kotlin
    @Test
    fun `a book in a series names the series and its number`() =
        assertEquals("The Long Way · Book 2", NowPlaying.of(seriesBook, episodeId = null, now = now)!!.kicker)

    @Test
    fun `a standalone book names its first genre`() =
        assertEquals("Science fiction", NowPlaying.of(standalone, episodeId = null, now = now)!!.kicker)

    @Test
    fun `a book with neither has no line above the title`() =
        assertNull(NowPlaying.of(book, episodeId = null, now = now)!!.kicker)

    @Test
    fun `the byline says who wrote it and who reads it`() =
        assertEquals("Andy Weir · read by A Reader", NowPlaying.of(seriesBook, episodeId = null, now = now)!!.byline)

    @Test
    fun `without a narrator the byline is the author`() =
        assertEquals("Andy Weir", NowPlaying.of(book, episodeId = null, now = now)!!.byline)

    @Test
    fun `an episode's line above the title is its show and when it came out`() {
        val playing = NowPlaying.of(podcast, episodeId = "e295", now = now)!!
        assertEquals("Welcome to Night Vale · Yesterday", playing.kicker)
        assertNull(playing.byline)
    }
```

- [ ] **Step 2: Run red.** Command: `./gradlew :app:testDebugUnitTest --tests '*NowPlayingTest'`. It should fail to compile on `kicker` and `byline`.

- [ ] **Step 3: Implement.**
  - In `NowPlaying`, replace `private val line: (Double) -> String` with these two properties, and delete `subtitle()`:

```kotlin
    /** Above the title: the series and number, a standalone book's genre, or an episode's show and date. */
    val kicker: String? = null,
    /** Who wrote it and who reads it; null for an episode. */
    val byline: String? = null
```

  - Update the class KDoc: "What differs between a book and an episode is the kicker, the byline, the chapter marks, and a way to the podcast."
  - In `of`, the book branch:

```kotlin
                    kicker = seriesLine(metadata) ?: metadata.genres?.firstOrNull { it.isNotBlank() },
                    byline = listOfNotNull(
                        metadata.authorName?.takeIf { it.isNotBlank() },
                        metadata.narratorName?.takeIf { it.isNotBlank() }?.let { "read by $it" }
                    ).joinToString(" · ").takeIf { it.isNotEmpty() }
```

  - The episode branch:

```kotlin
                kicker = listOfNotNull(metadata.title, published.takeIf { it.isNotEmpty() }).joinToString(" · "),
                byline = null
```

  - Add to the companion:

```kotlin
        /** "Series · Book N", or the series alone when it has no number. */
        private fun seriesLine(metadata: com.paulohenriquesg.fahrenheit.api.Metadata): String? {
            val series = metadata.series?.firstOrNull()
            val name = series?.name?.takeIf { it.isNotBlank() } ?: metadata.seriesName?.takeIf { it.isNotBlank() } ?: return null
            val number = series?.sequence?.takeIf { it.isNotBlank() } ?: return name
            return "$name · Book $number"
        }
```

  - Use the metadata class's real name, read from `LibraryItemResponse.kt`.
  - Mechanically update the tests that pass `line`: delete `, line = { "" }` and `line = { "" }`, and turn the trailing lambda `) { "" }` into `)`. Confirm with the compiler.
  - In `PlayerScreen.kt`, replace `nowPlaying.subtitle(currentTime)` with `nowPlaying.kicker` for now, so it compiles. Task 5 rebuilds the screen.

- [ ] **Step 4:** Run `NowPlayingTest`. It should pass. Then compile the tests: `./gradlew :app:compileDebugUnitTestKotlin` should exit 0.
- [ ] **Step 5:** Commit: "Say what is playing: series, genre, author and narrator (#107)".

### Task 2: `ChapterClock`

**Files:** create `player/ChapterClock.kt`; test `ChapterClockTest.kt`.

**Interfaces:**
- `data class ChapterSpan(val index: Int, val title: String, val start: Double, val end: Double)`, with `elapsed(at)`, `left(at)` and `fraction(at): Float`.
- `object ChapterClock`, with:
  - `spans(chapters: List<Chapter>?, total: Double): List<ChapterSpan>`;
  - `at(spans, at): ChapterSpan?`;
  - `previousTarget(spans, at): Double?`;
  - `nextTarget(spans, at): Double?`;
  - `const val RESTART_WITHIN = 3.0`.

- [ ] **Step 1: Failing tests.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where a position sits in its chapter, and where chapter skip goes (#107). */
class ChapterClockTest {
    private val three = listOf(
        Chapter(start = 0.0, end = 1800.0, title = "One"),
        Chapter(start = 1800.0, end = 3600.0, title = "Two"),
        Chapter(start = 3600.0, end = 5400.0, title = "Three")
    )
    private val spans = ChapterClock.spans(three, total = 5400.0)

    @Test fun `a position is in the chapter that contains it`() = assertEquals("Two", ChapterClock.at(spans, 2000.0)!!.title)
    @Test fun `a chapter's start belongs to it`() = assertEquals("Two", ChapterClock.at(spans, 1800.0)!!.title)

    @Test fun `time in and left in the chapter`() {
        val two = ChapterClock.at(spans, 2000.0)!!
        assertEquals(200.0, two.elapsed(2000.0), 0.0)
        assertEquals(1600.0, two.left(2000.0), 0.0)
        assertEquals(0.111f, two.fraction(2000.0), 0.001f)
    }

    // Review Focus 3.
    @Test fun `past the last chapter's end is still the last chapter`() {
        val last = ChapterClock.at(spans, 9999.0)!!
        assertEquals("Three", last.title)
        assertEquals(0.0, last.left(9999.0), 0.0)
        assertNull(ChapterClock.nextTarget(spans, 9999.0))
    }

    @Test fun `previous restarts a chapter that has been playing`() = assertEquals(1800.0, ChapterClock.previousTarget(spans, 1810.0)!!, 0.0)
    @Test fun `previous right after a start goes to the chapter before`() = assertEquals(0.0, ChapterClock.previousTarget(spans, 1802.0)!!, 0.0)
    @Test fun `previous in the first chapter's first seconds stays at its start`() = assertEquals(0.0, ChapterClock.previousTarget(spans, 1.0)!!, 0.0)
    @Test fun `next goes to the next chapter's start`() = assertEquals(3600.0, ChapterClock.nextTarget(spans, 2000.0)!!, 0.0)

    // Review Focus 2.
    @Test fun `chapters without an end run to the next start`() {
        val open = ChapterClock.spans(listOf(Chapter(start = 0.0, title = "A"), Chapter(start = 600.0, title = "B")), total = 1200.0)
        assertEquals(600.0, open[0].end, 0.0)
        assertEquals(1200.0, open[1].end, 0.0)
    }

    @Test fun `chapters out of order are sorted`() {
        val shuffled = ChapterClock.spans(listOf(three[2], three[0], three[1]), total = 5400.0)
        assertEquals(listOf("One", "Two", "Three"), shuffled.map { it.title })
        assertEquals(3600.0, ChapterClock.nextTarget(shuffled, 2000.0)!!, 0.0)
    }

    @Test fun `no chapters is no clock`() {
        assertEquals(emptyList<ChapterSpan>(), ChapterClock.spans(null, 100.0))
        assertNull(ChapterClock.at(emptyList(), 10.0))
    }
}
```

- [ ] **Step 2: Run red.** It should fail to compile on `ChapterClock`.
- [ ] **Step 3: Implement.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter

/** One chapter on the book's timeline, in whole-book seconds. */
data class ChapterSpan(val index: Int, val title: String, val start: Double, val end: Double) {
    fun elapsed(at: Double): Double = (at - start).coerceIn(0.0, end - start)
    fun left(at: Double): Double = (end - at).coerceIn(0.0, end - start)
    fun fraction(at: Double): Float = if (end <= start) 0f else ((at - start) / (end - start)).coerceIn(0.0, 1.0).toFloat()
}

/**
 * Where a position sits in its chapter, and where chapter skip goes (#107).
 *
 * Chapters come as the server stores them: sorted here, and a missing end
 * runs to the next chapter's start, or the book's end for the last one.
 */
object ChapterClock {
    /** Previous within this many seconds of a chapter's start goes to the one before; later, it restarts this one. */
    const val RESTART_WITHIN = 3.0

    fun spans(chapters: List<Chapter>?, total: Double): List<ChapterSpan> {
        val sorted = chapters.orEmpty().filter { it.start != null }.sortedBy { it.start }
        return sorted.mapIndexedNotNull { i, c ->
            val start = c.start!!
            val end = c.end ?: sorted.getOrNull(i + 1)?.start ?: total
            if (end <= start) null else ChapterSpan(i, c.title.orEmpty(), start, end)
        }
    }

    fun at(spans: List<ChapterSpan>, at: Double): ChapterSpan? =
        spans.lastOrNull { it.start <= at } ?: spans.firstOrNull()

    fun previousTarget(spans: List<ChapterSpan>, at: Double): Double? {
        val current = at(spans, at) ?: return null
        if (at - current.start > RESTART_WITHIN) return current.start
        val i = spans.indexOf(current)
        return if (i > 0) spans[i - 1].start else current.start
    }

    fun nextTarget(spans: List<ChapterSpan>, at: Double): Double? {
        val current = at(spans, at) ?: return null
        return spans.getOrNull(spans.indexOf(current) + 1)?.start
    }
}
```

- [ ] **Step 4:** Run. It should pass.
- [ ] **Step 5:** Commit: "Know the chapter a position is in, and where chapter skip goes (#107)".

### Task 3: `PlaybackBar` and `SeekStep`

**Files:** create `player/PlaybackBar.kt`; test `PlaybackBarTest.kt`.

**Interfaces:**
- `object SeekStep { fun seconds(repeatCount: Int): Double }`. It returns 10 below 3 repeats, 30 below 10, and 60 from 10.
- The composable:

```kotlin
@Composable
fun PlaybackBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    ticks: List<Float> = emptyList(),
    thick: Boolean = true,
    onSeekBy: ((Double) -> Unit)? = null
)
```

- `ticks` are fractions from 0 to 1.
- With `onSeekBy`, the bar is focusable: Left/Right call it with −/+ `SeekStep.seconds(repeat)`, and its knob shows a focus ring.

- [ ] **Step 1: Failing tests.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlaybackBarTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `a press seeks ten seconds, a held key further`() {
        assertEquals(10.0, SeekStep.seconds(0), 0.0)
        assertEquals(30.0, SeekStep.seconds(3), 0.0)
        assertEquals(60.0, SeekStep.seconds(10), 0.0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `left and right on a focused bar seek`() {
        val seeks = mutableListOf<Double>()
        compose.setContent { FahrenheitTheme { PlaybackBar(0.5f, Modifier.testTag("bar"), onSeekBy = { seeks += it }) } }
        compose.onNodeWithTag("bar").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("bar").assertIsFocused()

        compose.onNodeWithTag("bar").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("bar").performKeyInput { pressKey(Key.DirectionLeft) }

        assertEquals(listOf(10.0, -10.0), seeks)
    }

    @Test fun `a bar that cannot seek takes no focus`() {
        compose.setContent { FahrenheitTheme { PlaybackBar(0.5f, Modifier.testTag("bar"), thick = false) } }
        compose.onNodeWithTag("bar").assert(androidx.compose.ui.test.SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
    }
}
```

- [ ] **Step 2: Run red.** It should fail to compile on `PlaybackBar` and `SeekStep`.
- [ ] **Step 3: Implement.** In `PlaybackBar.kt`:
  - `SeekStep` as specified.
  - The composable is a `Canvas` in a `Box`, its height 16 dp when thick and 8 dp when thin, the drawn track 7 dp or 3 dp:
    - the track is `onSurface` at 16% alpha (10% when thin);
    - the filled part is `primary` (55% alpha when thin);
    - ticks are 2 dp wide, `onSurface` at 45%;
    - when thick, a knob of radius 8 dp at `fraction`.
  - When `onSeekBy != null`, add `.focusable()`, `.onFocusChanged { focused = it.isFocused }` and `.onKeyEvent`:
    - on `KeyEventType.KeyDown` with `Key.DirectionLeft`/`Key.DirectionRight`, call `onSeekBy(∓/± SeekStep.seconds(it.nativeKeyEvent.repeatCount))` and return true;
    - return false for every other key, so Up and Down still move focus.
  - When focused, draw a ring of 3 dp at `primary` around the knob.
  - Use the TV `MaterialTheme.colorScheme`.
- [ ] **Step 4:** Run. It should pass.
- [ ] **Step 5:** Commit: "A playback bar that seeks with the remote (#107)".

### Task 4: The transport block: two bars, times, five buttons, trailing slot

**Files:**
- Modify `player/MediaPlayerController.kt` and `res/values/strings.xml`.
- Test: `TransportTest.kt` (extend).

**Interfaces:**
- Adds `trailing: @Composable RowScope.() -> Unit = {}` to `MediaPlayerController`. The other parameters are unchanged.
- `const val CHAPTER_BAR_TAG = "player_chapter_bar"` and `const val BOOK_BAR_TAG = "player_book_bar"`.

- [ ] **Step 1: Strings** (in `values/strings.xml`):

```xml
    <string name="time_left_in_chapter">%1$s left in chapter</string>
    <string name="time_of">%1$s of %2$s</string>
    <string name="time_left">%1$s left</string>
    <string name="previous_chapter">Previous chapter</string>
    <string name="next_chapter">Next chapter</string>
```

- [ ] **Step 2: Failing tests.** In `TransportTest`:
  - add a chaptered book: `twoParts` with chapters One 0–1800, Two 1800–3600, Three 3600–5400;
  - add `show(player, timeline, chapters)`, which passes `chapters` through;
  - add a helper `press(desc)` = `onNodeWithContentDescription(desc).performSemanticsAction(SemanticsActions.OnClick)`.

```kotlin
    private val chapters = listOf(
        Chapter(start = 0.0, end = 1800.0, title = "One"),
        Chapter(start = 1800.0, end = 3600.0, title = "Two"),
        Chapter(start = 3600.0, end = 5400.0, title = "Three")
    )

    @Test fun `with chapters, the first bar is the chapter and the second the book`() {
        show(queuedAt(3900.0), chapters = chapters)
        compose.onNodeWithText("5 min 0 s").assertIsDisplayed()
        compose.onNodeWithText("25 min 0 s left in chapter").assertIsDisplayed()
        compose.onNodeWithText("1 h 5 min of 1 h 30 min").assertIsDisplayed()
        compose.onNodeWithText("25 min 0 s left").assertIsDisplayed()
    }

    @Test fun `next chapter goes to the next chapter's start`() {
        show(queuedAt(1900.0), chapters = chapters)
        press(compose.activity.getString(R.string.next_chapter)); compose.waitForIdle()
        assertEquals(1, player.currentMediaItemIndex); assertEquals(0L, player.currentPosition)
    }

    @Test fun `previous chapter restarts the chapter that is playing`() {
        show(queuedAt(1810.0), chapters = chapters)
        press(compose.activity.getString(R.string.previous_chapter)); compose.waitForIdle()
        assertEquals(1_800_000L, player.currentPosition)
    }

    @Test fun `previous chapter right after a start goes to the one before`() {
        show(queuedAt(1801.0), chapters = chapters)
        press(compose.activity.getString(R.string.previous_chapter)); compose.waitForIdle()
        assertEquals(0L, player.currentPosition)
    }

    // Review Focus 1.
    @Test fun `without chapters there is one bar and no chapter buttons`() {
        show(queuedAt(0.0))
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_chapter)).assertDoesNotExist()
        compose.onNodeWithTag(BOOK_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithTag(CHAPTER_BAR_TAG).assertExists()
    }

    // Review Focus 4.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `up from play reaches the chapter bar, which seeks`() {
        show(queuedAt(1900.0), chapters = chapters)
        compose.onNodeWithContentDescription("Play").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag(CHAPTER_BAR_TAG).assertIsFocused()
        compose.onNodeWithTag(CHAPTER_BAR_TAG).performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitForIdle()
        assertEquals(1_910_000L, player.currentPosition)
    }

    @Test fun `actions sit beside the transport`() {
        player = queuedAt(0.0)
        compose.setContent { FahrenheitTheme { MediaPlayerController(player, BookPlayback(player, twoParts), twoParts.totalDuration, trailing = { Text("ACTIONS") }) } }
        compose.onNodeWithText("ACTIONS").assertIsDisplayed()
    }
```

  - The existing `the times are whole-book times, with what is left` stays as it is: no chapters means one bar and the #93 times.
- [ ] **Step 3: Run red.** It should fail to compile on `trailing`, `CHAPTER_BAR_TAG` and `next_chapter`.
- [ ] **Step 4: Implement.** In `MediaPlayerController`:
  - Compute `val spans = remember(chapters, totalTime) { ChapterClock.spans(chapters, totalTime) }` and `val chapter = ChapterClock.at(spans, currentTime)`.
  - Lay out a `Column(Modifier.fillMaxWidth())` containing:
    - **With a chapter:**
      - `PlaybackBar(chapter.fraction(currentTime), Modifier.fillMaxWidth().testTag(CHAPTER_BAR_TAG), onSeekBy = { seekTo(PlaybackPosition.skip(currentTime, it, totalTime)) })`;
      - a times row: `spoken(chapter.elapsed(currentTime))` against `time_left_in_chapter(spoken(chapter.left(currentTime)))`;
      - `PlaybackBar(fraction(currentTime, totalTime), Modifier.fillMaxWidth().testTag(BOOK_BAR_TAG), ticks = PlaybackPosition.chapterMarks(chapters, totalTime).map { it / 100f }, thick = false)`;
      - a small times row: `time_of(spoken(currentTime), spoken(totalTime))` against `time_left(spoken(left(currentTime, totalTime)))`.
    - **Without one:** one `PlaybackBar` (tag `CHAPTER_BAR_TAG`, seeking), and the #93 times row.
    - **On error:** "Couldn't play this" replaces the first times row, as today.
    - **The transport row:** `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(14.dp))`, containing:
      - when `chapter != null`, a previous-chapter button, `Icons.Filled.SkipPrevious`, described `previous_chapter`, that seeks to `ChapterClock.previousTarget(spans, currentTime)`;
      - back, play/pause and forward, unchanged;
      - when `chapter != null`, a next-chapter button, `Icons.Filled.SkipNext`, described `next_chapter`, that seeks to `nextTarget`, or does nothing when null;
      - `Spacer(Modifier.weight(1f))`, then `trailing()`.
  - Remove the `Slider`, the `Canvas` chapter marks and `drawLineAtPercentage`, and their imports. Confirm with the compiler and lint.
- [ ] **Step 5:** Run `TransportTest`, `TransportFocusTest` and `PlaybackBarTest`. All should pass.
- [ ] **Step 6:** Commit: "The transport: chapter and book bars, chapter skip, room for actions (#107)".

### Task 5: Layout C, and Go to podcast beside the transport

**Files:**
- Modify `player/PlayerScreen.kt` and `player/PlayerActivity.kt`.
- Test: `PlayerScreenTest.kt` (rewrite).

**Interfaces:**
- `PlayerScreen(nowPlaying: NowPlaying, currentTime: Double, transport: @Composable () -> Unit)`. The `onGoToPodcast` parameter is removed.
- `@Composable fun GoToPodcastButton(onClick: () -> Unit)`, tagged `GO_TO_PODCAST_TAG`.

- [ ] **Step 1: Failing tests.** In `PlayerScreenTest`:
  - build a chaptered book: `NowPlaying(..., chapters = listOf(Chapter(0.0, 600.0, "Chapter 1"), Chapter(600.0, 1800.0, "Chapter 2")), kicker = "The Long Way · Book 2", byline = "An Author · read by A Reader")`;
  - build an episode with `kicker = "Welcome to Night Vale · Yesterday"`.

```kotlin
    @Test fun `a book shows its series, title, byline and the chapter playing`() {
        render(book, currentTime = 700.0)
        compose.onNodeWithText("THE LONG WAY · BOOK 2").assertIsDisplayed()
        compose.onNodeWithText("An Author · read by A Reader").assertIsDisplayed()
        compose.onNodeWithText("Chapter 2").assertIsDisplayed()
        compose.onNodeWithText("TRANSPORT").assertIsDisplayed()
    }

    @Test fun `the chapter follows the position`() {
        render(book, currentTime = 100.0)
        compose.onNodeWithText("Chapter 1").assertIsDisplayed()
    }

    @Test fun `an episode shows its show and date above its title`() {
        render(episode, currentTime = 0.0)
        compose.onNodeWithText("WELCOME TO NIGHT VALE · YESTERDAY").assertIsDisplayed()
        compose.onNodeWithText("295 - The Book of Dale").assertIsDisplayed()
    }

    @Test fun `the description is not on the player screen`() {
        render(book.copy(description = "<p>A long description</p>"), currentTime = 0.0)
        compose.onNodeWithText("A long description", substring = true).assertDoesNotExist()
    }

    // Review Focus 5.
    @OptIn(ExperimentalTestApi::class)
    @Test fun `an episode offers its podcast beside the transport`() {
        var went = 0
        compose.setContent { FahrenheitTheme { GoToPodcastButton { went++ } } }
        compose.onNodeWithTag(GO_TO_PODCAST_TAG).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(GO_TO_PODCAST_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(1, went)
    }
```

  - The kicker is drawn in capitals, as the mock draws it, so the text matcher checks the upper-cased string.
  - Before using `substring`, check whether `onNodeWithText` accepts it in this Compose version.
- [ ] **Step 2: Run red.** It should fail to compile on the `PlayerScreen` signature and `GoToPodcastButton`.
- [ ] **Step 3: Implement.**
  - `PlayerScreen`:

```kotlin
@Composable
fun PlayerScreen(nowPlaying: NowPlaying, currentTime: Double, transport: @Composable () -> Unit) {
    var titleFocused by remember { mutableStateOf(false) }
    val chapter = remember(nowPlaying.chapters, nowPlaying.trackTotal) { ChapterClock.spans(nowPlaying.chapters, nowPlaying.trackTotal ?: 0.0) }
        .let { ChapterClock.at(it, currentTime)?.title?.takeIf { t -> t.isNotBlank() } }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 60.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            CoverImage(itemId = nowPlaying.itemId, contentDescription = nowPlaying.title, size = 250.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nowPlaying.kicker?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MarqueeText(
                    text = nowPlaying.title, isFocused = titleFocused,
                    style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, modifier = Modifier.onFocusChanged { titleFocused = it.isFocused }
                )
                nowPlaying.byline?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                chapter?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        transport()
    }
}
```

  - `GoToPodcastButton` is today's `Button` from the screen, moved out with its icon and `GO_TO_PODCAST_TAG`.
  - Update the KDoc to layout C, frame "C…" in `docs/mocks/player.html`. Note that the description moves to About in step 3.
  - `PlayerActivity`: change the call to `PlayerScreen(nowPlaying = playing, currentTime = currentTime, transport = { … })`, and in the `MediaPlayerController` call add:

```kotlin
                            trailing = {
                                if (playing.goToPodcast) GoToPodcastButton { goToPodcast(itemId) }
                            }
```

  - Remove the `RichText` and `verticalScroll` imports if unused. Confirm with the compiler and lint.
- [ ] **Step 4:** Run `PlayerScreenTest`, `TransportTest` and `PlayerActivityTest`. All should pass.
- [ ] **Step 5: Gate.** Run `set -o pipefail; ./gradlew :app:testDebugUnitTest --rerun :app:lintDebug > log 2>&1; echo EXIT_CODE=$?`. It should report `EXIT_CODE=0` and 0 failures.
- [ ] **Step 6:** Commit: "Lay the player out as frame C (#107)".

### Task 6: Device check

- [ ] Install on the stick, after checking it is not in use.
- [ ] With a chaptered book, a book without chapters, and an episode:
  - the layout matches frame C;
  - focus is on play;
  - Up reaches the chapter bar, and Left/Right seek in 10 s steps, further when held;
  - Down returns to the transport;
  - previous and next chapter work;
  - an episode shows Go to podcast on the right;
  - the chapter name and times follow playback.
- [ ] Read positions from the media session. Leave no test progress behind: reset any it creates, as before.
