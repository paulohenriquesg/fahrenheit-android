# Book screen and About: one layout (#134) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** The book screen and the player's About become one layout, built once: option 1 of `docs/mocks/book-screen.html` (approved).
- **Left column:** the cover, the title, author · narrator, then the actions.
  - On the book screen: Resume / Play, Mark finished, Chapters.
  - On About: Mark finished.
- **Right, top:** the description, in a wide box about ten lines tall. Down scrolls it.
- **Right, below, side by side:**
  - **the series:** "<series> · N books", as covers with titles, this book marked. The row scrolls sideways and opens with this book in view. Choosing another opens that book's screen from the book screen; from the player it asks "Play <title> instead?" (#130).
  - **the facts:** a two-column list (read by, publisher, published, length, genres, plus progress on the book screen).
- **About from the player replaces the player while open,** over the same cover wash. Back returns to the player.

**Architecture:**
- **`BookOverview`** (`ui/components/BookOverview.kt`): the shared layout. It takes the item id, title, byline, description, facts, the series and an `actions` slot. It hosts the series row, with the question when `askBeforeSwitching`, and the facts grid. `FullDescription` provides the description's focus and scrolling.
- **Pure:** `AboutFacts.book` gains a `Progress` fact for the book screen ("34% in", "Finished"). The series name for the label comes from the book's own series reference.
- **Book screen:**
  - `BookDetailView` renders `BookOverview`, with the #133 actions and the Chapters panel;
  - `DetailActivity` loads the series as the player does, and choosing a book opens its screen;
  - the chips of the old header give way to the facts list.
- **Player:**
  - `PlayerPanel.About` draws an `AboutScreen`: `BookOverview` over the wash in place of `PlayerScreen`'s content, with `BackHandler`;
  - the side-panel `AboutPanel` goes;
  - the wash becomes a `Modifier.coverWash(wash)` both use.
- **Podcast and episode screens are out of scope.** An episode's About keeps today's panel until they are designed.

## Global Constraints

- Branch `feat/book-overview`, stacked on `feat/details-chapters-finished` (#133).
- Test-first. Gradle runs only in the coordinator's slot; tests are written first and run red there.
- Public repo: the mock's real titles are replaced with invented ones.

## Rulings

1. **Focus on arrival:**
   - **the book screen:** Resume (#133, Ruling 1);
   - **About:** the description (#130);
   - **the description is reached from the actions with Right.**
2. **The series label** is "<series> · N books", N counting the books the app can play.
3. **The facts list keeps today's fact lines and labels,** laid out as label and value columns. The book screen adds progress, where the old header had a chip.
4. **An episode's About stays the side panel** until podcast screens are designed (out of scope).

## Review Focus

1. **Focus:** Resume first on the book screen and the description first in About; Right reaches the description; Down scrolls it; the series row and facts are reachable.
2. **The series row opens with this book in view,** even as book 30 of 38.
3. **Back from About returns to the player** with focus on the About chip, not out of the player.
4. **A book with no series, no description, or few facts** still lays out (no empty boxes).

### Task 1: `BookOverview` (layout, description, series row, facts grid)
### Task 2: The book screen on `BookOverview`; series loading; choosing a book
### Task 3: About from the player as a screen over the wash; the episode keeps the panel
### Task 4: Gate, review, PR (`--base feat/details-chapters-finished`)

### Device check (the coordinator)

- **The book screen:**
  - the layout matches option 1;
  - Resume is focused, and Right reaches the description;
  - the series shows "· 38 books", opens on this book, and choosing another opens it;
  - the facts include progress.
- **About from the player:**
  - opens full-screen over the wash, with the description focused;
  - the series asks before switching;
  - Back returns to the player.
- **A book in no series,** and a book with no description.
