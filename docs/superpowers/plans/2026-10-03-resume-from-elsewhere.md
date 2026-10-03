# Ask which position to continue from - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** When the player still holds a book or episode and the server's position is newer and more than 30 seconds away, and was not written by this device, ask which to continue from (#90).

**Architecture:** One pure rule, `ResumeOffer.of`, decides whether to ask, from the player's position, the server's progress, when this player last knew the server's state, and the device behind the latest listening session. `ResumeCheck` gathers those inputs for one queued item. The Activity asks it in two places: when the screen reattaches to a queued item that is not playing, and when Play is pressed while paused (the screen's button and the remote's Play key while the screen shows). `ResumeChoice` is the question, drawn over the player.

**Tech Stack:** Kotlin, Retrofit, Media3 1.11.1, Compose for TV, JUnit4, Robolectric.

**Spec:** the approved design in the last comment of #90.

## Global Constraints

- Branch `feat/progress-mismatch-prompt` from `origin/main`. One PR, `Closes #90`.
- Test-first: each test is written and seen failing for the stated reason before the code it covers.
- Gradle runs capture the exit code; never pipe into `head`/`tail`. The shell is zsh.
- No bot-attribution trailer; commit messages go through a file and `git commit -F`.
- The repo is public: fixtures use made-up ids, `abs.test`, invented titles.
- Imports are pruned by the compiler or lint, never by text search.
- No real `PlayerActivity` in Robolectric (it leaves later Compose tests unable to go idle); test plain functions and composables.
- Keep the player files lane-player is changing touched as little as possible: the question is an overlay in `PlayerActivity`'s `Player`, and the transport gets one `onPlay` parameter.

## Decisions

- **Mismatch:** the server's `lastUpdate` is later than when this player last knew the server's state, and `|server - here| > 30 s`. Exactly 30 s does not ask.
- **"Last knew":** per queued item, the later of when the player queued it from the server's position (that progress's own `lastUpdate`, the server's clock) and when it last delivered a report (this device's clock). Nothing known (the queue predates this process) means any server position counts as newer.
- **Same device:** the latest listening session for the item (`GET api/me/item/listening-sessions/{itemId}[/{episodeId}]`, latest by `updatedAt`); its `deviceInfo.deviceId` equal to `PlaybackDevice`'s id means no question. Sessions unreadable: ask, since the server's copy is newer than anything this player knows.
- **Not asked:** an unreadable or never-started server position (#16 covers that), a server position marked finished, or no `lastUpdate`.
- **Choosing the server's position** seeks there (and plays, when the question came from Play). **Staying** dismisses; nothing is sent until playback moves, and the same server position is not asked about again (its `lastUpdate` becomes what this player knows).
- **Wording:** "You're at 1 h 05 min here. Continue from 1 h 20 min, listened to 10 minutes ago on another device?" Buttons "Continue from 1 h 20 min" (focused) and "Stay at 1 h 05 min". Times under an hour read "45 min".

## Review Focus

1. **The TV's clock and the server's disagree.** The report time is the TV's clock and `lastUpdate` the server's. TV ahead: a newer server position may look older, and nothing is asked - today's behaviour. TV behind: the device check still keeps this device's own listening from asking.
2. **Stay, then Play.** Must not ask again about the same server position. Pinned in Task 5.
3. **Playing in the background, screen comes back.** Not asked: the player's reports are the newest. Pinned in Task 5 (a playing player is not checked).
4. **The remote's Play key** goes to the media session when the screen does not take it; only presses while the player screen shows are checked.

---

### Task 1: `ResumeOffer.of` - the rule

**Files:** create `player/ResumeOffer.kt`; test `player/ResumeOfferTest.kt`.

- Produces `data class ResumeOffer(val here: Double, val there: Double, val listenedAt: Long)` and `ResumeOffer.of(here: Double, server: MediaProgressResponse?, knownAt: Long?, latestDevice: String?, thisDevice: String): ResumeOffer?`.
- [x] Tests: newer and 41 s away from another device asks; 30 s does not; 31 s does; not newer than known does not; nothing known counts as newer; latest session from this device does not; sessions unknown (null) asks; no server progress, no `currentTime`, no `lastUpdate`, or finished does not.
- [x] Implement; green.

### Task 2: the latest listening session's device

**Files:** modify `api/ApiService.kt` (two `@GET`s), create `api/ItemListeningSessions.kt`; test `api/ItemListeningSessionsTest.kt` (Gson parse of an invented reply, plus the pick).
- Produces `ItemListeningSessions.latestDeviceId(): String?`: the `deviceInfo.deviceId` of the session with the greatest `updatedAt`, null when none.
- [x] Tests: picks the latest by `updatedAt`, not the first listed; none gives null; a session without device info gives null.

### Task 3: what this player last knew

**Files:** create `player/ServerKnowledge.kt`; modify `player/PlaybackReporting.kt` (a delivered report records), `player/PlayerStart.kt` (queuing from the server's progress records its `lastUpdate`); tests `ServerKnowledgeTest.kt`, additions to `PlaybackReportingTest`.
- Produces a process-wide `ServerKnowledge` with `knownAt(itemId, episodeId): Long?`, `saw(itemId, episodeId, at: Long)` keeping the later time.
- [x] Tests: later wins, earlier is ignored, books and episodes are separate; a delivered report records; a failed one does not.

### Task 4: `ResumeChoice` - the question

**Files:** create `player/ResumeChoice.kt`, strings and plurals in `res/values/strings.xml`; test `player/ResumeChoiceTest.kt` (Compose + Robolectric).
- [x] Tests: the sentence with both times and "10 minutes ago"; "1 minute ago", "just now", hours, days; times under an hour; focus starts on the server's position; its button (centre key) chooses the server position; Stay chooses to stay; both are TV buttons.

### Task 5: `ResumeCheck`, and asking at the right moments

**Files:** create `player/ResumeCheck.kt`; modify `player/MediaPlayerController.kt` (`onPlay` parameter), `player/PlayerActivity.kt` (overlay, the two moments); test `player/ResumeCheckTest.kt`, `MediaPlayerController` transport test for `onPlay`.
- Produces `ResumeCheck(progress, latestDevice, thisDevice, knowledge).offer(itemId, episodeId, here, playing): ResumeOffer?`, and `answered(itemId, episodeId, offer)` recording the server's `lastUpdate` as known.
- After review: `ResumePrompt` holds the question's state and races (one check at a time, a result dropped once playback started, a 1.5 s cap on Play, the remote key's down and up paired), unit-tested apart from the Activity. The question traps focus and Back (Back stays); Play takes focus again after it.
- [x] Tests: a playing player is not checked; an offer comes from the rule's inputs; after Stay the same server position is not offered again; the transport's Play calls `onPlay` when paused and pauses directly when playing.
- [x] Wire: on reattach to a queued item that is paused, check and show `ResumeChoice`; Play (button or remote Play key on the screen) while paused checks first and plays after the choice.

### Task 6: gate, review, PR

- [x] `./gradlew :app:testDebugUnitTest :app:lintDebug`, exit code 0 (when given the Gradle slot).
- [x] Fresh review, one fix pass, PR `Closes #90` with what to check on the stick.
