# Player step 2: the cover-coloured background - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Behind the player, a wash taken from the cover's colours, as frame C draws it: a soft glow of the cover's colour behind it, fading into the dark background. It is plain black when the cover has not loaded or has no usable colour (#107; step 7 of the spec, brought forward at the maintainer's request).

**Architecture:**
- **`CoverWash`** (pure): picks a colour from the cover's palette swatches and darkens it until the screen's light text stays readable on it.
- **`rememberCoverWash(itemId, load)`**: loads the cover as a software bitmap through Coil, with the same URL and auth header as `CoverImage`, runs `androidx.palette` off the main thread, and returns the wash colour or null. The loader is a parameter, so tests pass a bitmap.
- **`PlayerScreen`** takes `wash: Color?` and draws the glow and fade behind its content.

**Tech Stack:** Kotlin, Compose TV, Coil 2.7, `androidx.palette:palette-ktx` 1.0.0 (new).

**Spec:** `docs/superpowers/specs/2026-10-03-player-revamp-design.md` (§Layout: "Background"); mocks `docs/mocks/player.html` (the `.wash` in frame C).

## Global Constraints

- Branch `feat/player-cover-wash`, stacked on `feat/player-revamp` (#110).
- Test-first; Gradle exit codes captured; commits via `git commit -F`, with no trailer; imports pruned by the compiler or lint.
- **Contrast:** the screen's text colours (`onSurface`, `onSurfaceVariant`, `primary`) must keep at least 4.5:1 against the wash. The wash only darkens a colour; it never lightens one.
- No cover, or no swatch: the background stays `MaterialTheme.colorScheme.background`, as today.

## Review Focus

1. **A very light or white cover** (common: printed covers on white). The wash must still be dark, keeping contrast at or above 4.5:1. Pinned in Task 1 (`a white cover still gives a dark wash`).
2. **A grey or black cover:** no colour to show, so no wash rather than a muddy grey. Pinned in Task 1 (`a colourless cover gives no wash`).
3. **The cover fails to load** (offline, 401): the screen draws as today, with no crash. Pinned in Task 2 (`no cover is no wash`).
4. **The player is opened and closed quickly:** loading is cancelled with the screen. It runs in `LaunchedEffect`.

---

### Task 1: `CoverWash`, the pure choice

**Files:** create `player/CoverWash.kt`; test `CoverWashTest.kt` (JVM; uses Compose `Color` maths only).

**Interfaces:**
- `object CoverWash` with:
  - `fun pick(candidates: List<Int?>): Color?`: the first ARGB candidate with enough colour;
  - `fun tone(argb: Int): Color`, which darkens until the contrast holds;
  - `const val MIN_CONTRAST = 4.5`.

- [ ] **Step 1: Failing tests.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The colour behind the player, taken from its cover (#107). */
class CoverWashTest {
    private val onSurface = Color(0xFFE6E1E5)
    private val onSurfaceVariant = Color(0xFFCAC4D0)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance() + 0.05, b.luminance() + 0.05).sortedDescending()
        return (hi / lo).toDouble()
    }

    @Test fun `the first colourful candidate wins`() {
        val green = Color(0xFF5B6B3A).toArgb()
        val wash = CoverWash.pick(listOf(null, Color(0xFF777777).toArgb(), green))!!
        assertTrue("greenish: $wash", wash.green > wash.red && wash.green > wash.blue)
    }

    // Review Focus 2.
    @Test fun `a colourless cover gives no wash`() =
        assertNull(CoverWash.pick(listOf(Color.Black.toArgb(), Color(0xFF808080).toArgb(), Color.White.toArgb())))

    @Test fun `no candidates gives no wash`() = assertNull(CoverWash.pick(listOf(null, null)))

    // Review Focus 1.
    @Test fun `a white cover still gives a dark wash`() {
        val wash = CoverWash.tone(Color(0xFFFFE8E8).toArgb())
        assertTrue(contrast(onSurface, wash) >= CoverWash.MIN_CONTRAST)
        assertTrue(contrast(onSurfaceVariant, wash) >= CoverWash.MIN_CONTRAST)
    }

    @Test fun `a dark colour is kept as it is`() {
        val dark = Color(0xFF24301A)
        assertEquals(dark.toArgb(), CoverWash.tone(dark.toArgb()).toArgb())
    }

    @Test fun `toning keeps the hue`() {
        val wash = CoverWash.tone(Color(0xFF3060E0).toArgb())
        assertTrue("still blue: $wash", wash.blue > wash.red && wash.blue > wash.green)
    }
}
```

- [ ] **Step 2: Run red.** It should fail to compile on `CoverWash`.
- [ ] **Step 3: Implement.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * The colour behind the player, taken from its cover (#107; the wash in
 * frame C of docs/mocks/player.html).
 *
 * Only a colour with some colour in it is used: a grey, black or white cover
 * gives no wash rather than a muddy one. The colour is darkened - never
 * lightened - until the screen's light text keeps [MIN_CONTRAST] against it.
 */
object CoverWash {
    const val MIN_CONTRAST = 4.5

    /** The lightest text drawn over the wash: onSurface. */
    private val TEXT = Color(0xFFE6E1E5)
    private const val MIN_SATURATION = 0.18f

    fun pick(candidates: List<Int?>): Color? =
        candidates.filterNotNull().firstOrNull { saturation(it) >= MIN_SATURATION }?.let(::tone)

    fun tone(argb: Int): Color {
        var colour = Color(argb).copy(alpha = 1f)
        repeat(40) {
            if (contrast(TEXT, colour) >= MIN_CONTRAST + 0.2) return colour
            colour = Color(colour.red * 0.9f, colour.green * 0.9f, colour.blue * 0.9f)
        }
        return colour
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (max(la, lb) / min(la, lb)).toDouble()
    }

    private fun saturation(argb: Int): Float {
        val c = Color(argb)
        val hi = maxOf(c.red, c.green, c.blue)
        val lo = minOf(c.red, c.green, c.blue)
        return if (hi == 0f) 0f else (hi - lo) / hi
    }
}
```

`tone` keeps a margin of 0.2 above `MIN_CONTRAST`, because `onSurfaceVariant` is darker than `onSurface`. The test checks both colours; if `onSurfaceVariant` falls short, raise the margin, never lower the bar.

- [ ] **Step 4:** Run. It should pass.
- [ ] **Step 5:** Commit: "Choose the wash colour from a cover, dark enough to read on (#107)".

### Task 2: `rememberCoverWash`, loading the cover's palette

**Files:**
- Modify `gradle/libs.versions.toml` and `app/build.gradle.kts`, adding `androidx.palette:palette-ktx:1.0.0`.
- Create `player/CoverWashLoader.kt`.
- Test: `CoverWashLoaderTest.kt` (Robolectric).

**Interfaces:**
- `suspend fun coverWashOf(bitmap: Bitmap?): Color?`: runs Palette on `Dispatchers.Default`. Candidates, in order: `darkVibrant`, `vibrant`, `darkMuted`, `muted`, `dominant`.
- `@Composable fun rememberCoverWash(itemId: String, load: suspend (String) -> Bitmap? = { coverBitmap(context, it) }): Color?`
- `suspend fun coverBitmap(context: Context, itemId: String): Bitmap?`: a Coil `ImageRequest` with the same URL and auth header as `CoverImage`, `allowHardware(false)`, and `size(128)` (a small bitmap is enough for a palette); returns null on any failure.

- [ ] **Step 1: Dependency.** Add `palette = "1.0.0"` under `[versions]`, `androidx-palette = { module = "androidx.palette:palette-ktx", version.ref = "palette" }` under `[libraries]`, and `implementation(libs.androidx.palette)` in the app.

- [ ] **Step 2: Failing tests.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverWashLoaderTest {
    private fun solid(argb: Int) = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(argb) }

    @Test fun `a green cover gives a green wash`() = runBlocking {
        val wash = coverWashOf(solid(0xFF5B8B3A.toInt()))!!
        assertTrue("greenish: $wash", wash.green > wash.red && wash.green > wash.blue)
    }

    // Review Focus 3.
    @Test fun `no cover is no wash`() = runBlocking { assertNull(coverWashOf(null)) }

    @Test fun `a grey cover is no wash`() = runBlocking { assertNull(coverWashOf(solid(0xFF808080.toInt()))) }
}
```

- [ ] **Step 3: Run red.** It should fail to compile on `coverWashOf`.
- [ ] **Step 4: Implement** `CoverWashLoader.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.paulohenriquesg.fahrenheit.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The wash for a cover bitmap, or null when it has no usable colour. */
suspend fun coverWashOf(bitmap: Bitmap?): Color? {
    bitmap ?: return null
    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
    return CoverWash.pick(
        listOf(palette.darkVibrantSwatch, palette.vibrantSwatch, palette.darkMutedSwatch, palette.mutedSwatch, palette.dominantSwatch)
            .map { it?.rgb }
    )
}

/** The item's cover, small and in software memory so its pixels can be read; null on any failure. */
suspend fun coverBitmap(context: Context, itemId: String): Bitmap? = runCatching {
    val url = ApiClient.generateFullUrl("/api/items/$itemId/cover") ?: return null
    val request = ImageRequest.Builder(context)
        .data(url)
        .addHeader("Authorization", "Bearer ${ApiClient.getToken() ?: ""}")
        .allowHardware(false)
        .size(128)
        .build()
    ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
}.getOrNull()

/** The wash for an item's cover; null until it is known, or when there is none. */
@Composable
fun rememberCoverWash(itemId: String, load: (suspend (String) -> Bitmap?)? = null): Color? {
    val context = LocalContext.current
    var wash by remember(itemId) { mutableStateOf<Color?>(null) }
    LaunchedEffect(itemId) {
        wash = coverWashOf(load?.invoke(itemId) ?: coverBitmap(context, itemId))
    }
    return wash
}
```

  A `CancellationException` from leaving the screen must not be swallowed. `runCatching` in `coverBitmap` would swallow it. If the compiler or a test shows that matters, catch `Exception` and rethrow `CancellationException`.
- [ ] **Step 5:** Run. It should pass.
- [ ] **Step 6:** Commit: "Read a cover's colours for the player's wash (#107)".

### Task 3: The wash behind the player

**Files:**
- Modify `player/PlayerScreen.kt` and `player/PlayerActivity.kt`.
- Test: `PlayerScreenTest.kt`.

**Interfaces:** `PlayerScreen(nowPlaying, currentTime, wash: Color? = null, transport)`.

- [ ] **Step 1: Failing test** (smoke; the colour itself is Task 1's):

```kotlin
    @Test fun `with a wash the screen still shows everything`() {
        compose.setContent { FahrenheitTheme { PlayerScreen(nowPlaying = book, currentTime = 700.0, wash = Color(0xFF24301A), transport = { Text("TRANSPORT") }) } }
        compose.onNodeWithText("Chapter 2").assertIsDisplayed()
        compose.onNodeWithText("TRANSPORT").assertIsDisplayed()
    }
```

- [ ] **Step 2: Run red.** It should fail to compile on `wash`.
- [ ] **Step 3: Implement.** In `PlayerScreen`, wrap the content in a `Box(Modifier.fillMaxSize())`. Its first child, only when `wash != null`, is a `Box(Modifier.fillMaxSize().background(...))` with two brushes, as in the mock:
  - `Brush.radialGradient(listOf(wash, Color.Transparent), center = Offset(w * 0.18f, h * 0.30f), radius = max(w, h) * 0.75f)`, using `drawWithCache` or `BoxWithConstraints` for the size;
  - under it, a `Brush.verticalGradient(listOf(wash.copy(alpha = 0.55f), background), endY = h * 0.75f)`.
  The existing `Column` comes second.

  In `PlayerActivity`, pass `wash = rememberCoverWash(playing.itemId)`.
- [ ] **Step 4:** Run `PlayerScreenTest`, `CoverWashTest` and `CoverWashLoaderTest`, then the full gate: `EXIT_CODE=0`, lint clean.
- [ ] **Step 5:** Commit: "Wash the player in its cover's colours (#107)".

### Task 4: Device check

- [ ] On the stick, after checking it is free:
  - a colourful cover (the green wash shows behind the cover and fades to black, and the text reads clearly);
  - a white or pale cover (still dark);
  - a black-and-white cover, such as the episode's chess piece (no wash, plain black).
- [ ] Take screenshots. Nothing is played, so no progress is written.
