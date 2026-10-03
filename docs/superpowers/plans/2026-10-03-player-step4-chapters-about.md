# Player step 4: Chapters and About - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The last two action chips (#107; spec step 3).
- **Chapters** opens the book's chapters in the side panel, with start times and the current one marked; choosing one plays from its start. The list is one component, reusable by the details screen (#105).
- **About** opens a wide panel (frame "A, with About open"):
  - the description, which takes focus and scrolls with Down;
  - the series as small covers with this book marked, where choosing another asks "Play <title> instead?";
  - the facts (narrator, publisher, year, length, genres), one line each;
  - **Mark finished** / Mark unfinished.
  For an episode, About is the publish date and the show notes.
- The series line reads **"Series · Book N of M"** once the series is known.

**Architecture:**
- **Pure:** `SeriesBooks` (the series' books in order, this one's place, the total), `AboutFacts` (the fact lines), `NowPlaying` gains what About needs: the series reference, the library id, the facts and whether the book is finished.
- **API:** `LibraryApi.getLibraryItems` takes a `filter`. `LibraryRepository.seriesBooks(libraryId, seriesId)`. `LibraryApi.markFinished` for a book and an episode.
- **Mark finished** (`FinishMark`): pause, then `PATCH` `isFinished` together with the paused position.
- **Screen:** `ChapterList` and `ChaptersPanel`; `AboutPanel`, with a confirm step for switching books; `FullDescription` from the details screen, shared rather than copied. The chips are Chapters (only with chapters), Speed, Sleep, About; for an episode, Go to podcast, Speed, Sleep, About.

**Tech Stack:** Kotlin, Compose TV Material, Retrofit + Gson, JUnit4 + Robolectric + MockWebServer.

**Spec:** `docs/superpowers/specs/2026-10-03-player-revamp-design.md` (§Panels; §Data); mocks `docs/mocks/player.html`, frames "A, with About open" and "C…".

## Global Constraints

- Branch `feat/player-chapters-about`, stacked on `feat/player-speed-sleep` (#120).
- Test-first; Gradle exit codes captured (zsh: `pipefail`); commits via `git commit -F`, with no trailer; imports pruned by the compiler or lint.
- MockWebServer: always `takeRequest(5, SECONDS)`.
- TV components only for focusable controls.
- The public repo uses invented titles only.

## Findings from the server's code (they shape the design)

1. **The series endpoint (`GET /api/libraries/:id/series/:seriesId`) lists its books as ids only** (`progress.libraryItemIds`, with `include=progress`). "Play <title> instead?" needs titles. **`GET /api/libraries/:id/items?filter=series.<base64 id>&sort=sequence`** returns the same books in series order, with titles, and its `total` is M. One call. The id is base64-encoded and then URL-encoded, as the web client does.
2. **Once an item is finished, any later progress update that changes `currentTime` un-finishes it**, and `isFinished: false` resets `currentTime` to 0. A playing book keeps syncing, so marking it finished while it plays would undo itself within seconds. Mark finished therefore:
   - pauses first, which sends the closing report;
   - sends `isFinished: true` with the **same paused position** as `currentTime`.
   Whichever reaches the server first, the other changes nothing.

## Rulings (to confirm with the maintainer in the PR)

1. **After Mark finished the player stays open, paused.** Playing on is listening again, and the server un-finishes the book by itself.
2. **Mark unfinished** keeps the player where it is. The server resets its saved position to 0, and the next report sets it to where the listener is.
3. **Switching to another book in the series** stops this one, then opens the player on the other, playing from its saved position.
4. **Chapter start times** use the player's spoken lengths ("1 h 5 min"), like every other time on the screen.

## Review Focus

1. **Mark finished while playing stays finished.** Pinned in Task 3 (`marking finished pauses first, and sends the paused position`).
2. **The series request encodes the id as the server decodes it.** Pinned in Task 2.
3. **A book with no series, a series of one, or a series that fails to load:** no series row, and the line keeps "Book N". Pinned in Tasks 2 and 4.
4. **Focus:** Chapters lands on the current chapter; About lands on the description, which Down scrolls; Back from the confirm step returns to the series row, not out of the panel. Pinned in Tasks 1 and 4.
5. **A long chapter list** opens scrolled to the current chapter. Pinned in Task 1.

---

### Task 1: `ChapterList` and the Chapters panel

**Files:** create `player/ChapterList.kt` (`ChapterList`, `ChaptersPanel`, `ChaptersChip`); `PlayerPanel` gains `Chapters`. Test `ChapterListTest.kt`.

- [ ] Red: each chapter shows its title and start; the current one is marked and focused; choosing one reports its start and closes the panel; with 60 chapters and the 50th current, the 50th is displayed and focused; a chapter without a title reads "Chapter N".
- [ ] Green, commit: "A chapter list, and the player's Chapters panel (#107, #105)".

### Task 2: The series: request, model, "Book N of M"

**Files:** modify `api/LibraryApi.kt` and `api/LibraryRepository.kt`; create `player/SeriesBooks.kt`; modify `player/NowPlaying.kt`. Tests `SeriesBooksTest.kt` (MockWebServer and pure), `NowPlayingTest.kt`.

- [ ] Red:
  - the request is `/api/libraries/l1/items?filter=series.<urlencoded base64>&sort=sequence`;
  - books come in server order, with this one's place and the total;
  - a failed request is a failure, not an empty series;
  - the kicker reads "The Long Way · Book 2 of 3" with a total, "· Book 2" without one, and stays as it is for a book with no number.
- [ ] Green, commit: "Know the rest of the series, and say Book N of M (#107)".

### Task 3: Mark finished

**Files:** modify `api/LibraryApi.kt`; create `player/FinishMark.kt`. Test `FinishMarkTest.kt` (MockWebServer + TestExoPlayer).

- [ ] Red:
  - `marking finished pauses first, and sends the paused position`: the body is `{"isFinished":true,"currentTime":P}` to `/api/me/progress/b1`, and the player has stopped playing;
  - an episode goes to `/api/me/progress/p1/e1`;
  - unfinished sends `{"isFinished":false}` and leaves the player alone;
  - a failed request is reported as a failure.
- [ ] Green, commit: "Mark a book finished without the next report undoing it (#107)".

### Task 4: The About panel

**Files:** create `player/AboutPanel.kt` (`AboutPanel`, `AboutChip`, `AboutFacts`); make `detail/DetailHeader.kt`'s `FullDescription` internal and shareable; `PlayerPanel` gains `About`; `SidePanel` takes a width. Test `AboutPanelTest.kt`.

- [ ] Red:
  - focus lands on the description, and Down scrolls a long one;
  - the facts read one line each, with missing ones left out;
  - the series row shows each book, with this one marked;
  - choosing another asks "Play <title> instead?": Play calls back, Cancel or Back returns to the row;
  - Mark finished, or Mark unfinished when finished, calls back;
  - an episode shows its date and notes, with no series and no Mark finished.
- [ ] Green, commit: "The About panel: description, series, facts, Mark finished (#107)".

### Task 5: Wired into the player

**Files:** modify `player/PlayerActivity.kt` and `player/NowPlaying.kt` (facts, finished, series reference). Tests: `NowPlayingTest.kt`; activity logic in plain functions (the Robolectric trap).

- [ ] Red:
  - `NowPlaying.of` carries the facts, the finished flag, the series reference and the library id;
  - `PlayerActivity.switchTo` (plain function) stops playback before opening the other book.
- [ ] Green:
  - the chips;
  - the series is loaded with the item, and the kicker updates;
  - Chapters seeks;
  - About's actions call `FinishMark` and switch books.
- [ ] Gate, commit: "Chapters and About in the player (#107)".

### Task 6: The details screen's chapter list (#105)

If the coordinator confirms it belongs in this step: the book details screen shows `ChapterList`, and choosing a chapter opens the player playing from that chapter's start. Otherwise it is listed as deferred.

### Task 7: Review, fixes, PR

- [ ] A fresh review (opus, `code-reviewer.md`); fix Critical/Important and cheap minors test-first; run the gate; push; `gh pr create --base feat/player-speed-sleep`.

### Device check (the coordinator)

- **Chapters:**
  - opens on the current chapter;
  - choosing one plays from its start;
  - a long list scrolls.
- **About:**
  - the description takes focus and Down scrolls it;
  - the series covers, with this book marked;
  - choosing another asks first, and Play switches books;
  - the facts lines.
- **Mark finished:**
  - with the book playing, then wait 30 s;
  - the server still shows it finished;
  - Mark unfinished works.
- **The series line** reads "Book N of M".
- **An episode:**
  - About shows the date and notes;
  - there is no Chapters chip.
