# Listening sessions replace the progress PATCH - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Report listening through Audiobookshelf playback sessions (open, sync, close), so listening in this app counts in stats and finishing marks finished, with the progress PATCH as a fallback (#92).

**Architecture:** `PlaybackReporting` keeps its rules but delivers through a per-item `ListeningDelivery`. The real one is `ListeningSession`, which opens a session lazily, syncs `{currentTime, timeListened, duration}`, reopens once on 404, and falls back to the PATCH when no session can be had. `ListeningTime` measures the seconds actually played, by the clock. `ProgressSync` also sends a round that moved nowhere but has listening time.

**Tech Stack:** Kotlin, Retrofit/OkHttp, Media3 1.11.1, JUnit4, Robolectric, MockWebServer.

**Spec:** `docs/superpowers/specs/2026-10-02-listening-sessions-design.md`

## Global Constraints

- Branch `feat/listening-sessions`, stacked on `feat/media3-playback-service` (#91).
- Test-first: each test is written and seen failing for the stated reason before the code it covers.
- Gradle runs capture the exit code; never pipe into `head`/`tail`. The shell is zsh.
- Commits carry no bot-attribution trailer; write the message to a file and use `git commit -F <file>`.
- The repo is public: fixtures use `abs.test`, made-up ids and titles.
- Imports are pruned by the compiler or lint, never by text search.
- Sessions open with `forceDirectPlay = true` and `mediaPlayer = "ExoPlayer"`.
- `deviceId` stays `"Fire Stick"`, unchanged, so the server's device records stay as they are. The other device fields become real values.

## Review Focus

1. **A stop arrives twice in a row** (the guard, then the player going quiet). Expected: one close, carrying the last report. Pinned in Task 3 (`stopping twice closes once`).
2. **The server restarted, or 36 hours passed, mid-book.** Expected: the next sync reopens a session and still saves the position. Pinned in Task 4 (`a session the server forgot is reopened once`).
3. **Seeking or skipping while playing.** Expected: listening time counts the seconds played, not the distance moved. Pinned in Task 5 (`listening time is what was played, not how far it moved`).
4. **The server answers 500 to opening a session.** Expected: the position is still saved through the PATCH. Pinned in Task 4 (`when no session can be opened, the position is still saved`).
5. **A short listen (under one 5-second round) then Back.** Expected: a session is opened and closed with that position and time. Pinned in Task 4 (`closing a session never opened opens it to close it`).

---

### Task 1: `ListeningReport`, and a round with listening time is sent

**Files:** create `player/ListeningReport.kt`; modify `player/ProgressSync.kt`; test `ProgressSyncTest.kt`.

**Interfaces:**
- Produces:
  - `data class ListeningReport(val currentTime: Double, val duration: Double, val timeListened: Double = 0.0)`
  - `ProgressSync.next(position: Double, total: Double, lastSent: Double?, listened: Double = 0.0): ListeningReport?`

- [ ] **Step 1: Failing tests** (append to `ProgressSyncTest`):

```kotlin
    // Listening stats need the time even when the position barely moved,
    // e.g. a round spent buffering (#92).
    @Test
    fun `a round with listening time but no movement is sent`() {
        val report = ProgressSync.next(position = 900.2, total = 3600.0, lastSent = 900.0, listened = 5.0)!!

        assertEquals(5.0, report.timeListened, 1e-9)
    }

    @Test
    fun `listening time travels with the report`() =
        assertEquals(5.0, ProgressSync.next(position = 930.0, total = 3600.0, lastSent = 900.0, listened = 5.0)!!.timeListened, 1e-9)

    @Test
    fun `under a second of listening, an unmoved round is still skipped`() =
        assertNull(ProgressSync.next(position = 900.2, total = 3600.0, lastSent = 900.0, listened = 0.5))
```

- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*ProgressSyncTest'`. It should fail to compile on `listened`.
- [ ] **Step 3: Implement.**

`ListeningReport.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

/**
 * Where playback is, in whole-book seconds, and how many seconds were
 * listened since the last report that was delivered.
 */
data class ListeningReport(val currentTime: Double, val duration: Double, val timeListened: Double = 0.0)
```

In `ProgressSync.next`:
- add the parameter `listened: Double = 0.0` and return `ListeningReport?`;
- replace the movement check with this, and update its KDoc to say that listening time alone is also worth a round:

```kotlin
        val moved = lastSent == null || kotlin.math.abs(reported - lastSent) >= MIN_MOVEMENT_SECONDS
        if (!moved && listened < MIN_MOVEMENT_SECONDS) return null

        return ListeningReport(currentTime = reported, duration = total, timeListened = listened.coerceAtLeast(0.0))
```

- In `ProgressReporter`, retype `send` to `suspend (ListeningReport) -> Unit` so it compiles. Tests touching `.currentTime!!` still compile (with a warning).
- In `PlaybackReporting`, retype `send` to `suspend (QueuedFile, ListeningReport) -> Unit`.
- In `PlaybackService.sendProgress`, take a `ListeningReport` and build `MediaProgressRequest(currentTime = report.currentTime, duration = report.duration)` from it.

- [ ] **Step 4:** Run `ProgressSyncTest`, `ProgressReporterTest` and `PlaybackReportingTest`. All should pass.
- [ ] **Step 5:** Commit: "Send a round that has listening time even if it barely moved".

### Task 2: `ListeningTime`

**Files:** create `player/ListeningTime.kt`; test `ListeningTimeTest.kt`.

**Interfaces:**
- Produces: `class ListeningTime(now: () -> Long)` with:
  - `fun playing(isPlaying: Boolean)`
  - `fun pending(): Double`, in seconds
  - `fun delivered(seconds: Double)`

- [ ] **Step 1: Failing tests:**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Seconds actually listened, by the clock: what stats count (#92). */
class ListeningTimeTest {
    private var ms = 0L
    private val time = ListeningTime { ms }

    @Test
    fun `time playing counts`() {
        time.playing(true); ms += 10_000
        assertEquals(10.0, time.pending(), 1e-9)
    }

    @Test
    fun `time paused does not`() {
        time.playing(true); ms += 10_000; time.playing(false); ms += 30_000
        assertEquals(10.0, time.pending(), 1e-9)
    }

    @Test
    fun `delivered seconds are spent, and the clock keeps counting`() {
        time.playing(true); ms += 10_000
        time.delivered(10.0); ms += 5_000
        assertEquals(5.0, time.pending(), 1e-9)
    }

    @Test
    fun `undelivered seconds roll into the next report`() {
        time.playing(true); ms += 10_000
        // A failed sync delivers nothing.
        ms += 5_000
        assertEquals(15.0, time.pending(), 1e-9)
    }

    @Test
    fun `playing twice in a row does not restart the count`() {
        time.playing(true); ms += 4_000; time.playing(true); ms += 1_000
        assertEquals(5.0, time.pending(), 1e-9)
    }
}
```

- [ ] **Step 2:** Run. It should fail to compile on `ListeningTime`.
- [ ] **Step 3: Implement:**

```kotlin
package com.paulohenriquesg.fahrenheit.player

/**
 * The seconds actually listened, by the clock while playing, since the last
 * report that was delivered. Not the distance the position moved: a seek is
 * not listening, and stats count listening (#92). Seconds from a report that
 * failed stay here and go with the next one.
 *
 * @param now a monotonic clock in milliseconds.
 */
class ListeningTime(private val now: () -> Long) {
    private var playingSince: Long? = null
    private var bankedMs = 0L

    fun playing(isPlaying: Boolean) {
        val t = now()
        if (isPlaying) {
            if (playingSince == null) playingSince = t
        } else {
            playingSince?.let { bankedMs += t - it }
            playingSince = null
        }
    }

    fun pending(): Double = (bankedMs + (playingSince?.let { now() - it } ?: 0L)) / 1000.0

    /** A report carrying [seconds] of listening was delivered. */
    fun delivered(seconds: Double) {
        val t = now()
        playingSince?.let {
            bankedMs += t - it
            playingSince = t
        }
        bankedMs = (bankedMs - (seconds * 1000).toLong()).coerceAtLeast(0L)
    }
}
```

- [ ] **Step 4:** Run. It should pass.
- [ ] **Step 5:** Commit: "Measure the seconds actually listened".

### Task 3: `ProgressReporter` carries listening time and closes

**Files:** modify `player/ProgressReporter.kt`; test `ProgressReporterTest.kt`.

**Interfaces:**
- Produces:

```kotlin
ProgressReporter(
    send: suspend (ListeningReport) -> Unit,
    position: () -> Double,
    total: () -> Double,
    pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) },
    listened: () -> Double = { 0.0 },
    delivered: (Double) -> Unit = {},
    close: suspend (ListeningReport?) -> Unit = { it?.let { r -> send(r) } }
)
```

- `finish()` calls `close(lastReportOrNull)` once per play stretch, and only if something played.

- [ ] **Step 1: Failing tests** (append):

```kotlin
    private class Session {
        val synced = mutableListOf<ListeningReport>()
        val closed = mutableListOf<ListeningReport?>()
        val delivered = mutableListOf<Double>()
        var at = 0.0
        var heard = 0.0
        var failNext = false
        var rounds = 0
        val reporter = ProgressReporter(
            send = { if (failNext) { failNext = false; error("offline") }; synced += it },
            position = { at },
            total = { 1000.0 },
            pause = { at += 10.0; heard += 10.0 },
            listened = { heard },
            delivered = { delivered += it; heard -= it },
            close = { closed += it }
        )
        suspend fun play(rounds: Int) { this.rounds = rounds; reporter.run { this.rounds-- > 0 } }
    }

    @Test
    fun `listening time goes with each report, and is spent once delivered`() = runBlocking {
        val s = Session()
        s.play(rounds = 2)
        assertEquals(listOf(10.0, 10.0), s.synced.map { it.timeListened })
        assertEquals(listOf(10.0, 10.0), s.delivered)
    }

    @Test
    fun `a failed report keeps its listening time for the next`() = runBlocking {
        val s = Session()
        s.failNext = true
        s.play(rounds = 2)
        assertEquals(listOf(20.0), s.synced.map { it.timeListened })
    }

    @Test
    fun `stopping closes with the last report`() = runBlocking {
        val s = Session()
        s.play(rounds = 1)
        s.at = 13.0; s.heard = 3.0
        s.reporter.finish()
        assertEquals(13.0, s.closed.single()!!.currentTime, 1e-9)
        assertEquals(3.0, s.closed.single()!!.timeListened, 1e-9)
    }

    @Test
    fun `stopping with nothing new still closes`() = runBlocking {
        val s = Session()
        s.play(rounds = 1)
        s.reporter.finish()
        assertEquals(listOf<ListeningReport?>(null), s.closed)
    }

    // Review Focus 1: the guard and the player both announce a stop.
    @Test
    fun `stopping twice closes once`() = runBlocking {
        val s = Session()
        s.play(rounds = 1)
        s.reporter.finish(); s.reporter.finish()
        assertEquals(1, s.closed.size)
    }

    @Test
    fun `playing again after a stop closes again at the next stop`() = runBlocking {
        val s = Session()
        s.play(rounds = 1); s.reporter.finish()
        s.play(rounds = 1); s.reporter.finish()
        assertEquals(2, s.closed.size)
    }

    @Test
    fun `stopping before anything played closes nothing`() = runBlocking {
        val s = Session()
        s.reporter.finish()
        assertEquals(emptyList<ListeningReport?>(), s.closed)
    }
```

- [ ] **Step 2:** Run. It should fail to compile on `listened`, `delivered` and `close`.
- [ ] **Step 3: Implement.** Add the three parameters. Then change the class body to:

```kotlin
    private var lastSent: Double? = null
    private var played = false

    /** Whether the stretch since the last play has been closed; a stop arrives twice. */
    private var closed = false

    suspend fun run(isPlaying: () -> Boolean) {
        while (isPlaying()) {
            played = true
            closed = false
            pause()
            val report = ProgressSync.next(position(), total(), lastSent, listened()) ?: continue
            // Only a delivered report counts as sent; a failed one is retried,
            // and its listening time goes with the next.
            if (runCatching { send(report) }.isSuccess) {
                lastSent = report.currentTime
                delivered(report.timeListened)
            }
        }
    }

    /**
     * Closes this stretch of listening: once, with whatever is new since the
     * last report. The position and time are read before anything suspends,
     * so a caller starting this undispatched captures them as they are now.
     * Not retried: there is no next round to retry in.
     */
    suspend fun finish() {
        if (!played || closed) return
        closed = true
        val report = ProgressSync.next(position(), total(), lastSent, listened())
        report?.let { lastSent = it.currentTime }
        if (runCatching { close(report) }.isSuccess) report?.let { delivered(it.timeListened) }
    }
```

- Also set `closed = false` inside `run` before the loop, so a run that never enters the loop changes nothing. Keeping it inside the loop, as above, is correct, because the loop is what marks a stretch as played.
- The existing tests must stay green. With the default `close`, `finish` sends its report through `send`, as before.

- [ ] **Step 4:** Run `ProgressReporterTest`. All tests should pass, old and new.
- [ ] **Step 5:** Commit: "Carry listening time in reports, and close each stretch once".

### Task 4: Session calls and `ListeningSession`

**Files:**
- Modify `api/ApiService.kt`.
- Create `api/SessionSyncRequest.kt`, `player/ListeningDelivery.kt` and `player/ListeningSession.kt`.
- Test `ListeningSessionTest.kt`.

**Interfaces:**
- `ApiService` additions:

```kotlin
    @POST("api/items/{libraryItemId}/play")
    fun playBook(@Path("libraryItemId") libraryItemId: String, @Body request: PlayLibraryItemRequest): Call<PlayLibraryItemResponse>

    @POST("api/session/{sessionId}/sync")
    fun syncSession(@Path("sessionId") sessionId: String, @Body body: SessionSyncRequest): Call<Void>

    @POST("api/session/{sessionId}/close")
    fun closeSession(@Path("sessionId") sessionId: String, @Body body: SessionSyncRequest): Call<Void>
```

- `data class SessionSyncRequest(val currentTime: Double? = null, val timeListened: Double? = null, val duration: Double? = null)`. Gson omits nulls, so an empty one is `{}`, which the server reads as "close without syncing".
- `interface ListeningDelivery { suspend fun sync(report: ListeningReport); suspend fun close(report: ListeningReport?) }`
- `class ListeningSession(file: QueuedFile, api: () -> ApiService?, device: PlayLibraryItemDeviceInfo) : ListeningDelivery`

- [ ] **Step 1: Failing tests:** `ListeningSessionTest`. Plain JUnit; it builds Retrofit against MockWebServer, as the existing `TokenRefreshIntegrationTest` does.

```kotlin
package com.paulohenriquesg.fahrenheit.player

import com.google.gson.JsonParser
import com.paulohenriquesg.fahrenheit.api.ApiService
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Listening reported the way Audiobookshelf's own clients do: a session (#92). */
class ListeningSessionTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ApiService
    private val device = PlayLibraryItemDeviceInfo("Fire Stick", "Fahrenheit", "test", "Amazon", "AFT", 25)
    private val report = ListeningReport(currentTime = 3605.0, duration = 7632.9, timeListened = 5.0)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun session(episodeId: String? = null) =
        ListeningSession(QueuedFile("b1", episodeId, 0.0, 7632.9), { api }, device)

    private fun ok(body: String = "") = MockResponse().setResponseCode(200).setBody(body)
    private fun opened(id: String) = ok("""{"id":"$id"}""")
    private fun json(body: String) = JsonParser.parseString(body).asJsonObject

    @Test
    fun `a book's session opens with direct play, then syncs`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        session().sync(report)
        val open = server.takeRequest()
        assertEquals("/api/items/b1/play", open.path)
        assertTrue(json(open.body.readUtf8()).get("forceDirectPlay").asBoolean)
        val sync = server.takeRequest()
        assertEquals("/api/session/s1/sync", sync.path)
        val body = json(sync.body.readUtf8())
        assertEquals(3605.0, body.get("currentTime").asDouble, 1e-9)
        assertEquals(5.0, body.get("timeListened").asDouble, 1e-9)
        assertEquals(7632.9, body.get("duration").asDouble, 1e-9)
    }

    @Test
    fun `an episode's session opens on the episode`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        session(episodeId = "e1").sync(report)
        assertEquals("/api/items/b1/play/e1", server.takeRequest().path)
    }

    @Test
    fun `later reports use the same session`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok()); server.enqueue(ok())
        val s = session()
        s.sync(report); s.sync(report)
        assertEquals(listOf("/api/items/b1/play", "/api/session/s1/sync", "/api/session/s1/sync"), List(3) { server.takeRequest().path })
    }

    // Review Focus 2.
    @Test
    fun `a session the server forgot is reopened once, and the report resent`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(opened("s2")); server.enqueue(ok())
        val s = session()
        s.sync(report); s.sync(report)
        assertEquals(
            listOf("/api/items/b1/play", "/api/session/s1/sync", "/api/session/s1/sync", "/api/items/b1/play", "/api/session/s2/sync"),
            List(5) { server.takeRequest().path }
        )
    }

    // Review Focus 4.
    @Test
    fun `when no session can be opened, the position is still saved`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500)); server.enqueue(ok())
        session().sync(report)
        server.takeRequest()
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("/api/me/progress/b1", patch.path)
        assertEquals(3605.0, json(patch.body.readUtf8()).get("currentTime").asDouble, 1e-9)
    }

    @Test
    fun `a rejected sync is not delivered`() {
        server.enqueue(opened("s1")); server.enqueue(MockResponse().setResponseCode(500))
        val failed = runCatching { runBlocking { session().sync(report) } }
        assertTrue(failed.isFailure)
    }

    @Test
    fun `closing sends the last report and ends the session`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok()); server.enqueue(ok())
        server.enqueue(opened("s2")); server.enqueue(ok())
        val s = session()
        s.sync(report); s.close(report); s.sync(report)
        val paths = List(5) { server.takeRequest().path }
        assertEquals("/api/session/s1/close", paths[2])
        assertEquals("/api/items/b1/play", paths[3])
    }

    @Test
    fun `closing with nothing new ends the session without moving the position`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok()); server.enqueue(ok())
        val s = session()
        s.sync(report); s.close(null)
        server.takeRequest(); server.takeRequest()
        val close = server.takeRequest()
        assertEquals("/api/session/s1/close", close.path)
        assertEquals("{}", close.body.readUtf8())
    }

    // Review Focus 5.
    @Test
    fun `closing a session never opened opens it to close it`() = runBlocking {
        server.enqueue(opened("s1")); server.enqueue(ok())
        session().close(report)
        assertEquals(listOf("/api/items/b1/play", "/api/session/s1/close"), List(2) { server.takeRequest().path })
    }

    @Test
    fun `closing a session never opened, with nothing to say, sends nothing`() = runBlocking {
        session().close(null)
        assertEquals(0, server.requestCount)
    }
}
```

- [ ] **Step 2:** Run. It should fail to compile on `ListeningSession`, `playBook` and `SessionSyncRequest`.
- [ ] **Step 3: Implement.**

`ListeningDelivery.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

/** Where one item's listening reports go. */
interface ListeningDelivery {
    /** Throws when the report could not be delivered, so it is retried. */
    suspend fun sync(report: ListeningReport)

    /** Ends this stretch of listening, with a last report if there is one. */
    suspend fun close(report: ListeningReport?)
}
```

`ListeningSession.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.ApiService
import com.paulohenriquesg.fahrenheit.api.MediaProgressRequest
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemRequest
import com.paulohenriquesg.fahrenheit.api.SessionSyncRequest
import retrofit2.awaitResponse

/**
 * One item's listening, reported as Audiobookshelf's own clients do: a
 * playback session, opened when there is first something to report, synced
 * with the position and the seconds listened, closed at a stop (#92).
 *
 * A sync saves the position too, so this is the one writer of it. The
 * progress PATCH is only a fallback, for when no session can be had: the
 * position is still saved, and only that round's listening time is lost.
 *
 * Sessions live in the server's memory, and a restart or 36 idle hours drops
 * them; a 404 opens a new one, once.
 *
 * @param api the current client; null when signed out.
 */
class ListeningSession(
    private val file: QueuedFile,
    private val api: () -> ApiService?,
    private val device: PlayLibraryItemDeviceInfo
) : ListeningDelivery {

    private var id: String? = null

    override suspend fun sync(report: ListeningReport) = deliver(report, closing = false)

    override suspend fun close(report: ListeningReport?) {
        if (report != null) return deliver(report, closing = true)
        val open = id ?: return
        id = null
        service().closeSession(open, SessionSyncRequest()).awaitResponse()
    }

    private suspend fun deliver(report: ListeningReport, closing: Boolean) {
        val body = SessionSyncRequest(report.currentTime, report.timeListened, report.duration)
        repeat(2) {
            val session = id ?: open() ?: return patch(report)
            val call = if (closing) service().closeSession(session, body) else service().syncSession(session, body)
            val response = call.awaitResponse()
            if (response.isSuccessful) {
                if (closing) id = null
                return
            }
            if (response.code() != 404) error("session rejected: ${response.code()}")
            // The server forgot the session (restart, or idle too long).
            id = null
        }
        patch(report)
    }

    /** The new session's id, or null when the server would not open one. */
    private suspend fun open(): String? {
        val request = PlayLibraryItemRequest(
            deviceInfo = device,
            // The app plays the files itself; without this the server may
            // set up a transcode nobody listens to.
            forceDirectPlay = true,
            supportedMimeTypes = SUPPORTED_MIME_TYPES,
            mediaPlayer = "ExoPlayer"
        )
        val call = file.episodeId?.let { service().playLibraryItem(file.itemId, it, request) }
            ?: service().playBook(file.itemId, request)
        val response = runCatching { call.awaitResponse() }.getOrNull() ?: return null
        return response.body()?.id.takeIf { response.isSuccessful }?.also { id = it }
    }

    private suspend fun patch(report: ListeningReport) {
        val request = MediaProgressRequest(currentTime = report.currentTime, duration = report.duration)
        val call = file.episodeId?.let { service().userCreateOrUpdateMediaProgress(file.itemId, it, request) }
            ?: service().userCreateOrUpdateMediaProgress(file.itemId, request)
        val response = call.awaitResponse()
        if (!response.isSuccessful) error("progress rejected: ${response.code()}")
    }

    private fun service(): ApiService = api() ?: error("signed out")

    private companion object {
        val SUPPORTED_MIME_TYPES = listOf(
            "audio/mpeg", "audio/mp4", "audio/x-m4b", "audio/aac", "audio/flac", "audio/ogg", "audio/opus", "audio/wav", "audio/webm"
        )
    }
}
```

`SessionSyncRequest.kt`:

```kotlin
package com.paulohenriquesg.fahrenheit.api

/**
 * A playback session sync. Nulls are left out, so an empty one is `{}`: the
 * server closes a session given that without syncing it.
 */
data class SessionSyncRequest(
    val currentTime: Double? = null,
    val timeListened: Double? = null,
    val duration: Double? = null
)
```

- [ ] **Step 4:** Run `ListeningSessionTest`. It should pass.
- [ ] **Step 5:** Commit: "Report listening through playback sessions".

### Task 5: `PlaybackReporting` delivers through sessions and measures listening

**Files:** modify `player/PlaybackReporting.kt`; test `PlaybackReportingTest.kt`.

**Interfaces:**
- Produces:

```kotlin
PlaybackReporting(
    player: Player,
    scope: CoroutineScope,
    open: (QueuedFile) -> ListeningDelivery,
    pause: suspend () -> Unit = { delay(ProgressSync.INTERVAL_MS) },
    now: () -> Long = { SystemClock.elapsedRealtime() }
)
```

- [ ] **Step 1: Failing tests.**
- Change `setUp` to record through a fake delivery. Keep `sent` as `MutableList<Pair<QueuedFile, Double>>`, recording `sync` reports and non-null `close` reports. The existing assertions then hold unchanged:

```kotlin
    private val reports = mutableListOf<Pair<QueuedFile, ListeningReport>>()

    private inner class Recorder(val file: QueuedFile) : ListeningDelivery {
        override suspend fun sync(report: ListeningReport) { reports += file to report; sent += file to report.currentTime }
        override suspend fun close(report: ListeningReport?) { report?.let { reports += file to it; sent += file to it.currentTime } }
    }
```

- Construct with `open = { Recorder(it) }` and `now = { player.clock.elapsedRealtime() }`. The `TestExoPlayer`'s fake clock is what the playback runs on.
- The test `the closing report survives the service going away` builds its own `PlaybackReporting`. Give it a delivery whose `close` awaits `network`, then records.
- Add:

```kotlin
    // Review Focus 3.
    @Test
    fun `listening time is what was played, not how far it moved`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)
        guarded.seekTo(1, 600_000)
        run(player).untilPositionAtLeast(602_000)

        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        val heard = reports.single().second.timeListened
        assertTrue("heard $heard s", heard in 4.0..6.5)
    }

    @Test
    fun `pausing closes, and playing on reports again`() {
        queue(nowPlaying("b1", twoParts), startAt = 3602.0)
        playUntil(5_000)
        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        guarded.play()
        run(player).untilPositionAtLeast(8_000)
        guarded.pause()
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(2, reports.size)
    }
```

- [ ] **Step 2:** Run. It should fail to compile on `open` and `now`.
- [ ] **Step 3: Implement.** In `PlaybackReporting`:
  - replace `send` with `open` and add `now`;
  - add `private var time: ListeningTime? = null`;
  - in `start()`, when a new reporter is made:

```kotlin
            reportingFor = current
            val delivery = open(current)
            val listening = ListeningTime(now).also { time = it }
            reporter = ProgressReporter(
                send = delivery::sync,
                position = { positionIn(current) },
                total = { current.bookTotal },
                pause = pause,
                listened = listening::pending,
                delivered = listening::delivered,
                close = delivery::close
            )
```

  - Then `time?.playing(true)` before launching rounds.
  - In `finish()`, call `time?.playing(false)` right after cancelling rounds and before launching the closing report.
  - Update the class KDoc: reports go to the item's `ListeningDelivery`, a playback session in the app.

- [ ] **Step 4:** Run `PlaybackReportingTest`. All tests should pass. If the listening-time range fails, print `heard` and the fake clock's advance before changing the range. The range must stay well under the 600 s the seek moved.
- [ ] **Step 5:** Commit: "Report each item through its session, with the time listened".

### Task 6: The service opens sessions; the screen stops

**Files:**
- Create `player/PlaybackDevice.kt`.
- Modify `player/PlaybackService.kt` and `player/PlayerActivity.kt`.
- Test `PlaybackDeviceTest.kt`.

- [ ] **Step 1: Failing test** (Robolectric):

```kotlin
@RunWith(AndroidJUnit4::class)
class PlaybackDeviceTest {
    @Test
    fun `the device says what it really is`() {
        val info = PlaybackDevice.info(ApplicationProvider.getApplicationContext())
        assertEquals("Fire Stick", info.deviceId)
        assertEquals(BuildConfig.VERSION_NAME, info.clientVersion)
        assertEquals(Build.VERSION.SDK_INT, info.sdkVersion)
        assertEquals(Build.MANUFACTURER, info.manufacturer)
    }
}
```

- [ ] **Step 2:** Run. It should fail to compile.
- [ ] **Step 3: Implement.**

```kotlin
package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.os.Build
import com.paulohenriquesg.fahrenheit.BuildConfig
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.PlayLibraryItemDeviceInfo

/**
 * How this device introduces itself when it opens a listening session. The
 * id is unchanged from before, so the server's device records stay as they
 * were; the rest used to be hard-coded.
 */
object PlaybackDevice {
    fun info(context: Context) = PlayLibraryItemDeviceInfo(
        deviceId = "Fire Stick",
        clientName = context.getString(R.string.app_name),
        clientVersion = BuildConfig.VERSION_NAME,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        sdkVersion = Build.VERSION.SDK_INT
    )
}
```

`PlaybackService`:
- the reporting line becomes `PlaybackReporting(exo, scope, open = { ListeningSession(it, ApiClient::getApiService, PlaybackDevice.info(this)) })`;
- delete `sendProgress` and its now-unused imports (`Log` and `TAG` only if unused; check with the compiler and lint).

`PlayerActivity`:
- delete `openSession` and its call (`if (episodeId != null) openSession(itemId, episodeId)`);
- delete the imports it alone used, confirmed by the compiler and lint.

- [ ] **Step 4: Gate:**

```
set -o pipefail; ./gradlew :app:testDebugUnitTest --rerun :app:lintDebug > log 2>&1; echo EXIT_CODE=$?
```

It should end `EXIT_CODE=0`, with 0 failures.

- [ ] **Step 5:** Commit: "Open listening sessions from the playback service".

### Task 7: Device check (Fire Stick)

- [ ] Use an item the maintainer allows. Record its progress and listening sessions first.
- [ ] Play about 40 s, pause, play about 20 s, then Back.
- [ ] Read the server:
  - `GET /api/me/listening-sessions` now lists sessions from this app, with roughly the time played (about 40 s and about 20 s);
  - `GET /api/me/progress/<id>` has the position at Back.
- [ ] Afterwards, reset the item's progress and delete the test sessions (`DELETE /api/sessions/:id`) if the maintainer asks.
