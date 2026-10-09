# Now playing goes when playback ends (#179) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** When the service's player reaches the end of its queue (`STATE_ENDED`: the newest episode with nothing after it, the end of a book), it does what Stop does: the listening session closes at the end, the queue empties, and the rail's Now playing entry and the book and podcast screens' bar go. The entry's badge never shows bars for an ended or idle player.

**Architecture:**
- A `QueueEnd` listener in the service calls `Playback.end` on the guarded session player when a batch of events leaves it `STATE_ENDED`. In `onEvents`, so the reporting listener has already closed the session at the end position; the guard's second close is a no-op (`ProgressReporter.finish` closes once). Auto-advance never reaches `STATE_ENDED` (Media3 moves to the next item), so it is untouched.
- `RailEntry` gets `playing` from `RailEntry.showsPlaying(playWhenReady, state)`: true only while play is wanted and the player is ready or buffering.
- Focus: an entry or bar that goes by itself while it holds focus stays composed until focus has moved out - the rail's `onStopped` (the section), the bar's `moveFocus(Down)` - then goes. Moving focus first is what Stop already does (#53).

## Tasks

1. **Red, then green - the end of the queue** (`QueueEndTest`, reporting + guard + `QueueEnd` on a test player):
   - an episode played to its end: the queue is empty, one closing report, at the end;
   - an episode with the next one queued: it moves on, the queue keeps the next, nothing ends.
   - Wire `QueueEnd` into `PlaybackService`.
2. **Red, then green - the badge** (`RailEntryTest`: `showsPlaying` for ready/buffering wanted, paused, ended, idle; `RailEntrySourceTest`: a test player at its end gives `playing = false`).
3. **Red, then green - focus** (`NowPlayingSlotTest`: focus on Stop, the queue cleared from elsewhere → entry gone, `onStopped` once; `NowPlayingBarSlotTest`: the same → bar gone, the screen's content focused).
4. **Docs:** the style guide's Now playing section says the entry goes at the end.
5. **Gate, review, PR** (Closes #179).

## Device check (the coordinator)

- **Newest episode, auto-advance on or off, played to its end** (seek to the last seconds): the rail's entry goes; `dumpsys media_session` shows no queue; the episode reads as finished on the server and in its podcast's list.
- **The rail open with focus on Stop as it ends:** focus lands on the section; the remote still moves.
- **On a podcast's screen with focus on the bar's Stop as it ends:** the bar goes, focus moves into the screen.
- **An older episode with auto-advance on:** at its end the next one plays and the entry shows it.
- **The last seconds of a book:** the entry goes; the book reads as finished.
- **Seeking while playing:** the badge keeps its bars (buffering is not a pause).
- **The player screen open as it ends:** the queue empties under it - note what it shows (not changed here).
