# Player step 7: Now playing in the rail - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** The way back to the player (spec §Now playing in the rail; mocks: the two rail frames in `docs/mocks/player.html`; closes #107).
- **While something is queued in the playback service,** the side rail shows a **Now playing** entry above the sections, with a line under it.
  - **Closed:** the cover, a ring for how far through the book or episode, and the play state.
  - **Open:** the title, and "Chapter · M left" (or "M left" for an episode, or a book without chapters).
- **Choosing it** opens the player, which reattaches.
- **Nothing is shown** when nothing is queued.

**Architecture:**
- **Pure:** `RailEntry.of(file, title, positionInFile, playing, speed, spans)` gives the title, the progress through the book, the chapter (if any) and the time left. The time left counts at the speed, as the player's does.
- **`rememberRailEntry(player, chaptersOf)`:**
  - reads a `Player` (the main screen's `MediaController`): what is queued, its title (the queued item's metadata), its position and play state;
  - follows the player's events, and its position only while playing;
  - fetches the book's chapters once per item.
- **`NowPlayingEntry`:** the rail entry, closed or open, drawn by `NavigationRail` above its sections.
- **`MainActivity`** holds a `MediaController` while it is started (`ControllerSlot`, as the player does) and hands it to `MainScreen`. Choosing the entry opens `PlayerActivity` on what is queued.

## Global Constraints

- Branch `feat/rail-now-playing` from origin/main (a862311). Independent of the skip lengths PR.
- Test-first, each test watched red; gate; fresh review; one fix pass; the PR says "Closes #107".

## Rulings

1. **The entry is not a control.** Play/pause stays on the remote's own key, as the spec says.
2. **Focus on arrival stays on the section you are in;** the entry is one Up away.
3. **The ring** is the progress through the whole book (or episode). The subtitle is the chapter's time left.

## Review Focus

1. **Nothing queued, no entry** - and it goes when Back in the player clears the queue. Pinned in `RailEntrySourceTest`.
2. **The subtitle:** a chapter with its time left; an episode or a book without chapters with its own. Pinned in `RailEntryTest`.
3. **Choosing it opens the player on what is queued** (book, or episode). Pinned in `NowPlayingEntryTest`.

### Task 1: `RailEntry` (pure)
### Task 2: `rememberRailEntry` from a Player
### Task 3: `NowPlayingEntry` in the rail
### Task 4: `MainActivity` holds the controller; opening the player; gate; review; PR

### Device check (the coordinator)

- **Play a book, then Home.** The rail shows the cover, the ring and the play state.
  - Open the rail: the title and "Chapter · M left".
  - Centre: the player, still playing.
- **An episode:** "M left".
- **Back in the player stops playback:** the entry goes.
- **Play/pause on the remote:** the entry's state follows.
