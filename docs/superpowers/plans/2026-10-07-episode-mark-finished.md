# Finished episodes, and Mark finished on a row (#181) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** On a podcast's screen, finished episodes are dimmed with a tick and say
"finished" in their line, and the focused row offers a round **Mark finished** /
**Mark unfinished** button one Right away. Marking shows at once and goes back,
with the app's usual message, if the server refuses. Mock: `podcast-actions.html`, frame 2.

**Architecture:**
- **`EpisodeMarks`** (pure): the screen's progress with the marks made here laid
  over it - marked finished reads as finished, un-marked reads as not finished -
  and `keepAt`, where an un-finished episode should stay (the book's rule, from
  `placeToKeep`).
- **`EpisodeMarking`**: holds the marks made here; `mark` sets one at once, runs
  the send, and takes the mark back if the send says no.
- **`Playback.markFinished`** gains `episodeId`, so an episode goes through the
  same `FinishCommand` and `FinishMarker` as a book: the one playing is paused and
  its closing report goes first; any other is marked directly.
- **`PodcastEpisodesView`**: each row is the card plus a slot after it that holds
  the round button while the row or the button has focus. The slot keeps its
  width on every row, so focus moves nothing. Feed-only rows have no button.

## Rulings

1. **"Heard" becomes "finished"** on this screen, as the mock and the web app say
   it: the tick's description is "Finished" and the line ends "· finished"; the
   "Heard" chip goes. Latest Episodes' cards keep their own wording (not in #181).
2. **Dimmed unless focused.** A finished row is drawn at 55% until it, or its
   button, holds focus: a focused row has to be readable.
3. **The button is a TV `IconButton`**, 40dp, the transport buttons' look:
   inverts on focus with a 3dp primary border, no growth.
4. **The failure message is the book's** (`mark_finished_failed`).
5. **After a mark the server's progress is read again**, as the book reads itself
   again: the rows and the header's Resume follow it, and marks the server has
   answered give way to it (`settle`) - playing a finished episode un-finishes it
   there (review).
6. **A press on an episode whose mark is still out is ignored**, so two marks
   never race to the server (review).
7. **The button sits after the card, outside its border**, in a slot every row
   keeps; the mock draws it inside the row. The device check decides if it reads
   as part of the row.

## Tasks

1. **Red:** `EpisodeMarksTest` (overlay both ways, keepAt rules), `EpisodeMarkingTest`
   (shows at once, kept on success, restored on failure), `PodcastEpisodesViewTest`
   (finished reads "finished" with the tick; Right from a focused row reaches Mark
   finished and Left returns; the button is only on the focused row; it reads Mark
   unfinished on a finished row; a press asks for the right mark; feed-only rows
   have none).
2. **Green:** the above, wired in `DetailActivity.PodcastEpisodes`.
3. **Gate, review, PR** (Closes #181).

## Device check (the coordinator)

- A podcast with a finished episode: dimmed, tick, "finished" in its line.
- Focus an episode, Right: the tick button; Center marks it, the row dims at once.
  Left goes back to the row; Up/Down still walk rows.
- On a finished episode the button says Mark unfinished; it un-dims.
- Mark the episode that is playing: it pauses, and stays finished after the next
  progress report (the closing report went first).
- With the server unreachable: the mark shows, then goes back with the message.
