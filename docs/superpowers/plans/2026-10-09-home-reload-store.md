# Home reloads its shelves on store events and on return (#197) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Start a book, come back to Home: Continue Listening shows it. Home fetches `/personalized` again when it is returned to, and when the shared progress store (#207) says listening started, stopped or finished - without disturbing the screen. A rework of #203 on top of the store and `HomeViewModel` (#208).

**Architecture:**
- `ProgressStore.news: StateFlow<Int>`, bumped when listening on an item starts (the first report of a stretch the server took - the server counts an item as started only then), stops (the service's read-back after a stretch closes, even when that read fails: `stopped(...)`), or a mark changes whether an item is finished. Not by a resync or a page's own read: those change no shelf.
- `HomeReload` (from #203): requests inside a 500 ms window make one fetch; the latest fetch wins; while Home is hidden a request is owed and made when it shows; a failure keeps the shelves and is said only for a load the viewer asked for that leaves Home empty.
- `HomeViewModel` takes Home's shelves (#208): `HomeUiState(shelves, loading, covers, episodes)`, and the events `open(library)` (first load, library switch), `choseHome()`, `returned()`, `visible(showing)`, `refreshFavourites(...)`. It listens to `store.news` itself; the composable only says when Home shows and when the screen came back. A failure to say is a `failures` flow the screen toasts.
- `MainHandler.fetchPersonalizedView` returns null on failure and toasts nothing: only the caller knows whether the viewer asked.

## Rulings

1. **Window 500 ms, trailing.** A request after a fetch began starts another window; the latest fetch's shelves win.
2. **Only while Home is the view and the screen is up.** Otherwise owed, and made when Home shows (rail, Back, or the return).
3. **The return reloads everything, Favourites included,** so the separate Favourites refresh on resume goes; the Favourites choice changed in Settings still refreshes only that shelf.
4. **No loading state for a reload:** old shelves stay until new ones arrive; shelf and card keys keep focus.
5. **A background reload fails silently;** the first load, a library switch, or choosing Home with nothing shown say so.

## Tasks

1. Red/green `ProgressStore.news`: a stretch's first report bumps it once, later reports do not; a stop bumps it (read-back or not) and the next report starts a new stretch; a mark that flips finished bumps it; a resync and a page's read do not.
2. Red/green `ProgressWrites.closed` goes through `stopped`.
3. Red/green `HomeReload` (ported tests).
4. Red/green `HomeViewModel` shelves: open loads and shows; a failed open says so; news while shown reloads after the window; news while hidden is owed until shown; a return reloads; a background failure keeps shelves silently; choosing Home with nothing shown says a failure; a switch drops the last library's shelves.
5. Red/green `MainHandler` null on failure, no toast.
6. Red/green focus kept across a reload (ported); the screen's failure toasts with the real `MainScreen` (ported).
7. Gate, review, PR (Closes #197).

## Device check (the coordinator)

- Start a book not in Continue Listening, go back to Home: within a few seconds it is there.
- On Home, start playback from the rail's Now playing entry of something not in Continue Listening: it appears after the first sync, without leaving Home.
- Finish a book with Mark finished on its page, Back to Home: it has left Continue Listening.
- Focus on a card in a lower shelf, return from the player: focus is still there, and the shelves never blank out.
- Network off, return to Home: the shelves stay, no toast. Network off and Home empty, choose Home in the menu: the toast.
