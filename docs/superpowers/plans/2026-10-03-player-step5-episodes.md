# Player step 5: Episodes (#108) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** An episode says what it is, and moves on (frame "C, playing an episode"; spec §Episodes, §Moving on when an episode ends).
- **Under the title:**
  - a details line: a badge for a non-regular episode (Bonus, Trailer), then "Season S · Episode E · <length>", with missing parts left out;
  - the notes: the feed's subtitle, or the plain-text start of the description, three lines at most.
- **The outer transport buttons are previous and next episode,** by `publishedAt` among the episodes the server has audio for. Next on the newest, and Previous on the oldest, are disabled.
- **"Up next · <title> · <length>"** under the transport names what Next plays. It is hidden when there is none.
- **A setting, "Play the next episode automatically", off by default.** When on, the next newer episode is queued after the current one. Media3 moves on without a gap; its progress and session belong to it (per-item reporting already does that); and the screen follows it.

**Architecture:**
- **Pure:**
  - `EpisodeNeighbours`: older and newer by `publishedAt`, among episodes with an audio track;
  - `EpisodeDetails`: the badge, the line and the notes;
  - `NowPlaying` for an episode gains `details`, `badge`, `notes`, `previous` and `next` (an `EpisodeRef`: id, title, length).
- **Queue:** `PlaybackQueue.of` takes an optional `next: NowPlaying`, whose items follow the current episode's, each carrying its own `QueuedFile`.
- **Transport:**
  - `MediaPlayerController` takes `onPreviousItem` / `onNextItem`, which take the outer slots when there are no chapters, and are drawn disabled when null;
  - `PlayerScreen` draws the details line, the notes and Up next.
- **Moving between episodes:** `PlayerActivity.switchTo` takes an `episodeId`. With auto-advance, the screen keeps the episode it shows in step with the controller's current `QueuedFile` (`onMediaItemTransition`), so a moved-on queue redraws as the new episode, with its own next queued.
- **Setting:** `PlayerSettings` (its own `SharedPreferences`, like `SpeedMemory`), and one row in `SettingsView`. It is kept out of `UserPreferences`, which another lane is changing.

## Global Constraints

- Branch `feat/player-episodes`, stacked on `feat/details-chapters-finished` (the #105 PR).
- Test-first; no Gradle run until the coordinator frees the machine (2026-10-03). Tests are written first and run red once it is free.
- Public repo: invented titles in fixtures.

## Rulings (to confirm with the maintainer)

1. **"Bonus" and "Trailer"** come from `episodeType` (`bonus`, `trailer`), capitalised. `full`, or none, has no badge.
2. **Previous and Next open that episode at its saved position,** as choosing it from the list does. With the screen open, they replace the current episode in the queue.
3. **Auto-advance queues one episode ahead.** The screen, while open, queues the next one when it follows the move. With the screen closed (Home), playback stops after that one episode. Queuing a whole run of episodes without a screen to show them is left for later.
4. **An episode moved on to starts from its saved position,** or from the start when it has none or is finished.

## Review Focus

1. **Episodes with no audio are skipped** by Previous, Next and Up next. Pinned in `EpisodeNeighboursTest`.
2. **Ties in `publishedAt`** order by the server's `index`, so Next is never itself. Pinned in `EpisodeNeighboursTest`.
3. **After moving on,** reports go to the new episode (`PlaybackReportingTest` already pins per-item reporting), and the screen shows it.
4. **A book is unchanged:** chapter skip on the outer buttons, and no Up next.

### Task 1: `EpisodeNeighbours` and `EpisodeDetails` (pure)
### Task 2: `NowPlaying` for an episode: details, notes, previous, next
### Task 3: Transport and screen: previous/next episode, details line, notes, Up next
### Task 4: Moving to another episode (`switchTo` with an episode)
### Task 5: The setting, and queuing the next episode; the screen follows the move
### Task 6: Gate, review, PR (`--base feat/details-chapters-finished`)

### Device check (the coordinator)

- **An episode in the middle of a show:**
  - the details line, the badge where there is one, and three lines of notes;
  - Up next names the newer episode;
  - Previous and Next open those episodes.
- **The newest episode:** Next is disabled, and there is no Up next.
- **With the setting on:**
  - near the end of an episode, it moves on without a gap;
  - the screen shows the new one;
  - the server shows progress on the new episode.
- **With the setting off,** an episode stops at its end.
- **A book** still has chapter skip and no Up next.
