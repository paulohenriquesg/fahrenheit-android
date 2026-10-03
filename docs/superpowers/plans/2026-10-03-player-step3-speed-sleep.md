# Player step 3: Speed and Sleep - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Two action chips beside the transport, Speed and Sleep, each opening the same panel from the right (frame "A, with a panel open: Speed"). Speed offers 0.75× to 2× in seven steps and is remembered per book or show on the device; the book's "left" time counts at that speed. Sleep offers Off, End of chapter, 15, 30 and 60 min; it runs in the playback service, so it works with the screen closed, and the chip shows the minutes left (#107; spec step 2, the handover's step "Speed and Sleep").

**Architecture:**
- **Pure:** `ListeningSpeed` (the steps, their labels, time left at a speed). `SleepTimer` (what is left, when to pause).
- **Device storage:** `SpeedMemory`, a plain `SharedPreferences` file keyed by item id. A show's episodes share the show's item id, so "per book or show" falls out.
- **Service:** `SleepWatch` drives a `SleepTimer` from the player: it counts listening time, retargets the chapter end after a seek, pauses when the timer runs out, and publishes what is left as session extras. `PlaybackService` accepts a `SleepCommand` custom command from trusted controllers and ticks the watch while a timer runs.
- **Screen:** `SidePanel` (slides in from the right over a scrim, traps focus, Back closes it), `ActionChip`, `SpeedPanel`, `SleepPanel`. `PlayerActivity` holds which panel is open, sets the speed on the controller, and hears the sleep extras through a `MediaController.Listener`.

**Tech Stack:** Kotlin, Compose TV Material, Media3 1.11.1 session custom commands and session extras, JUnit4 + Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-03-player-revamp-design.md` (§Panels: Speed, Sleep; §Times); mocks `docs/mocks/player.html`, frames "A, with a panel open: Speed" and "C…" (chips).

## Global Constraints

- Branch `feat/player-speed-sleep`, stacked on `feat/player-cover-wash` (#111).
- Test-first; Gradle exit codes captured (zsh: `pipefail`); commits via `git commit -F`, no trailer; imports pruned by the compiler or lint.
- TV components only for focusable controls.
- Lengths via `PlaybackPosition.spoken`.
- **Not in this step:** the Chapters and About chips (step "Chapters and About"), skip lengths (step "Settings").

## Rulings (made here; listed in the PR for the maintainer)

1. **The sleep countdown counts listening time, not wall time.** A pause for tea does not eat the timer. This matches End of chapter, which also only moves while playing, so the chip's minutes mean the same thing in both modes.
2. **End of chapter follows the listener:** the target is the end of the chapter playback is in; a seek or chapter skip retargets it. Playing across a chapter end pauses. The screen sends the chapter ends in book time, so the service still knows nothing about books.
3. **For End of chapter the chip shows the minutes left in the chapter at the current speed**, like the minute modes.
4. **A new queue (another book, Back, sign-out) turns the timer off.** It was set for what was playing.
5. **Without chapters, End of chapter is not offered.**
6. **"Left in chapter" stays in book time** (spec §Times). Only the book's time left counts at the speed: "M left at 1.25×", or "M left" at 1×. Without chapters the single row reads "M left of T at 1.25×".
7. **Speed is applied by the screen** on every start and reattach (1× when nothing is remembered), since the service's player keeps the last book's speed.

## Review Focus

1. **The timer with the screen closed:** it lives in the service and pauses through the session player, so the closing progress report goes out as on any pause. Pinned in Task 3 (`fifteen minutes of listening pauses the player`).
2. **A seek past the chapter end** must not pause at once; it retargets. Pinned in Task 2 (`a seek retargets the chapter end`) and Task 3.
3. **Back closes a panel before it leaves the player.** Pinned in Task 4 (`back closes the panel, not the screen`).
4. **Focus while a panel is open** stays in the panel, lands on the chosen option, and returns to the chip on close. Pinned in Task 4.
5. **Only trusted controllers may set a timer.** Pinned in Task 3.

---

### Task 1: `ListeningSpeed` and `SpeedMemory`

**Files:** create `player/ListeningSpeed.kt`, `player/SpeedMemory.kt`; tests `ListeningSpeedTest.kt` (JVM), `SpeedMemoryTest.kt` (Robolectric).

**Interfaces:**
- `object ListeningSpeed { val STEPS: List<Float>; const val NORMAL = 1f; fun label(speed: Float): String; fun left(position: Double, total: Double, speed: Float): Double }`. Labels: `0.75×`, `1×`, `1.1×`, `1.25×`, `2×`.
- `class SpeedMemory(context: Context) { fun of(itemId: String): Float; fun remember(itemId: String, speed: Float) }`. Unknown or non-step values read as `NORMAL`.

- [ ] Red: the seven steps; labels; left at 1.25× is (total − position) ÷ 1.25, never below 0; memory per item, default 1×, a show's episodes share it, a corrupt value reads 1×.
- [ ] Green, commit: "Know the listening speeds, and remember one per book (#107)".

### Task 2: `SleepTimer`

**Files:** create `player/SleepTimer.kt`; test `SleepTimerTest.kt` (JVM).

**Interfaces:**
- `sealed interface SleepChoice { Off; EndOfChapter; Minutes(n) }`, `SleepChoice.OFFERED`.
- `class SleepTimer` with `minutes(n)`, `endOfChapter(ends: List<Double>, position)`, `off()`, `seeked(position)`, `listened(ms)`, `due(position): Boolean`, `secondsLeft(position, speed): Double?`, `choice: SleepChoice`.

- [ ] Red:
  - minutes count only what `listened` adds; due at zero;
  - end of chapter targets the end of the chapter containing the position (50 ms grace before a start, as `ChapterClock`), is due once the position reaches it;
  - `a seek retargets the chapter end`; after the last chapter end it is due;
  - seconds left at 1.5× is chapter left ÷ 1.5; Off leaves nothing.
- [ ] Green, commit: "A sleep timer that counts listening, or waits for the chapter's end (#107)".

### Task 3: The timer in the service

**Files:** create `player/SleepWatch.kt` (with `SleepCommand`: the custom command, its argument bundle, and the extras it reports); modify `PlaybackService.kt`, `Playback.kt`. Tests `SleepWatchTest.kt` (TestExoPlayer, fake media), `PlaybackServiceTest.kt`.

**Interfaces:**
- `SleepCommand.COMMAND: SessionCommand`; `SleepCommand.args(choice, chapterEnds)`; `SleepCommand.state(extras): SleepState?` with `choice` and `minutesLeft`.
- `class SleepWatch(player, now: () -> Long, publish: (Bundle) -> Unit) : Player.Listener` with `set(args: Bundle)`, `check()`, `beforeLeaving()`, `val running`.
- `Playback.connect(context, listener: MediaController.Listener? = null)`.

- [ ] Red (`SleepWatchTest`): `fifteen minutes of listening pauses the player`; a pause does not count; end of chapter pauses at the chapter end and a seek past it does not pause; extras report choice and whole minutes left, and clear when it fires or is turned off; a new queue turns it off.
- [ ] Red (`PlaybackServiceTest`): a controller sets 30 min and reads it back through `sessionExtras`/`onExtrasChanged`; an untrusted controller is not offered the command; the app is.
- [ ] Green: in `PlaybackService` the session callback adds `SleepCommand.COMMAND` for trusted controllers and handles it; a coroutine ticks `check()` every second (sooner near a chapter end) only while a timer runs; `beforeLeaving` also turns the timer off.
- [ ] Commit: "Run the sleep timer in the playback service (#107)".

### Task 4: The panel and the chips

**Files:** create `player/SidePanel.kt` (`SidePanel`, `ActionChip`, `PanelOption`), `player/ListeningPanels.kt` (`SpeedPanel`, `SleepPanel`, `SpeedChip`, `SleepChip`); strings. Test `ListeningPanelsTest.kt`.

- [ ] Red: the Speed chip reads "Speed 1.25×"; the Sleep chip reads "Sleep" or "Sleep 12 min"; opening Speed lands focus on the chosen speed; choosing one reports it and closes; `back closes the panel, not the screen`; focus returns to the chip; focus cannot leave the panel to the left; Sleep without chapters offers no End of chapter.
- [ ] Green, commit: "Speed and Sleep panels that slide in from the right (#107)".

### Task 5: Wired into the player

**Files:** modify `MediaPlayerController.kt` (a `speed` parameter for the book times), `PlayerScreen.kt` (an overlay slot for the panel), `PlayerActivity.kt`. Tests `TransportTest.kt`, `PlayerScreenTest.kt`.

- [ ] Red: `TransportTest` - at 1.25× the book row reads "M left at 1.25×", at 1× "M left"; without chapters "M left of T at 1.25×". `PlayerScreenTest` - an overlay draws over the screen.
- [ ] Green: `PlayerActivity` reads `SpeedMemory`, sets `connected.setPlaybackSpeed` once ready and on choice, connects with a listener that keeps the `SleepState`, and sends `SleepCommand` with the chapter ends. Trailing slot: Go to podcast (episode), Speed, Sleep.
- [ ] Gate, commit: "Speed and Sleep in the player (#107)".

### Task 6: Review, fixes, PR

- [ ] Fresh review (opus, `requesting-code-review/code-reviewer.md`); fix Critical/Important and cheap minors test-first; gate again; push; `gh pr create --base feat/player-cover-wash`.

### Device check (the coordinator)

- Speed: each step audibly applies; the book row reads "left at"; reopening the book restores it; another book starts at 1×.
- Sleep: 15 min chip counts down; End of chapter pauses at the chapter end; Home with a timer running still pauses; the server shows the paused position.
- Back closes a panel first; focus lands on the current option and returns to the chip.
