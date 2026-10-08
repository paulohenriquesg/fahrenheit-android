# Home reloads its shelves on return and on playback (#197) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Start a book, come back to Home, and Continue Listening shows it. Home fetches `/personalized` again when it is returned to, and when the server's idea of what is in progress has just changed (playback started, stopped, or moved to another item), without disturbing the screen.

**Architecture:**
- `PlaybackReporting` gains `reported: (QueuedFile) -> Unit`: told once per stretch of listening when its **first** report reaches the server (the moment the server counts the item as started), and when the **closing** report reaches it (a stop, or leaving for the next item). The service wires it to a process-wide `ListeningNews`, a `StateFlow<Int>` that counts these.
- `HomeReload` (plain class, `main/`) fetches the shelves for a library after a short coalescing window: requests inside one window make one fetch. A failed fetch (null) or one for a library no longer current changes nothing.
- `HomeReloadTriggers` (composable) asks for a reload on ON_RESUME after the activity was stopped (not on the first resume, which the start-up load covers) and on each change of `ListeningNews`. Collected as state: no loop in composition.
- `MainHandler.fetchPersonalizedView` returns null on failure instead of an empty list, so a reload can tell "failed" from "empty". The start-up and library-switch loads keep showing empty on failure.

## Rulings

1. **Window: 500 ms**, trailing: the first request starts it, the fetch runs at its end. A request after the fetch started starts a new window - it may carry news the running fetch predates.
2. **Only while Home is the view.** Other views do not show the shelves, and choosing Home fetches them.
3. **The rail's Home row goes through `HomeReload` too**, so its failure also keeps what is shown.
4. **No loading state for a reload:** the old shelves stay until the new ones arrive; the shelf and card keys already keep focus on the same item.

## Tasks

1. **Red/green `PlaybackReporting.reported`:** not told before the first report is delivered; told once at the first delivered report, not again on later rounds; told when the closing report is delivered; a new stretch after a pause is told again.
2. **Red/green `HomeReload`:** a request fetches after the window; two requests inside it make one fetch; a failed fetch leaves the shown shelves alone; a result for a library no longer current is dropped; a request after a fetch began makes another.
3. **Red/green `HomeReloadTriggers`:** the first resume does nothing; stop then resume asks once; a `ListeningNews` change asks once.
4. **Red/green focus:** `PersonalizedHomeView` with focus on a card keeps it on that card when the shelves are replaced by a list that still has it (a new book added ahead of it).
5. **Wire up** in `MainScreen` and `PlaybackService`; `MainHandler` returns null on failure.
6. **Gate, review, PR** (Closes #197).

## Device check (the coordinator)

- Start a book that is not in Continue Listening, go back to Home: within a few seconds it is there.
- On Home, start playback from the rail's Now playing entry (or the remote's play key) of something not in Continue Listening: it appears after about five seconds, without leaving Home.
- With focus on a card in a lower shelf, return from the player: focus is still on that card, and the shelves never blank out.
- Turn the network off, return to Home: the shelves stay as they were.
