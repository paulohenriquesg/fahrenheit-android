# Media3 playback in a service - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Play every file of a multi-file book back to back with ExoPlayer, in whole-book time, from a Media3 `MediaSessionService` that outlives the player screen (#16).

**Architecture:** `PlaybackService` owns an ExoPlayer wrapped in a `LeavingGuard` and exposed through a `MediaSession`; `PlayerActivity` drives it through a `MediaController`. Each queued `MediaItem` carries its book facts (`itemId`, `episodeId`, `startOffset`, `bookTotal`) in `RequestMetadata.extras`, so the service reports whole-book progress without knowing about books. Audio is fetched through the app's authenticated OkHttp client.

**Tech Stack:** Kotlin, Jetpack Compose (TV Material), Media3 1.11.1 (`exoplayer`, `session`, `datasource-okhttp`), OkHttp/Retrofit, JUnit4 + Robolectric 4.17, `media3-test-utils(-robolectric)`, MockWebServer.

**Spec:** `docs/superpowers/specs/2026-10-02-media3-playback-service-design.md`

## Global Constraints

- Media3 version: `1.11.1` (the existing `media3` entry in `gradle/libs.versions.toml`); every new Media3 artifact uses `version.ref = "media3"`.
- compileSdk 37, minSdk 25, targetSdk 34 - unchanged.
- Test-first: every test is written and run to a failure *for the stated reason* before the code it covers.
- Run Gradle with exit codes captured, never piped into `head`/`tail`:
  `set -o pipefail; ./gradlew <task> 2>&1 | tee /tmp/run.log; echo "EXIT_CODE=$?" >> /tmp/run.log`
  The machine's shell is zsh: use `pipefail`, never `${PIPESTATUS[0]}`.
- Never prune Kotlin imports by text search; use lint/the compiler (`getValue`/`setValue` for `by` delegates look unused).
- Commit messages carry **no** bot-attribution trailer (no `Co-Authored-By`, no "Generated with"). Write the message to a temp file and `git commit -F <file>`; a hook rejects `-F -`.
- The repo is public: no server hosts, credentials, library titles or listening history in code, tests or commits. Test fixtures use `abs.test` / `abs.invalid` and invented titles.
- Unchanged by this plan: the episode `/play` session (`openSession`), transport layout (frame 4 is separate), `media2-session` dependency.
- Copy: the playback error line reads exactly `Couldn't play this`.

## Review Focus

1. **Back on a player opened but never played** - the listener opened a book, did not press play, pressed Back. Expected: no progress write at all (a write would bump the book in Continue Listening and could overwrite a resume point). Pinned in Task 3 (`stopping before anything played sends nothing`) and Task 6 (`stopping something never played sends nothing`).
2. **Book A replaced by book B while A plays** - expected: A's final position goes to A's ids, never to B's, and B is not reported until it plays. Pinned in Task 6 (`replacing one book with another reports the first to the first`).
3. **Reopening the player for the book already playing in the background** - expected: playback continues where it is; the saved (older) server position and `auto_play` are ignored. Pinned in Task 9 (`the book already playing is left where it is`).
4. **A long book outliving its access token, or a sign-in as someone else between files** - expected: the next file is fetched with the current token of the current session. Pinned in Task 4 (`an expired token is refreshed for audio too`, `each file uses the client current when it is opened`).
5. **A playback error mid-book, then Play** - expected: the error line shows, and Play retries at the same whole-book position, not from 0:00. Pinned in Task 8 (`after an error, play retries where it was`).

---

## File structure

Create (all under `app/src/main/java/com/paulohenriquesg/fahrenheit/player/` unless noted):

| file | responsibility |
|---|---|
| `QueuedFile.kt` | The book facts carried by one queued `MediaItem`; whole-book time; "is this the item I want". |
| `PlaybackQueue.kt` | `NowPlaying` + start position -> `MediaItem`s, start index, start offset. |
| `AudioHttp.kt` | `DataSource.Factory` over the app's authenticated OkHttp client, resolved per file. |
| `LeavingGuard.kt` | `ForwardingPlayer` announcing stop/replace/clear before they happen. |
| `PlaybackReporting.kt` | Runs `ProgressReporter` for whatever the player is playing; final report on stop/leave. |
| `PlayableItems.kt` | Rebuilds playable items from `RequestMetadata.mediaUri` in the session callback. |
| `PlaybackService.kt` | The `MediaSessionService`: ExoPlayer, session, reporting, progress sending. |
| `Playback.kt` | App-side entry: `connect`, `leave`, `stop`. |
| `PlayerStart.kt` | Activity logic: reattach or load the queue. |

Test files mirror them under `app/src/test/java/com/paulohenriquesg/fahrenheit/player/`.

Modify: `NowPlaying.kt`, `BookPlayback.kt`, `ProgressReporter.kt`, `MediaPlayerController.kt`, `PlayerActivity.kt`, `main/MainScreen.kt`, `api/ApiClient.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `res/values/strings.xml`, and the tests `NowPlayingTest`, `PlayerScreenTest`, `BookPlaybackTest`, `ProgressReporterTest`, `TransportFocusTest`, `MediaPlayerAuthenticationTest`.

Delete: `GlobalMediaPlayer.kt`, `GlobalMediaPlayerTest.kt`.

Test command shape (one class): `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.QueuedFileTest'`

---

### Task 1: `NowPlaying` carries the whole timeline

Today `NowPlaying.contentUrl` is the first track's URL only - the root of #16. Replace it with the book's `TrackTimeline` (an episode gets a one-track timeline), and derive `trackTotal` from it.

**Files:**
- Modify: `app/src/main/java/com/paulohenriquesg/fahrenheit/player/NowPlaying.kt`
- Modify: `app/src/main/java/com/paulohenriquesg/fahrenheit/player/PlayerActivity.kt` (one line, to stay compiling)
- Test: `app/src/test/java/com/paulohenriquesg/fahrenheit/player/NowPlayingTest.kt`, `PlayerScreenTest.kt`

**Interfaces:**
- Consumes: `TrackTimeline`, `TimelineTrack`, `timelineOf(tracks)` (existing, `TrackTimeline.kt`).
- Produces: `NowPlaying(itemId: String, title: String, timeline: TrackTimeline?, mediaDuration: Double?, chapters: List<Chapter>?, episodeId: String?, goToPodcast: Boolean, description: String?, line: (Double) -> String)` with `val trackTotal: Double?` (= `timeline?.totalDuration`). `contentUrl` no longer exists.

- [ ] **Step 1: Write the failing tests**

In `NowPlayingTest.kt`, add a two-file book fixture after `book`, and replace the two `contentUrl` assertions.

```kotlin
    private val bookInTwoFiles: LibraryItemResponse = Gson().fromJson(
        """{"id":"b2","mediaType":"book","media":{"duration":5400.0,
            "metadata":{"title":"A Book in Parts","authorName":"An Author","explicit":false},
            "tracks":[
              {"index":1,"startOffset":0.0,"duration":3600.0,"title":"t1","contentUrl":"/api/items/b2/file/1","mimeType":"audio/mpeg",
               "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}},
              {"index":2,"startOffset":3600.0,"duration":1800.0,"title":"t2","contentUrl":"/api/items/b2/file/2","mimeType":"audio/mpeg",
               "metadata":{"filename":"b","ext":"mp3","path":"/b","relPath":"b","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}]}}""",
        LibraryItemResponse::class.java
    )

    @Test
    fun `a book in several files plays all of them, not just the first`() {
        val playing = NowPlaying.of(bookInTwoFiles, episodeId = null, now = now)!!

        val timeline = playing.timeline!!
        assertEquals(2, timeline.size)
        assertEquals("/api/items/b2/file/2", timeline.track(1).contentUrl)
        assertEquals(5400.0, playing.trackTotal!!, 0.0)
    }
```

In `a book plays its tracks, with chapter marks, and trusts the tracks' length`, replace
`assertEquals("/api/items/b1/file/1", playing.contentUrl)` with
`assertEquals("/api/items/b1/file/1", playing.timeline!!.track(0).contentUrl)`.

In `an episode plays its one file, without chapter marks, and offers its podcast`, replace
`assertEquals("/api/items/p1/file/9", playing.contentUrl)` with:

```kotlin
        val timeline = playing.timeline!!
        assertEquals(1, timeline.size)
        assertEquals("/api/items/p1/file/9", timeline.track(0).contentUrl)
        assertEquals(0.0, timeline.track(0).startOffset, 0.0)
```

In `PlayerScreenTest.kt`, `playing(episode)` becomes:

```kotlin
    private fun playing(episode: Boolean) = NowPlaying(
        itemId = "p1",
        title = if (episode) "295 - The Book of Dale" else "Project Hail Mary",
        timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x"))),
        mediaDuration = null,
        chapters = null,
        episodeId = if (episode) "e295" else null,
        goToPodcast = episode,
        description = null,
        line = { if (episode) "Welcome to Night Vale · Yesterday" else "Chapter 2 · Andy Weir" }
    )
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.NowPlayingTest' --tests 'com.paulohenriquesg.fahrenheit.player.PlayerScreenTest'`
Expected: compilation FAILS with `Unresolved reference 'timeline'` (and `No parameter with name 'timeline'`).

- [ ] **Step 3: Implement**

In `NowPlaying.kt`: replace the `contentUrl` and `trackTotal` constructor properties with `val timeline: TrackTimeline?`, add the derived property, and update the KDoc `@property trackTotal` to `@property timeline every file that will play, in order; null when there is nothing to play.` plus a line `trackTotal` is its length.

```kotlin
data class NowPlaying(
    val itemId: String,
    val title: String,
    val timeline: TrackTimeline?,
    val mediaDuration: Double?,
    val chapters: List<Chapter>?,
    val episodeId: String?,
    val goToPodcast: Boolean,
    val description: String?,
    private val line: (Double) -> String
) {
    /** The length of what will actually play; trusted over [mediaDuration] (see [ResumePoint]). */
    val trackTotal: Double? get() = timeline?.totalDuration
```

Book branch: `timeline = timelineOf(tracks),` (remove `contentUrl` and `trackTotal` lines).
Episode branch:

```kotlin
                timeline = episode.audioTrack?.let {
                    // An episode is one file, starting at the start.
                    TrackTimeline(listOf(TimelineTrack(index = it.index, startOffset = 0.0, duration = it.duration, contentUrl = it.contentUrl)))
                },
```

In `PlayerActivity.kt` line ~159, replace `playing.contentUrl?.let { ApiClient.generateFullUrl(it) }` with `playing.timeline?.track(0)?.contentUrl?.let { ApiClient.generateFullUrl(it) }` (temporary; Task 9 rewrites this).

- [ ] **Step 4: Run to verify pass**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/paulohenriquesg/fahrenheit/player/NowPlaying.kt app/src/main/java/com/paulohenriquesg/fahrenheit/player/PlayerActivity.kt app/src/test/java/com/paulohenriquesg/fahrenheit/player/NowPlayingTest.kt app/src/test/java/com/paulohenriquesg/fahrenheit/player/PlayerScreenTest.kt
git commit -F /tmp/msg   # "NowPlaying carries every file of a book (#16)" + body: contentUrl was the first file only.
```

---

### Task 2: `QueuedFile` and `PlaybackQueue`

**Files:**
- Create: `player/QueuedFile.kt`, `player/PlaybackQueue.kt`
- Modify: `player/BookPlayback.kt` (remove `load` and `resolveUrl`)
- Test: create `QueuedFileTest.kt`, `PlaybackQueueTest.kt`; modify `BookPlaybackTest.kt`

**Interfaces:**
- Consumes: `NowPlaying` (Task 1), `TrackTimeline.locate`.
- Produces:
  - `data class QueuedFile(val itemId: String, val episodeId: String?, val startOffset: Double, val bookTotal: Double)` with `fun bookTime(positionInFile: Double): Double`, `fun isFor(itemId: String, episodeId: String?): Boolean`, `fun toBundle(): Bundle`, `companion fun of(item: MediaItem?): QueuedFile?`
  - `data class QueueStart(val items: List<MediaItem>, val index: Int, val positionMs: Long)`
  - `object PlaybackQueue { fun of(nowPlaying: NowPlaying, startAt: Double, resolveUrl: (String) -> String?): QueueStart? }`
  - `class BookPlayback(player: Player, timeline: TrackTimeline)` with `seekToBookTime(Double)`, `bookPosition(): Double` (unchanged).

- [ ] **Step 1: Write the failing tests**

`QueuedFileTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a queued file knows about the book it belongs to. It rides inside the
 * MediaItem, because that is all that crosses from the screen to the service.
 */
@RunWith(AndroidJUnit4::class)
class QueuedFileTest {

    private val partTwo = QueuedFile(itemId = "b1", episodeId = null, startOffset = 3600.0, bookTotal = 5400.0)

    @Test
    fun `a position in a later file is that far past the file's start in the book`() =
        assertEquals(3900.0, partTwo.bookTime(300.0), 0.0)

    @Test
    fun `the facts survive the trip through a MediaItem`() {
        val item = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(partTwo.toBundle()).build())
            .build()

        assertEquals(partTwo, QueuedFile.of(item))
    }

    @Test
    fun `an episode's id survives too`() {
        val episode = QueuedFile(itemId = "p1", episodeId = "e1", startOffset = 0.0, bookTotal = 1800.0)
        val item = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(episode.toBundle()).build())
            .build()

        assertEquals(episode, QueuedFile.of(item))
    }

    @Test
    fun `an item that is not one of ours has no facts`() {
        assertNull(QueuedFile.of(MediaItem.fromUri("https://abs.test/x")))
        assertNull(QueuedFile.of(null))
    }

    @Test
    fun `the same book is the same book`() = assertTrue(partTwo.isFor("b1", null))

    @Test
    fun `another episode of the same podcast is not the one playing`() =
        assertFalse(QueuedFile("p1", "e1", 0.0, 1800.0).isFor("p1", "e2"))

    @Test
    fun `a book is not an episode that shares its item id`() =
        assertFalse(partTwo.isFor("b1", "e1"))
}
```

`PlaybackQueueTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** A book or episode turned into what a Media3 player queues (#16). */
@RunWith(AndroidJUnit4::class)
class PlaybackQueueTest {

    private fun nowPlaying(timeline: TrackTimeline?, episodeId: String? = null) = NowPlaying(
        itemId = "b1", title = "A Book in Parts", timeline = timeline, mediaDuration = null,
        chapters = null, episodeId = episodeId, goToPodcast = episodeId != null, description = null, line = { "" }
    )

    private val threeParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2"),
            TimelineTrack(index = 3, startOffset = 5400.0, duration = 600.0, contentUrl = "/part3")
        )
    )

    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @Test
    fun `every file is queued, in order, with its full URL`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0, resolve)!!

        assertEquals(
            listOf("https://abs.test/part1", "https://abs.test/part2", "https://abs.test/part3"),
            queue.items.map { it.requestMetadata.mediaUri.toString() }
        )
        // Also playable as is, by a player in the same process.
        assertEquals("https://abs.test/part2", queue.items[1].localConfiguration?.uri.toString())
    }

    @Test
    fun `a start in a later file starts in that file, that far in`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 4500.0, resolve)!!

        assertEquals(1, queue.index)
        assertEquals(900_000L, queue.positionMs)
    }

    @Test
    fun `each file knows where it sits in the book, and the book's length`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0, resolve)!!

        assertEquals(
            listOf(QueuedFile("b1", null, 0.0, 6000.0), QueuedFile("b1", null, 3600.0, 6000.0), QueuedFile("b1", null, 5400.0, 6000.0)),
            queue.items.map { QueuedFile.of(it) }
        )
    }

    @Test
    fun `the title travels with every file`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0, resolve)!!

        assertEquals("A Book in Parts", queue.items[2].mediaMetadata.title.toString())
    }

    @Test
    fun `an episode is one file that knows its episode`() {
        val one = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/ep")))

        val queue = PlaybackQueue.of(nowPlaying(one, episodeId = "e1"), startAt = 60.0, resolve)!!

        assertEquals(1, queue.items.size)
        assertEquals(QueuedFile("b1", "e1", 0.0, 1800.0), QueuedFile.of(queue.items[0]))
        assertEquals(60_000L, queue.positionMs)
    }

    @Test
    fun `nothing to play is no queue`() =
        assertNull(PlaybackQueue.of(nowPlaying(timeline = null), startAt = 0.0, resolve))

    @Test
    fun `no server to resolve against is no queue`() =
        assertNull(PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0) { null })
}
```

`BookPlaybackTest.kt`: the queue now comes from `PlaybackQueue`. Replace `playback(...)` and every `playback().load(startAtBookTime = X)` / `playback.load(...)` with this helper, and delete the two tests now covered by `PlaybackQueueTest` (`every file of the book is queued, in order` and `a saved position in a later file starts there, not at the beginning`):

```kotlin
    /** Queues [timeline] at [startAt] the way the player screen does, and returns its BookPlayback. */
    private fun loaded(startAt: Double, timeline: TrackTimeline = twoParts): BookPlayback {
        val nowPlaying = NowPlaying(
            itemId = "b1", title = "t", timeline = timeline, mediaDuration = null, chapters = null,
            episodeId = null, goToPodcast = false, description = null, line = { "" }
        )
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        return BookPlayback(player, timeline)
    }
```

e.g. `the position reported back is whole-book time` becomes `val playback = loaded(startAt = 4500.0)`; `a book in one file behaves as it always did` becomes `val playback = loaded(startAt = 900.0, timeline = single)`; `the player is left ready to play, not playing` becomes `loaded(startAt = 0.0)`.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.QueuedFileTest' --tests 'com.paulohenriquesg.fahrenheit.player.PlaybackQueueTest' --tests 'com.paulohenriquesg.fahrenheit.player.BookPlaybackTest'`
Expected: compilation FAILS: `Unresolved reference 'QueuedFile'`, `'PlaybackQueue'`, and `BookPlayback` constructor arity.

- [ ] **Step 3: Implement**

`QueuedFile.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import androidx.media3.common.MediaItem

/**
 * What one queued file knows about the book it belongs to.
 *
 * Carried in the MediaItem's request metadata because that is what survives
 * the trip from the screen's controller to the service: Media3 strips the
 * playable URI on the way, but keeps request metadata. The service reports
 * progress from these facts alone, so it never needs to know what a book is.
 *
 * @property startOffset seconds from the start of the book where this file begins.
 * @property bookTotal the length of everything that will play, in seconds.
 */
data class QueuedFile(
    val itemId: String,
    val episodeId: String?,
    val startOffset: Double,
    val bookTotal: Double
) {
    /** Whole-book time for a position within this file - what progress sync reports. */
    fun bookTime(positionInFile: Double): Double = startOffset + positionInFile

    /** Whether this file belongs to that book, or to that episode of that podcast. */
    fun isFor(itemId: String, episodeId: String?): Boolean =
        this.itemId == itemId && this.episodeId == episodeId

    fun toBundle(): Bundle = Bundle().apply {
        putString(ITEM_ID, itemId)
        episodeId?.let { putString(EPISODE_ID, it) }
        putDouble(START_OFFSET, startOffset)
        putDouble(BOOK_TOTAL, bookTotal)
    }

    companion object {
        private const val ITEM_ID = "fahrenheit.itemId"
        private const val EPISODE_ID = "fahrenheit.episodeId"
        private const val START_OFFSET = "fahrenheit.startOffset"
        private const val BOOK_TOTAL = "fahrenheit.bookTotal"

        /** The facts an item carries, or null for an item that is not one of ours. */
        fun of(item: MediaItem?): QueuedFile? {
            val extras = item?.requestMetadata?.extras ?: return null
            val itemId = extras.getString(ITEM_ID) ?: return null
            return QueuedFile(
                itemId = itemId,
                episodeId = extras.getString(EPISODE_ID),
                startOffset = extras.getDouble(START_OFFSET),
                bookTotal = extras.getDouble(BOOK_TOTAL)
            )
        }
    }
}
```

`PlaybackQueue.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** The items to queue, which one to start in, and how far into it. */
data class QueueStart(val items: List<MediaItem>, val index: Int, val positionMs: Long)

/**
 * A book or an episode as a Media3 queue: one item per file, in book order.
 *
 * Each item carries its file's URI twice. As the item's own URI it plays in a
 * player in this process; as request metadata it survives a MediaController,
 * which strips the first (see PlayableItems).
 */
object PlaybackQueue {

    /**
     * @param startAt where to begin, in whole-book seconds.
     * @param resolveUrl a track's server path to a full URL; null when there is
     *   no server to resolve against.
     * @return null when there is nothing to play.
     */
    fun of(nowPlaying: NowPlaying, startAt: Double, resolveUrl: (String) -> String?): QueueStart? {
        val timeline = nowPlaying.timeline ?: return null
        val items = (0 until timeline.size).map { index ->
            val track = timeline.track(index)
            val url = resolveUrl(track.contentUrl) ?: return null
            val file = QueuedFile(
                itemId = nowPlaying.itemId,
                episodeId = nowPlaying.episodeId,
                startOffset = track.startOffset,
                bookTotal = timeline.totalDuration
            )
            MediaItem.Builder()
                .setMediaId("${nowPlaying.itemId}/${nowPlaying.episodeId.orEmpty()}/$index")
                .setUri(url)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(Uri.parse(url))
                        .setExtras(file.toBundle())
                        .build()
                )
                .setMediaMetadata(MediaMetadata.Builder().setTitle(nowPlaying.title).build())
                .build()
        }
        val at = timeline.locate(startAt)
        return QueueStart(items, at.trackIndex, (at.positionInTrack * 1000).toLong())
    }
}
```

`BookPlayback.kt`: delete `load(...)`, delete the `resolveUrl` constructor parameter and its `@param` KDoc, and drop the now-unused `MediaItem` import (confirm with the compiler/lint, not by eye). Add to the class KDoc: `The queue itself is built by [PlaybackQueue].`

- [ ] **Step 4: Run to verify pass**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 5: Commit** - message: `Queue every file of a book with its place in the book (#16)`.

---

### Task 3: `ProgressReporter` has a last word

**Files:**
- Modify: `player/ProgressReporter.kt`
- Test: `ProgressReporterTest.kt`

**Interfaces:**
- Produces: `suspend fun ProgressReporter.finish()` - sends one closing update by `ProgressSync`'s rules, only if `run` has played at least one round; reads `position()` before its first suspension point.

- [ ] **Step 1: Write the failing tests** (append to `ProgressReporterTest`)

```kotlin
    private class Listening {
        val sent = mutableListOf<Double>()
        var at = 0.0
        var rounds = 0
        val reporter = ProgressReporter(
            send = { sent += it.currentTime!! },
            position = { at },
            total = { 1000.0 },
            pause = { at += 10.0 }
        )
        suspend fun play(rounds: Int) {
            this.rounds = rounds
            reporter.run { this.rounds-- > 0 }
        }
    }

    @Test
    fun `stopping sends where it stopped, not where the last round left it`() = runBlocking {
        val listening = Listening()
        listening.play(rounds = 1)   // sends 10
        listening.at = 13.0

        listening.reporter.finish()

        assertEquals(listOf(10.0, 13.0), listening.sent)
    }

    @Test
    fun `stopping where the last report left off sends nothing more`() = runBlocking {
        val listening = Listening()
        listening.play(rounds = 1)

        listening.reporter.finish()

        assertEquals(listOf(10.0), listening.sent)
    }

    // Review Focus 1: opened, never played, Back. A write here would overwrite
    // the resume point with itself and bump the book in Continue Listening.
    @Test
    fun `stopping before anything played sends nothing`() = runBlocking {
        val listening = Listening()
        listening.at = 900.0

        listening.reporter.finish()

        assertEquals(emptyList<Double>(), listening.sent)
    }

    @Test
    fun `a stop shorter than one round is still saved`() = runBlocking {
        val sent = mutableListOf<Double>()
        var at = 900.0
        var playing = true
        val reporter = ProgressReporter(
            send = { sent += it.currentTime!! },
            position = { at },
            total = { 1000.0 },
            // Paused after three seconds, before the first round's report.
            pause = { at = 903.0; playing = false; throw kotlinx.coroutines.CancellationException() }
        )
        runCatching { reporter.run { playing } }

        reporter.finish()

        assertEquals(listOf(903.0), sent)
    }
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.ProgressReporterTest'`
Expected: compilation FAILS with `Unresolved reference 'finish'`.

- [ ] **Step 3: Implement** - `ProgressReporter.kt` body becomes:

```kotlin
class ProgressReporter(
    private val send: suspend (MediaProgressRequest) -> Unit,
    private val position: () -> Double,
    private val total: () -> Double,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) }
) {
    private var lastSent: Double? = null

    /** Whether anything has played; a reporter that never played has nothing to say. */
    private var played = false

    suspend fun run(isPlaying: () -> Boolean) {
        while (isPlaying()) {
            played = true
            pause()
            val request = ProgressSync.next(position(), total(), lastSent) ?: continue
            // Only a delivered position counts as sent; a failed one is retried.
            if (runCatching { send(request) }.isSuccess) lastSent = request.currentTime
        }
    }

    /**
     * The closing update, when playback stops or the queue is about to change.
     *
     * Without it up to one round of listening is lost on every pause. The
     * position is read before anything suspends, so a caller that starts this
     * undispatched captures the position as it is now, before the queue
     * changes under it. Not retried: there is no next round to retry in.
     */
    suspend fun finish() {
        if (!played) return
        val request = ProgressSync.next(position(), total(), lastSent) ?: return
        lastSent = request.currentTime
        runCatching { send(request) }
    }
}
```

Update the class KDoc's last sentence to: `Kept out of the Activity, so the playback service owns it unchanged.`

- [ ] **Step 4: Run to verify pass** - Step 2 command. Expected: PASS (all 8 tests, including the 4 existing ones).

- [ ] **Step 5: Commit** - message: `Send the listening position once more when playback stops`.

---

### Task 4: Audio over the app's authenticated client

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `api/ApiClient.kt`
- Create: `player/AudioHttp.kt`
- Test: create `app/src/test/java/com/paulohenriquesg/fahrenheit/player/AudioHttpTest.kt`

**Interfaces:**
- Consumes: `ApiClient.buildAuthenticatedClient(sessionManager, refreshSession)` (existing, `internal`), `SessionManager`, `AuthSession`, test `FakeTokenStore` (package `com.paulohenriquesg.fahrenheit.api`, test source set).
- Produces: `object AudioHttp { fun dataSourceFactory(client: () -> OkHttpClient?): DataSource.Factory }`; `fun ApiClient.audioHttpClient(): OkHttpClient?` (null when signed out).

- [ ] **Step 1: Add the dependency** (needed for the test to compile against `OkHttpDataSource`)

`gradle/libs.versions.toml` `[libraries]`:

```toml
androidx-media3-datasource-okhttp = { module = "androidx.media3:media3-datasource-okhttp", version.ref = "media3" }
```

`app/build.gradle.kts`, under `implementation(libs.androidx.media3.exoplayer)`:

```kotlin
    implementation(libs.androidx.media3.datasource.okhttp)
```

- [ ] **Step 2: Write the failing tests** - `AudioHttpTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.FakeTokenStore
import com.paulohenriquesg.fahrenheit.auth.AuthSession
import com.paulohenriquesg.fahrenheit.auth.SessionManager
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Audio is fetched with the same credentials as every other request.
 *
 * An access token lasts an hour and is replaced on each refresh, so a long
 * book outlives the token it started with: file three must carry whatever
 * token is current when it is opened, not the one the queue was built with.
 */
@RunWith(AndroidJUnit4::class)
class AudioHttpTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        ApiClient.clearSession()
    }

    private fun sessionWith(access: String) = SessionManager(FakeTokenStore()).apply {
        persist(host = server.url("/").toString().trimEnd('/'), session = AuthSession(access, "valid-refresh", "listener"))
    }

    private fun read(factory: androidx.media3.datasource.DataSource.Factory, path: String): String {
        val source = factory.createDataSource()
        source.open(DataSpec(Uri.parse(server.url(path).toString())))
        return try { String(DataSourceUtil.readToEnd(source)) } finally { source.close() }
    }

    // Review Focus 4.
    @Test
    fun `an expired token is refreshed for audio too`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("audio-bytes"))
        val client = ApiClient.buildAuthenticatedClient(sessionWith("expired-access")) {
            AuthSession("fresh-access", "rotated-refresh", "listener")
        }

        val body = read(AudioHttp.dataSourceFactory { client }, "/api/items/b1/file/2")

        assertEquals("audio-bytes", body)
        assertEquals("Bearer expired-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh-access", server.takeRequest().getHeader("Authorization"))
    }

    // Review Focus 4: signed out and in as someone else between two files.
    @Test
    fun `each file uses the client current when it is opened`() {
        server.enqueue(MockResponse().setBody("one"))
        server.enqueue(MockResponse().setBody("two"))
        var current: OkHttpClient = ApiClient.buildAuthenticatedClient(sessionWith("first-user")) { error("no refresh") }
        val factory = AudioHttp.dataSourceFactory { current }

        read(factory, "/file/1")
        current = ApiClient.buildAuthenticatedClient(sessionWith("second-user")) { error("no refresh") }
        read(factory, "/file/2")

        assertEquals("Bearer first-user", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer second-user", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `the app hands out an audio client while signed in, and none after`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SharedPreferencesHandler(context).saveUserPreferences(UserPreferences("https://abs.test", "listener", "token", false))
        ApiClient.initialize(context)
        assertNotNull(ApiClient.audioHttpClient())

        ApiClient.clearSession()

        assertNull(ApiClient.audioHttpClient())
    }
}
```

- [ ] **Step 3: Run to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.AudioHttpTest'`
Expected: compilation FAILS: `Unresolved reference 'AudioHttp'`, `'audioHttpClient'`.

- [ ] **Step 4: Implement**

`AudioHttp.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient

/**
 * Where ExoPlayer gets audio: the app's own authenticated OkHttp client, which
 * adds the current access token to every request and refreshes it on a 401.
 *
 * The client is looked up each time a file is opened, not once: a book can
 * outlive a sign-out and sign-in, and the next file must go out as whoever is
 * signed in now.
 */
object AudioHttp {
    private val signedOut by lazy { OkHttpClient() }

    fun dataSourceFactory(client: () -> OkHttpClient?): DataSource.Factory = DataSource.Factory {
        OkHttpDataSource.Factory(client() ?: signedOut).createDataSource()
    }
}
```

`ApiClient.kt`: add `private var audioClient: OkHttpClient? = null` beside the other fields; in `initialize`, after `podcastApi = ...`:

```kotlin
        audioClient = buildAuthenticatedClient(sessionManager!!, refreshVia(hostValue))
```

in `forget()`: `audioClient = null`; and the accessor below `getPodcastApi()`:

```kotlin
    /** The authenticated client for streaming audio; null until a session is active. */
    fun audioHttpClient(): OkHttpClient? = audioClient
```

- [ ] **Step 5: Run to verify pass** - Step 3 command. Expected: PASS.

- [ ] **Step 6: Commit** - message: `Fetch audio with the app's authenticated client`.

---

### Task 5: `LeavingGuard`

**Files:**
- Create: `player/LeavingGuard.kt`
- Test: create `LeavingGuardTest.kt`

**Interfaces:**
- Produces: `class LeavingGuard(player: Player, beforeLeaving: () -> Unit) : ForwardingPlayer(player)` - calls `beforeLeaving()` *before* `stop`, `clearMediaItems`, and every `setMediaItem`/`setMediaItems` overload.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The last progress report must be taken from the queue as it was, so every
 * way of ending or replacing the queue has to be announced before it happens.
 */
@RunWith(AndroidJUnit4::class)
class LeavingGuardTest {

    private lateinit var player: ExoPlayer

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext()).build()
    }

    @After
    fun tearDown() = player.release()

    @Test
    fun `every way of changing or ending the queue is announced first`() {
        val seen = mutableListOf<Int>()
        val guard = LeavingGuard(player) { seen += player.mediaItemCount }
        val a = MediaItem.fromUri("https://abs.test/a")
        val b = MediaItem.fromUri("https://abs.test/b")

        guard.setMediaItems(listOf(a, b))       // was 0
        guard.setMediaItems(listOf(a), true)    // was 2
        guard.setMediaItems(listOf(a, b), 1, 0) // was 1
        guard.setMediaItem(a)                   // was 2
        guard.setMediaItem(a, 0L)               // was 1
        guard.setMediaItem(a, true)             // was 1
        guard.stop()                            // was 1
        guard.clearMediaItems()                 // was 1

        assertEquals(listOf(0, 2, 1, 2, 1, 1, 1, 1), seen)
        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `playing and pausing are not leaving`() {
        var announced = 0
        val guard = LeavingGuard(player) { announced++ }

        guard.play()
        guard.pause()
        guard.seekTo(1_000)

        assertEquals(0, announced)
    }
}
```

- [ ] **Step 2: Run to verify failure** - `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.LeavingGuardTest'`. Expected: compilation FAILS: `Unresolved reference 'LeavingGuard'`.

- [ ] **Step 3: Implement** - `LeavingGuard.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * The session's player, announcing a stop, a new queue or an empty one before
 * it happens.
 *
 * The closing progress report has to read the position of what was playing.
 * A listener hears about these changes only after the queue has moved on, so
 * the report would go out with the next book's position, or none.
 */
class LeavingGuard(player: Player, private val beforeLeaving: () -> Unit) : ForwardingPlayer(player) {

    override fun stop() { beforeLeaving(); super.stop() }

    override fun clearMediaItems() { beforeLeaving(); super.clearMediaItems() }

    override fun setMediaItem(mediaItem: MediaItem) { beforeLeaving(); super.setMediaItem(mediaItem) }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        beforeLeaving(); super.setMediaItem(mediaItem, startPositionMs)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        beforeLeaving(); super.setMediaItem(mediaItem, resetPosition)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) { beforeLeaving(); super.setMediaItems(mediaItems) }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        beforeLeaving(); super.setMediaItems(mediaItems, resetPosition)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        beforeLeaving(); super.setMediaItems(mediaItems, startIndex, startPositionMs)
    }
}
```

If the compiler rejects `List<MediaItem>` as not overriding (Java `List<MediaItem>` maps to `(Mutable)List`), change those three parameters to `MutableList<MediaItem>`.

- [ ] **Step 4: Run to verify pass** - Step 2 command. Expected: PASS.

- [ ] **Step 5: Commit** - message: `Announce a stop or a new queue before it happens`.

---

### Task 6: `PlaybackReporting`

**Files:**
- Create: `player/PlaybackReporting.kt`
- Test: create `PlaybackReportingTest.kt`, and the shared test helper `app/src/test/java/com/paulohenriquesg/fahrenheit/player/FakeAudio.kt` (Tasks 8 and 9 use it too)

**Interfaces:**
- Consumes: `ProgressReporter` + `finish()` (Task 3), `QueuedFile` (Task 2), `LeavingGuard` (Task 5), `PlaybackQueue` (Task 2).
- Produces: `class PlaybackReporting(player: Player, scope: CoroutineScope, send: suspend (QueuedFile, MediaProgressRequest) -> Unit, pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) }) : Player.Listener` with `fun beforeLeaving()`.

- [ ] **Step 1: Write the failing tests**

`FakeAudio.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.FakeTimeline

/**
 * Fake media where every file is an hour long, whatever its URI. The default
 * fake file is 10 seconds, and a start position past that is clamped, so a
 * book resumed 15 minutes into a file would silently start elsewhere.
 */
fun hourLongFiles() = FakeMediaSourceFactory(
    FakeTimeline.TimelineWindowDefinition.Builder().setDurationUs(3_600_000_000L)
)
```

`PlaybackReportingTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The service reports whatever is playing, in whole-book time, to the item it
 * belongs to - and has a last word before a stop or a new queue.
 *
 * Fake media ([hourLongFiles]): book time comes from each file's startOffset,
 * so what the fake files contain does not matter.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackReportingTest {

    private lateinit var player: ExoPlayer
    private lateinit var guarded: Player
    private val sent = mutableListOf<Pair<QueuedFile, Double>>()

    private fun nowPlaying(itemId: String, timeline: TrackTimeline, episodeId: String? = null) = NowPlaying(
        itemId = itemId, title = itemId, timeline = timeline, mediaDuration = null, chapters = null,
        episodeId = episodeId, goToPodcast = episodeId != null, description = null, line = { "" }
    )

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )
    private val oneFile = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/ep")))

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
        val reporting = PlaybackReporting(
            player,
            CoroutineScope(Dispatchers.Unconfined),
            send = { file, request -> sent += file to request.currentTime!! },
            // Rounds never come round: only the closing reports are under test.
            pause = { awaitCancellation() }
        )
        player.addListener(reporting)
        guarded = LeavingGuard(player, reporting::beforeLeaving)
    }

    @After
    fun tearDown() = player.release()

    private fun queue(nowPlaying: NowPlaying, startAt: Double) {
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        guarded.setMediaItems(queue.items, queue.index, queue.positionMs)
        guarded.prepare()
    }

    private fun playUntil(positionMs: Long) {
        guarded.play()
        run(player).untilPositionAtLeast(positionMs)
    }

    @Test
    fun `pausing reports the whole-book position to the book`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val (file, time) = sent.single()
        assertEquals("b1", file.itemId)
        assertEquals(3605.0, time, 0.5)
    }

    @Test
    fun `stopping reports where it was, not where stopping leaves it`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)

        guarded.stop()
        guarded.clearMediaItems()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(3605.0, sent.single().second, 0.5)
    }

    // Review Focus 2.
    @Test
    fun `replacing one book with another reports the first to the first`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)

        queue(nowPlaying("b2", twoParts), startAt = 0.0)
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(listOf("b1"), sent.map { it.first.itemId })
    }

    // Review Focus 1.
    @Test
    fun `stopping something never played sends nothing`() {
        queue(nowPlaying("b1", twoParts), startAt = 900.0)

        guarded.stop()
        guarded.clearMediaItems()
        run(player).untilPendingCommandsAreFullyHandled()

        assertTrue(sent.isEmpty())
    }

    @Test
    fun `an episode reports to its episode`() {
        queue(nowPlaying("p1", oneFile, episodeId = "e1"), startAt = 0.0)
        playUntil(3_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val (file, time) = sent.single()
        assertEquals("e1", file.episodeId)
        assertEquals(3.0, time, 0.5)
    }
}
```

- [ ] **Step 2: Run to verify failure** - `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.PlaybackReportingTest'`. Expected: compilation FAILS: `Unresolved reference 'PlaybackReporting'`.

- [ ] **Step 3: Implement** - `PlaybackReporting.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reports the listening position of whatever [player] is playing.
 *
 * One [ProgressReporter] per item: its ids come from the queued file, so a
 * report can never go to a book other than the one it measured. Positions are
 * whole-book time ([QueuedFile.bookTime]), never time within one file.
 *
 * Install as a listener on the player, and pass [beforeLeaving] to the
 * [LeavingGuard] in front of it.
 *
 * @param send delivers one report for that file's item or episode; throwing is a failed send.
 */
class PlaybackReporting(
    private val player: Player,
    private val scope: CoroutineScope,
    private val send: suspend (QueuedFile, MediaProgressRequest) -> Unit,
    private val pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) }
) : Player.Listener {

    private var reportingFor: QueuedFile? = null
    private var reporter: ProgressReporter? = null
    private var rounds: Job? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) start() else finish()
    }

    /** The queue is about to be stopped, replaced or cleared. */
    fun beforeLeaving() = finish()

    private fun start() {
        val current = QueuedFile.of(player.currentMediaItem) ?: return
        val owner = reportingFor
        if (reporter == null || owner == null || !owner.isFor(current.itemId, current.episodeId)) {
            reportingFor = current
            reporter = ProgressReporter(
                send = { send(current, it) },
                position = { positionIn(current) },
                total = { current.bookTotal },
                pause = pause
            )
        }
        if (rounds?.isActive == true) return
        val active = reporter ?: return
        rounds = scope.launch { active.run { player.isPlaying } }
    }

    private fun finish() {
        rounds?.cancel()
        rounds = null
        val active = reporter ?: return
        // Undispatched, so the position is read now, before the queue changes.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { active.finish() }
    }

    /** Whole-book time, or -1 (never sent) once the player has moved on to something else. */
    private fun positionIn(owner: QueuedFile): Double {
        val now = QueuedFile.of(player.currentMediaItem) ?: return -1.0
        if (!now.isFor(owner.itemId, owner.episodeId)) return -1.0
        return now.bookTime(player.currentPosition / 1000.0)
    }
}
```


- [ ] **Step 4: Run to verify pass** - Step 2 command. Expected: PASS (5 tests). If a timing assertion is off by more than 0.5 s, print `player.currentPosition` at the pause and check `untilPositionAtLeast` overshoot before widening the tolerance; never widen past 1.0 (the `ProgressSync` movement threshold).

- [ ] **Step 5: Commit** - message: `Report whatever is playing, to the item it belongs to`.

---

### Task 7: `PlaybackService`, `PlayableItems`, `Playback`

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `player/PlayableItems.kt`, `player/PlaybackService.kt`, `player/Playback.kt`
- Test: create `PlayableItemsTest.kt`, `PlaybackServiceTest.kt`

**Interfaces:**
- Consumes: `AudioHttp` (Task 4), `ApiClient.audioHttpClient()` (Task 4), `PlaybackReporting` (Task 6), `LeavingGuard` (Task 5), `QueuedFile`/`PlaybackQueue` (Task 2).
- Produces:
  - `object PlayableItems { fun resolve(items: List<MediaItem>): List<MediaItem>? }`
  - `class PlaybackService : MediaSessionService` with `internal val sessionPlayer: Player?` (for tests)
  - `object Playback { fun connect(context: Context): ListenableFuture<MediaController>; fun leave(controller: Player, finishing: Boolean); fun stop(context: Context) }`

- [ ] **Step 1: Dependency and manifest** (the service test resolves the service from the merged manifest)

`libs.versions.toml`: `androidx-media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }`
`build.gradle.kts`: `implementation(libs.androidx.media3.session)` under the exoplayer line.

`AndroidManifest.xml`, after the INTERNET permission:

```xml
    <!-- Playback outlives the player screen (#16): a media foreground service,
         and a wake lock so streaming survives the TV's screensaver. -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
```

inside `<application>`, after the PlayerActivity line:

```xml
        <!-- Exported so the system's media controls and the remote can reach it. -->
        <service
            android:name=".player.PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
```

- [ ] **Step 2: Write the failing tests**

`PlayableItemsTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Media3 strips an item's playable URI on its way from a controller to the
 * session, and by default refuses items without one. Ours carry it in request
 * metadata, which survives, and are rebuilt from it.
 */
@RunWith(AndroidJUnit4::class)
class PlayableItemsTest {

    private fun asSentByAController(uri: String?) = MediaItem.Builder()
        .setMediaId("b1//0")
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri?.let(Uri::parse))
                .setExtras(QueuedFile("b1", null, 0.0, 60.0).toBundle())
                .build()
        )
        .build()

    @Test
    fun `an item is playable again from its request metadata, keeping its facts`() {
        val resolved = PlayableItems.resolve(listOf(asSentByAController("https://abs.test/part1")))!!.single()

        assertEquals("https://abs.test/part1", resolved.localConfiguration?.uri.toString())
        assertEquals(QueuedFile("b1", null, 0.0, 60.0), QueuedFile.of(resolved))
    }

    @Test
    fun `an item that is already playable is left alone`() {
        val item = MediaItem.fromUri("https://abs.test/x")

        assertEquals(item, PlayableItems.resolve(listOf(item))!!.single())
    }

    @Test
    fun `an item with no URI anywhere refuses the whole queue`() =
        assertNull(PlayableItems.resolve(listOf(asSentByAController("https://abs.test/a"), asSentByAController(null))))
}
```

`PlaybackServiceTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSessionService
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

/**
 * The wiring: a real MediaController, connected to the real service, the way
 * the player screen will use it. The pieces are tested on their own elsewhere;
 * this is what breaks when they are put together.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var service: ServiceController<PlaybackService>
    private var controller: MediaController? = null

    private val twoParts = NowPlaying(
        itemId = "b1", title = "A Book in Parts",
        timeline = TrackTimeline(
            listOf(
                TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
                TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
            )
        ),
        mediaDuration = null, chapters = null, episodeId = null, goToPodcast = false, description = null, line = { "" }
    )

    @Before
    fun setUp() {
        service = Robolectric.buildService(PlaybackService::class.java).create()
        val bind = Intent(MediaSessionService.SERVICE_INTERFACE).setClass(context, PlaybackService::class.java)
        shadowOf(context).setComponentNameAndServiceForBindService(
            ComponentName(context, PlaybackService::class.java),
            service.get().onBind(bind)
        )
    }

    @After
    fun tearDown() {
        controller?.release()
        service.destroy()
    }

    private fun connect(): MediaController {
        val future = Playback.connect(context)
        runMainLooperUntil { future.isDone }
        return future.get().also { controller = it }
    }

    private fun queued(controller: MediaController, startAt: Double) {
        val queue = PlaybackQueue.of(twoParts, startAt) { "https://abs.test$it" }!!
        controller.setMediaItems(queue.items, queue.index, queue.positionMs)
        runMainLooperUntil { service.get().sessionPlayer!!.mediaItemCount == 2 }
    }

    @Test
    fun `a queue sent through a controller arrives playable, in whole-book time`() {
        queued(connect(), startAt = 3900.0)

        val player = service.get().sessionPlayer!!
        assertEquals("https://abs.test/part2", player.getMediaItemAt(1).localConfiguration?.uri.toString())
        val file = QueuedFile.of(player.currentMediaItem)!!
        assertEquals(3900.0, file.bookTime(player.currentPosition / 1000.0), 0.001)
    }

    @Test
    fun `the controller sees which book is queued, so a screen can reattach`() {
        val controller = connect()
        queued(controller, startAt = 0.0)
        runMainLooperUntil { controller.mediaItemCount == 2 }

        assertTrue(QueuedFile.of(controller.currentMediaItem)!!.isFor("b1", null))
    }

    @Test
    fun `leaving the screen by Back empties the queue`() {
        val controller = connect()
        queued(controller, startAt = 0.0)

        Playback.leave(controller, finishing = true)

        runMainLooperUntil { service.get().sessionPlayer!!.mediaItemCount == 0 }
    }

    @Test
    fun `leaving the screen for Home keeps the queue`() {
        val controller = connect()
        queued(controller, startAt = 0.0)

        Playback.leave(controller, finishing = false)
        // Let anything that would have been sent arrive.
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(2, service.get().sessionPlayer!!.mediaItemCount)
    }

    @Test
    fun `stopping from elsewhere empties the queue`() {
        queued(connect(), startAt = 0.0)

        Playback.stop(context)

        runMainLooperUntil { service.get().sessionPlayer!!.mediaItemCount == 0 }
    }
}
```

**If Robolectric cannot host the connection** (the controller future never completes, or fails with a binding error): do not delete or `@Ignore` silently. Report it to the human with the exact error, keep `PlayableItemsTest`, and replace the service test with one that builds `PlaybackService`'s parts directly (an ExoPlayer from `TestExoPlayerBuilder`, wrapped in `LeavingGuard`, `MediaSession.Builder(context, guarded).setCallback(...)`), leaving the controller connection to the device check in Task 10. Note the gap in the Task 10 checklist.

- [ ] **Step 3: Run to verify failure** - `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.PlayableItemsTest' --tests 'com.paulohenriquesg.fahrenheit.player.PlaybackServiceTest'`. Expected: compilation FAILS: `Unresolved reference 'PlayableItems'`, `'PlaybackService'`, `'Playback'`.

- [ ] **Step 4: Implement**

`PlayableItems.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem

/**
 * Turns items as a MediaController delivers them back into items a player can
 * play. Media3 strips an item's own URI on that trip; [PlaybackQueue] put a
 * copy in request metadata, which arrives intact.
 */
object PlayableItems {

    /** @return null if any item has no URI at all - a queue with a hole in it is refused whole. */
    fun resolve(items: List<MediaItem>): List<MediaItem>? = items.map { item ->
        when {
            item.localConfiguration != null -> item
            else -> {
                val uri = item.requestMetadata.mediaUri ?: return null
                item.buildUpon().setUri(uri).build()
            }
        }
    }
}
```

`PlaybackService.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import retrofit2.awaitResponse

/**
 * Where playback lives, so it outlives the player screen (#16).
 *
 * The screen drives it through a MediaController (see [Playback]); so do the
 * remote's media keys and the system's controls. It knows nothing about books:
 * each queued file carries its own [QueuedFile] facts, and progress is
 * reported from those.
 */
class PlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaSession? = null

    /** The player the session drives; for tests. */
    internal val sessionPlayer: Player? get() = session?.player

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(AudioHttp.dataSourceFactory { ApiClient.audioHttpClient() })
            )
            // Speech, and audio focus handled: another app taking the audio pauses this one.
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            // Keeps streaming when a TV's screensaver starts.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        val reporting = PlaybackReporting(exo, scope, send = ::sendProgress)
        exo.addListener(reporting)
        exo.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                // Nothing queued: nothing to keep the service for.
                if (timeline.isEmpty) stopSelf()
            }
        })
        session = MediaSession.Builder(this, LeavingGuard(exo, reporting::beforeLeaving))
            .setCallback(QueueCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun sendProgress(file: QueuedFile, request: MediaProgressRequest) {
        val api = ApiClient.getApiService() ?: error("signed out")
        val call = file.episodeId?.let { api.userCreateOrUpdateMediaProgress(file.itemId, it, request) }
            ?: api.userCreateOrUpdateMediaProgress(file.itemId, request)
        val response = call.awaitResponse()
        // Logged, not shown: the next round retries.
        if (!response.isSuccessful) {
            Log.w(TAG, "Progress rejected: ${response.code()}")
            error("progress rejected: ${response.code()}")
        }
    }

    /** Makes items from a controller playable again (see [PlayableItems]). */
    private object QueueCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val playable = PlayableItems.resolve(mediaItems)
                ?: return Futures.immediateFailedFuture(UnsupportedOperationException("An item has no URI"))
            return Futures.immediateFuture(playable.toMutableList())
        }
    }

    private companion object {
        const val TAG = "PlaybackService"
    }
}
```

`Playback.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** The app's way to the [PlaybackService]. */
object Playback {

    fun connect(context: Context): ListenableFuture<MediaController> =
        MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .buildAsync()

    /**
     * The player screen is going away. Back ([finishing]) stops playback - with
     * no mini-player yet there would be no other way to stop it; Home leaves it
     * playing. Either way the controller is released by the caller afterwards.
     */
    fun leave(controller: Player, finishing: Boolean) {
        if (!finishing) return
        controller.stop()
        controller.clearMediaItems()
    }

    /** Stops whatever is playing, from anywhere - sign-out uses it. */
    fun stop(context: Context) {
        val future = connect(context.applicationContext)
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            leave(controller, finishing = true)
            controller.release()
        }, ContextCompat.getMainExecutor(context))
    }
}
```

- [ ] **Step 5: Run to verify pass** - Step 3 command. Expected: PASS. If `stopping from elsewhere empties the queue` fails because `release()` drops the commands before they are sent, move `controller.release()` into a `controller.addListener` that waits for `mediaItemCount == 0`, re-run, and say so in the commit body.

- [ ] **Step 6: Commit** - message: `Play from a media session service`.

---

### Task 8: The transport drives a `Player`

**Files:**
- Modify: `player/MediaPlayerController.kt`, `res/values/strings.xml`
- Test: rewrite `TransportFocusTest.kt`; create `TransportTest.kt`

**Interfaces:**
- Consumes: `BookPlayback(player, timeline)` (Task 2).
- Produces:

```kotlin
@Composable
fun MediaPlayerController(
    player: Player,
    playback: BookPlayback,
    totalTime: Double,
    chapters: List<Chapter>? = null,
    onCurrentTimeUpdate: (Double) -> Unit = {}
)
```

Behaviour decisions recorded here (the spec keeps the layout and says nothing about Stop):
- **Stop pauses and keeps the place.** Today's Stop reset to 0:00; with a closing progress report that would save 0:00 over the listener's resume point. Back is now the way to stop playback entirely.
- The play button's content description stays `"Play"` / `"Pause"` (device driving reads it).

- [ ] **Step 1: Add the string** - `res/values/strings.xml`, beside `item_load_failed`:

```xml
    <string name="playback_failed">Couldn't play this</string>
```

- [ ] **Step 2: Write the failing tests**

`TransportFocusTest.kt` - replace the body with an ExoPlayer-backed transport:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Seen on the stick: nothing on the player held focus, so no D-pad key reached
 * it. Frame 4 of the mocks: play is focused on arrival.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class TransportFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `play holds focus on arrival`() {
        val player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        val timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x")))
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(player = player, playback = BookPlayback(player, timeline), totalTime = 1800.0)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Play").assertIsFocused()
        player.release()
    }
}
```

`TransportTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The transport works the player it is given, in whole-book time (#16). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class TransportTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )

    @After
    fun tearDown() = player.release()

    private fun show(player: ExoPlayer, timeline: TrackTimeline = twoParts) {
        this.player = player
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(player = player, playback = BookPlayback(player, timeline), totalTime = timeline.totalDuration)
            }
        }
        compose.waitForIdle()
    }

    /** Queued but not prepared: an unprepared player keeps the position it is given, exactly. */
    private fun queuedAt(startAt: Double): ExoPlayer {
        val p = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        val nowPlaying = NowPlaying("b1", "t", twoParts, null, null, null, false, null) { "" }
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        p.setMediaItems(queue.items, queue.index, queue.positionMs)
        return p
    }

    @Test
    fun `play plays and pause pauses`() {
        show(queuedAt(0.0))

        compose.onNodeWithContentDescription("Play").performClick()
        compose.waitForIdle()
        assertTrue(player.playWhenReady)

        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitForIdle()
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `skipping forward near the end of a file crosses into the next`() {
        show(queuedAt(3590.0))

        compose.onNodeWithContentDescription(compose.activity.getString(com.paulohenriquesg.fahrenheit.R.string.skip_forward_30_seconds)).performClick()
        compose.waitForIdle()

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(20_000L, player.currentPosition)
    }

    @Test
    fun `the times are whole-book times`() {
        show(queuedAt(3900.0))

        compose.onNodeWithText("Current Time: 01:05:00").assertIsDisplayed()
        compose.onNodeWithText("Total Time: 01:30:00").assertIsDisplayed()
    }

    @Test
    fun `stop pauses and keeps the place`() {
        show(queuedAt(3900.0))
        compose.onNodeWithContentDescription("Play").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription(compose.activity.getString(com.paulohenriquesg.fahrenheit.R.string.stop)).performClick()
        compose.waitForIdle()

        assertFalse(player.playWhenReady)
        assertEquals(1, player.currentMediaItemIndex)
    }

    // Review Focus 5.
    @Test
    fun `after an error, play retries where it was`() {
        // The real media source: an unresolvable host fails to load.
        val failing = TestExoPlayerBuilder(compose.activity).build()
        failing.setMediaItems(
            listOf(MediaItem.fromUri("http://abs.invalid/1.mp3"), MediaItem.fromUri("http://abs.invalid/2.mp3")),
            1, 300_000L
        )
        failing.prepare()
        run(failing).untilPlayerError()
        show(failing)

        compose.onNodeWithText("Couldn't play this").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").performClick()
        compose.waitForIdle()

        assertNull(player.playerError)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(300_000L, player.currentPosition)
        assertTrue(player.playbackState != Player.STATE_IDLE)
    }
}
```

- [ ] **Step 3: Run to verify failure** - `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.TransportFocusTest' --tests 'com.paulohenriquesg.fahrenheit.player.TransportTest'`. Expected: compilation FAILS: `No parameter with name 'player'` for `MediaPlayerController`.

- [ ] **Step 4: Implement** - replace the `MediaPlayerController` composable (keep `drawLineAtPercentage`, `SKIP_SECONDS` and `TransportButton` as they are):

```kotlin
/**
 * The transport: skip, play/pause, stop, scrubber and times, for whatever
 * [player] is playing, in whole-book time (#16).
 *
 * It holds no playback of its own. The player is the screen's MediaController,
 * so this draws what the service is doing and sends it commands; positions
 * cross file boundaries through [playback].
 */
@Composable
fun MediaPlayerController(
    player: Player,
    playback: BookPlayback,
    totalTime: Double,
    chapters: List<Chapter>? = null,
    onCurrentTimeUpdate: (Double) -> Unit = {}
) {
    var isPlaying by remember(player) { mutableStateOf(player.playWhenReady) }
    var failed by remember(player) { mutableStateOf(player.playerError != null) }
    var currentTime by remember(player) { mutableDoubleStateOf(playback.bookPosition()) }
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            // Play/pause follows what was asked for, so it answers a press at
            // once rather than after buffering.
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { isPlaying = playWhenReady }
            override fun onPlayerErrorChanged(error: PlaybackException?) { failed = error != null }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Polls only while playing, as before: a loop that never ends keeps
    // Compose from ever being idle, which also hangs its UI tests.
    LaunchedEffect(player, isPlaying) {
        currentTime = playback.bookPosition()
        onCurrentTimeUpdate(currentTime)
        while (isPlaying) {
            delay(1000L)
            currentTime = playback.bookPosition()
            onCurrentTimeUpdate(currentTime)
        }
    }

    fun seekTo(seconds: Double) {
        playback.seekToBookTime(seconds)
        currentTime = seconds
        onCurrentTimeUpdate(seconds)
    }

    // Something must hold focus or no D-pad key reaches this screen at all;
    // play, so the remote's centre button does the obvious thing (frame 4).
    val playFocus = rememberInitialFocus(enabled = true, player)

    Column(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransportButton(
                onClick = { seekTo(PlaybackPosition.skip(currentTime, -SKIP_SECONDS, totalTime)) },
                size = 48.dp,
                container = TvMaterialTheme.colorScheme.secondaryContainer,
                content = TvMaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.skip_back_30_seconds))
            }

            TransportButton(
                onClick = {
                    when {
                        // After an error the player is idle where it failed;
                        // preparing again retries from there.
                        failed -> { player.prepare(); player.play() }
                        isPlaying -> player.pause()
                        else -> player.play()
                    }
                },
                size = 56.dp,
                container = TvMaterialTheme.colorScheme.primary,
                content = TvMaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.focusRequester(playFocus)
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            // Pauses and keeps the place: returning to 0:00 would now be saved
            // over the resume point by the closing progress report. Back stops.
            TransportButton(
                onClick = { player.pause() },
                size = 48.dp,
                container = Color.Transparent,
                content = TvMaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.stop))
            }

            TransportButton(
                onClick = { seekTo(PlaybackPosition.skip(currentTime, SKIP_SECONDS, totalTime)) },
                size = 48.dp,
                container = TvMaterialTheme.colorScheme.secondaryContainer,
                content = TvMaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.skip_forward_30_seconds))
            }
        }

        Box(modifier = Modifier.padding(top = 16.dp)) {
            Slider(
                value = PlaybackPosition.fraction(currentTime, totalTime),
                onValueChange = { seekTo(it * totalTime) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { sliderSize = it.size }
            )
            val chapterColor = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(modifier = Modifier.matchParentSize()) {
                PlaybackPosition.chapterMarks(chapters, totalTime).forEach { percentage ->
                    drawLineAtPercentage(percentage, sliderSize.width, 4.dp.toPx(), chapterColor)
                }
            }
        }

        Row(modifier = Modifier.padding(top = 8.dp)) {
            if (failed) {
                Text(
                    text = stringResource(R.string.playback_failed),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Current Time: ${PlaybackPosition.clock(currentTime)}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total Time: ${PlaybackPosition.clock(totalTime)}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
```

Imports: add `androidx.media3.common.Player`, `androidx.media3.common.PlaybackException`, `androidx.compose.runtime.DisposableEffect`, `androidx.compose.runtime.mutableDoubleStateOf`; remove `android.support.v4.media.session.MediaSessionCompat`, `androidx.core.net.toUri`, `android.net.Uri`, `rememberCoroutineScope`, `launch` - then let the compiler and `lintDebug` confirm what is unused (`getValue`/`setValue` stay). `GlobalMediaPlayer` is no longer referenced here.

- [ ] **Step 5: Run to verify pass** - Step 3 command. Expected: PASS. If a test that presses Play times out in `waitForIdle` (the polling loop keeps Compose busy), set `compose.mainClock.autoAdvance = false` after `show(...)` in that test and drive frames with `compose.mainClock.advanceTimeByFrame()`; do not remove the loop. If `after an error, play retries where it was` cannot reach `untilPlayerError` (Robolectric resolving `abs.invalid` differently), use `.withTimeoutMs(30_000)`; if it still cannot fail, report it rather than weaken the assertion.

- [ ] **Step 6: Commit** - message: `The transport drives a Media3 player, in whole-book time`.

---

### Task 9: The player screen uses the service; `GlobalMediaPlayer` goes

**Files:**
- Create: `player/PlayerStart.kt`
- Modify: `player/PlayerActivity.kt`, `main/MainScreen.kt`, `app/build.gradle.kts`
- Delete: `player/GlobalMediaPlayer.kt`, `test/.../player/GlobalMediaPlayerTest.kt`
- Test: create `PlayerStartTest.kt`; modify `MediaPlayerAuthenticationTest.kt`

**Interfaces:**
- Consumes: `Playback.connect`/`leave`/`stop` (Task 7), `PlaybackQueue`, `QueuedFile` (Task 2), `ResumePoint.decide` (existing), `MediaPlayerController(player, playback, totalTime, chapters, onCurrentTimeUpdate)` (Task 8).
- Produces: `object PlayerStart { fun begin(player: Player, nowPlaying: NowPlaying, progress: MediaProgressResponse?, autoPlay: Boolean, resolveUrl: (String) -> String?): Boolean }`.

- [ ] **Step 1: Write the failing tests** - `PlayerStartTest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** What the player screen does with the service it finds: reattach, or load. */
@RunWith(AndroidJUnit4::class)
class PlayerStartTest {

    private lateinit var player: ExoPlayer

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )

    private fun book(id: String, timeline: TrackTimeline? = twoParts) =
        NowPlaying(id, id, timeline, null, null, null, false, null) { "" }

    private fun episode(id: String) =
        NowPlaying("p1", id, TrackTimeline(listOf(TimelineTrack(1, 0.0, 1800.0, "/$id"))), null, null, id, true, null) { "" }

    private fun savedAt(seconds: Double) = MediaProgressResponse(currentTime = seconds)

    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
    }

    @After
    fun tearDown() = player.release()

    @Test
    fun `a book not yet queued starts at its saved position, waiting for play`() {
        assertTrue(PlayerStart.begin(player, book("b1"), savedAt(4500.0), autoPlay = false, resolve))

        assertEquals(2, player.mediaItemCount)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `opened to play, it plays`() {
        PlayerStart.begin(player, book("b1"), null, autoPlay = true, resolve)

        assertTrue(player.playWhenReady)
    }

    // Review Focus 3.
    @Test
    fun `the book already playing is left where it is`() {
        PlayerStart.begin(player, book("b1"), savedAt(4500.0), autoPlay = true, resolve)

        // Back to the screen: the server's copy is older than what is playing.
        assertTrue(PlayerStart.begin(player, book("b1"), savedAt(100.0), autoPlay = false, resolve))

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `another book replaces what was queued`() {
        PlayerStart.begin(player, book("b1"), savedAt(4500.0), autoPlay = false, resolve)

        PlayerStart.begin(player, book("b2"), null, autoPlay = false, resolve)

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("b2", null))
        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    fun `another episode of the same podcast replaces the one queued`() {
        PlayerStart.begin(player, episode("e1"), null, autoPlay = false, resolve)

        PlayerStart.begin(player, episode("e2"), null, autoPlay = false, resolve)

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("p1", "e2"))
    }

    @Test
    fun `nothing to play leaves the queue alone`() {
        PlayerStart.begin(player, book("b1"), null, autoPlay = false, resolve)

        assertFalse(PlayerStart.begin(player, book("b2", timeline = null), null, autoPlay = false, resolve))

        assertTrue(QueuedFile.of(player.currentMediaItem)!!.isFor("b1", null))
    }
}
```

- [ ] **Step 2: Run to verify failure** - `./gradlew :app:testDebugUnitTest --tests 'com.paulohenriquesg.fahrenheit.player.PlayerStartTest'`. Expected: compilation FAILS: `Unresolved reference 'PlayerStart'`.

- [ ] **Step 3: Implement `PlayerStart.kt`**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/** What the player screen does with the player it connects to. */
object PlayerStart {

    /**
     * Reattaches if [player] is already on this book or episode - leaving it
     * where it is, since what is playing is newer than the server's copy -
     * and otherwise queues it at the saved position, playing if [autoPlay].
     *
     * @return false when there is nothing to play; the queue is then untouched.
     */
    fun begin(
        player: Player,
        nowPlaying: NowPlaying,
        progress: MediaProgressResponse?,
        autoPlay: Boolean,
        resolveUrl: (String) -> String?
    ): Boolean {
        if (QueuedFile.of(player.currentMediaItem)?.isFor(nowPlaying.itemId, nowPlaying.episodeId) == true) return true
        val start = ResumePoint.decide(progress, nowPlaying.trackTotal, nowPlaying.mediaDuration)
        val queue = PlaybackQueue.of(nowPlaying, start.positionSeconds, resolveUrl) ?: return false
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        if (autoPlay) player.play()
        return true
    }
}
```

- [ ] **Step 4: Run to verify pass** - Step 2 command. Expected: PASS.

- [ ] **Step 5: Rewire `PlayerActivity`**

Remove: the `mediaSession` field and its whole setup block in `onCreate`; `isPlaying`; `reporting`; the `playing(...)` function; the `MediaSessionCompat`/`PlaybackStateCompat` imports; the `GlobalMediaPlayer.release()` and `mediaSession.release()` lines in `onDestroy` (delete `onDestroy` if empty). Update the class KDoc's second paragraph to: `Playback lives in [PlaybackService]; this screen drives it through a MediaController, connected while it is visible. Back stops playback, Home leaves it playing.`

Add fields and lifecycle:

```kotlin
    private var controller by mutableStateOf<MediaController?>(null)
    private var connecting: ListenableFuture<MediaController>? = null
    private var connectFailed by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        val future = Playback.connect(this).also { connecting = it }
        future.addListener({
            controller = runCatching { future.get() }.getOrNull()
            connectFailed = controller == null
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controller?.let { Playback.leave(it, finishing = isFinishing) }
        controller = null
        connecting?.let { MediaController.releaseFuture(it) }
        connecting = null
        super.onStop()
    }
```

In `Player(itemId, episodeId, autoPlay)`, keep the existing `LaunchedEffect(itemId, episodeId)` (progress first, then `nowPlaying`, then `openSession`), and replace the `when` with:

```kotlin
        val playing = nowPlaying
        val connected = controller
        var ready by remember(connected, playing) { mutableStateOf(false) }
        LaunchedEffect(connected, playing) {
            if (connected == null || playing == null) return@LaunchedEffect
            ready = PlayerStart.begin(connected, playing, progress, autoPlay) { ApiClient.generateFullUrl(it) }
            if (!ready) failed = true
        }
        when {
            failed || connectFailed -> Text(
                text = stringResource(R.string.item_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            playing == null || connected == null || !ready -> Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
            else -> PlayerScreen(
                nowPlaying = playing,
                currentTime = currentTime,
                onGoToPodcast = { startActivity(podcastIntent(this, itemId)) },
                transport = {
                    // ready implies a timeline: PlayerStart refuses a NowPlaying without one.
                    val timeline = playing.timeline
                    if (timeline != null) {
                        val playback = remember(connected, timeline) { BookPlayback(connected, timeline) }
                        MediaPlayerController(
                            player = connected,
                            playback = playback,
                            totalTime = timeline.totalDuration,
                            chapters = playing.chapters,
                            onCurrentTimeUpdate = { currentTime = it }
                        )
                    }
                }
            )
        }
```

Imports to add: `androidx.media3.session.MediaController`, `androidx.core.content.ContextCompat`, `com.google.common.util.concurrent.ListenableFuture`.

- [ ] **Step 6: Sign-out, deletions, dependency**

`MainScreen.kt` `signOut()`: replace `GlobalMediaPlayer.release()` with nothing, and make `Playback.stop(context)` the **first** line of `signOut()`, updating the comment to: `// Stop playback first, while its last report can still be sent; then the stored session and the client built from it.` Replace the import `...player.GlobalMediaPlayer` with `...player.Playback`.

Delete `GlobalMediaPlayer.kt` and `GlobalMediaPlayerTest.kt` (`git rm`). In `MediaPlayerAuthenticationTest.kt` delete the test `media player can be reset and reinitialized` and its `GlobalMediaPlayer` import.

`build.gradle.kts`: delete `implementation(libs.androidx.media)`; delete the `androidx-media` line in `libs.versions.toml` only if nothing else references `libs.androidx.media` (the version `media = "1.7.0"` too). If the build then fails on a `android.support.v4` reference anywhere, put the dependency back and note it - do not chase it in this task.

- [ ] **Step 7: Full gate**

```sh
set -o pipefail
./gradlew :app:testDebugUnitTest :app:lintDebug 2>&1 | tee /tmp/gate.log; echo "EXIT_CODE=$?" >> /tmp/gate.log
```

Expected: last line `EXIT_CODE=0`; test count = previous 627 minus deleted (3 in `GlobalMediaPlayerTest`, 1 in `MediaPlayerAuthenticationTest`, 2 in `BookPlaybackTest`) plus new; lint reports no new issues. If lint flags unused imports, remove exactly those it names.

- [ ] **Step 8: Commit** - message: `The player screen drives the playback service` with body: Back stops, Home keeps playing; GlobalMediaPlayer and the compat media session are gone; sign-out stops playback first.

---

### Task 10: Device check

No code. Requires the human.

- [ ] **Step 1: Ask the maintainer which book to use.** The check writes real listening progress to their account. Do not pick one, and never write progress back by hand. Ask for: one multi-file book (three or more files is best) and one podcast episode they are happy to have progress on. Before starting, check they are not using the device (`adb shell dumpsys activity activities | rg mResumedActivity`).

- [ ] **Step 2: Build and install the debug APK on the Android TV emulator**: `./gradlew :app:assembleDebug`, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Check, reading progress from the server (`GET /api/me/progress/<itemId>`), not from the screen:**
  - the book resumes at its saved position when that is past the first file;
  - skip forward over a file boundary (or seek to ~10 s before the end of file 1 and let it play): audio continues into file 2; the times row keeps counting in book time;
  - a chapter past the first file: scrubbing there plays there;
  - pause: within a second or two the server's `currentTime` matches the screen;
  - Home: audio keeps playing; the remote's play/pause pauses and resumes it; reopening the same book reattaches without jumping;
  - Back: audio stops; the server holds the position at Back;
  - the episode: plays, pauses, Back stops; progress lands on the episode.
  - If the Task 7 service test fell back, this is where the controller connection is proven - say so in the report.

- [ ] **Step 4: Repeat on the Fire TV stick**, through the adb proxy, the same list.

- [ ] **Step 5: Report** results per line, with server readings, in the PR description.
