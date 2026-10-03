# Details screen: Chapters and Mark finished (#105) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Frame 3 of `docs/mocks/screens.html`: a book's details screen has **Mark finished** and **Chapters** beside Resume.
- **Chapters** opens the same side panel as the player's, on the chapter the book was left in. Choosing one opens the player and plays from that chapter's start.
- **Mark finished** reads **Mark unfinished** once the book is finished.

**Architecture:**
- **`PlayerActivity.createIntent`** takes a `startAt`, read by `PlayerStart`:
  - a start position given by the screen that opened the player wins over the saved one;
  - if that book is already queued, the player seeks there and plays.
  It is honoured once, like `autoPlay`.
- **`FinishCommand`** names the item. The service marks the queued item as `FinishMarker` already does (pause, wait for the closing report, then `isFinished`). Any other item is marked directly. One path, so the details screen cannot race a book still playing behind it (the server un-finishes on a moved position; see step 4's plan).
- **`Playback.markFinished(context, itemId, finished, onDone)`**: a short-lived controller, as `Playback.stop` uses.
- **`BookDetailView`** takes the chapters, where the book was left, and the finished state. It draws both buttons, and hosts the Chapters panel with the player's `PlayerPanelHost` and `ChaptersPanel`.
- **`DetailActivity`** gains `playChapterIntent`, in the shape of #126's `playBookIntent` (`autoPlay = true`).

**Spec:** issue #105; `docs/superpowers/specs/2026-10-03-player-revamp-design.md` (Chapters panel: "the same component as the details screen's list").

## Global Constraints

- Branch `feat/details-chapters-finished`, stacked on `feat/player-chapters-about` (#130). It expects a small conflict with #126 in `DetailActivity`'s companion.
- Test-first; Gradle exit codes captured; commits via `git commit -F`, with no trailer.

## Rulings

1. **Focus still lands on Resume.** The chapter list is behind its button, as in the player.
2. **Chapters opens on the chapter of the saved position,** or the first chapter when the book is not started.
3. **Choosing a chapter plays at once** (autoPlay), as #126 made Play do.
4. **Mark unfinished on the details screen** loses the saved position. That is the server's rule when the book is not playing, and a finished book's place is its end anyway. When the book is the one queued, the listener's place is sent back as in the player.

## Review Focus

1. **A chapter chosen while the same book is already queued** (Home left it playing) seeks there rather than starting over at the saved position. Pinned in `PlayerStartTest`.
2. **Mark finished for a book that is not queued** does not pause whatever else is playing. Pinned in `FinishMarkerTest`.
3. **A book without chapters** has no Chapters button. Pinned in `DetailHeaderTest`.

### Task 1: Start at a position (`PlayerStart`, intent extra)
### Task 2: `FinishCommand` names the item; `Playback.markFinished`
### Task 3: `BookDetailView`: Mark finished, Chapters, the panel
### Task 4: `DetailActivity` wiring; gate; review; PR (`--base feat/player-chapters-about`)

### Device check (the coordinator)

- **Chapters on the details screen:**
  - it opens on the saved chapter;
  - choosing one opens the player playing from that chapter;
  - with the same book already playing (Home), choosing one jumps there.
- **Mark finished:**
  - the server shows the book finished;
  - Mark unfinished works;
  - with another book playing behind, that book keeps playing.
- **A book without chapters** has no Chapters button.
