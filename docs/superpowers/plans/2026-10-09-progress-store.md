# One shared progress store (#207) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Listening progress lives in one place. The player and Mark finished write it, the server is read into it on coming back to the app, and Home, Latest Episodes, the book page and the podcast page draw from it - so a finished episode shows finished on Back, with no screen fetching progress itself.

**Architecture:**
- `progress/ProgressStore` (process-wide, `ProgressStore.process`, like `ServerKnowledge.process`): `MediaProgressResponse` by `ProgressKey(itemId, episodeId)`, as a `StateFlow<Map<…>>`. Writes:
  - `replace(all, since)` - a whole read of `GET /api/me`;
  - `read(progress, since)` - one item's progress as the server sent it (the book page's item, the player's after a stop);
  - `played(itemId, episodeId, position, duration)` - a report of the player's reached the server;
  - `marked(itemId, episodeId, mark)` - Mark finished / unfinished reached the server;
  - `clear()` - sign-out.
  A read is answered late: `since` (the store's `generation` when the read began) keeps any entry written here after it, so a slow `/api/me` cannot put back a position from before a report.
- `progress/ProgressResync`: one `GET /api/me` at a time into the store; a failure or signed out leaves it as it is. Asked when the app comes to the foreground (the Application's activity counts) and when Home is created (it is where a sign-in lands).
- The playback service writes: each delivered report (`played`); after a stretch's closing report, that item's progress read from the server (`read`) - whether it is now finished is the server's rule (a library setting), not the app's; each mark it sends (`marked`).
- #208: `main/HomeViewModel(store)` holds Home's progress as `HomeUiState(covers, episodes)`; `PersonalizedHomeView` and `LatestEpisodesView` take it as values. The rest of Home's state moves when it is next touched (#197's rework).

## Rulings

1. **The server's word on finished.** A played report un-finishes locally only when the position moved (the server's own rule); whether the end finishes an item is read back from the server after the stop.
2. **No fetch on each screen's resume.** The resumes that read `/api/me` (Home's covers, Latest Episodes, the podcast page) and the book page's re-read of its item go; the store already holds what the player and the marks wrote. The app coming back to the foreground re-reads, for listening elsewhere.
3. **The podcast page still reads `/api/me` once** for the user's type (the admin feed check), not for progress. Optimistic marks settle on each change of the store.
4. **The player's own resume check** (`ResumeSources`, `PlayerActivity.savedProgress`) is left as it is: it asks the server on purpose, before playing.

## Tasks

1. Red/green `ProgressStore`: a played update is seen by a reader; Mark finished flips isFinished for that key only; a resync replaces stale entries (and drops ones the server no longer has); episode and book keys don't collide; a late read keeps what was written after it began; a played report on a finished item un-finishes it only when the position moved; un-finish puts the position to 0 (the server's rule), a position mark sets it.
2. Red/green `ProgressResync`: fills the store; one read at a time; a failure keeps the store.
3. Red/green service writes: `PlaybackReporting` tells `closed(file)` once a stretch's closing report is done; `FinishMarker`'s sends land in the store (wired in the service).
4. Red/green `HomeViewModel`: covers and episode progress follow the store.
5. Red/green screens: `PersonalizedHomeView(progress = …)`, `LatestEpisodesView(heard = …)`, the book page's facts from the store, the podcast rows from the store.
6. Red/green a source check naming the old call sites: no progress fetch left in `MainScreen`, `MainHandler`, `LatestEpisodesView`, `DetailActivity`.
7. Gate, review, PR (Closes #207).

## Device check (the coordinator)

- Finish an episode in the player, go Back: the podcast row and Home show it finished without a refresh.
- Play a book for a minute, go Back to its page: the Resume time and the progress fact moved; Back to Home: its cover's time left moved.
- Mark a book finished on its page: "Finished" at once; Home's Continue Listening cover loses its bar.
- Listen on another device, then bring the app back to the front: Home's covers show that position.
