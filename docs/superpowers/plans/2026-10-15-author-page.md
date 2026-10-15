# The author page in the book page's layout, and a Narrators page (#230) - Plan

**Goal:** the author page gets the book page's layout with the side menu kept,
its books grouped by series and told apart, and the Narrators section becomes
a grid whose narrators open the same page (mock: `docs/mocks/author-page.html`).

## Where it lives

`AuthorDetailActivity` stays its own activity, so Back leaves it for the screen
that opened it, as Back leaves the book page (`DetailActivity`), with focus
where it was there. It now draws the main screen's `NavigationRail`, with
Authors or Narrators highlighted. Choosing a section returns to the main
screen on that section: `MainActivity.sectionIntent(context, id)` (clear top,
single top), read in `onCreate` and `onNewIntent` and applied by `MainScreen`
as a rail choice.

## Server calls (read in the server's source)

- Author: `GET /api/authors/:id?include=items,series`, the server's own author
  page call: `libraryItems` (minified, no sequence) and `series`, each with its
  items and their sequence, sorted by it.
- Narrators: `GET /api/libraries/:id/narrators` -> `{ narrators: [{ id, name,
  numBooks }] }`. The server keeps no photos for them.
- A narrator's books: `GET /api/libraries/:id/items?filter=narrators.<base64
  name>` (expanded: `metadata.series` with sequence, and `authorName`).

## Pieces

1. **`PersonShelves`** (pure): items and each item's series -> one shelf per
   series by name, books by sequence (numeric where it is a number), then
   "Other books" by title. A book in two series is in both.
2. **`PersonPage`** model: name, photo URL, bio, count line, shelves, and
   `continueOf(items, progress)`: the most recently played book in progress
   from the shared store (#207).
3. **Card line**: `#2 · 14 h 40 min` (`listeningLength`), or `#2 · An Author`
   on a narrator's page; the subtitle as a tag.
4. **Layout** (`PersonStage`): the left column (`BookColumn`'s width) a round
   photo or initials, the name, "N books · M series" / "Reads N books", and
   Continue (filled, as Resume). The right column scrolls: About and the bio
   box clipped with "More ›" (OK opens the full bio in the book page's
   description box; Back closes it), then one row per shelf.
5. **Loading frame**: the same layout, the card's name and photo, grey shapes
   for the rest, nothing focusable (#225).
6. **Narrators**: `MainView.NARRATORS`, `NarratorsBrowseView` (grid like
   Authors, round initials), the "coming soon" string goes.

## Tests first (each watched red)

- `PersonShelvesTest`: grouping, sequence order, Other books, two editions
  apart, Continue chosen only from books in progress.
- `PersonPageTest` (Compose): rows and headings, tag and length, Continue
  present/absent and its press, bio box opens in full on OK and closes on
  Back, no bio no box, the narrator's page shows the author on its cards.
- `AuthorLoadingTest` still holds; a narrator frame test.
- `AuthorRailTest`: the rail is on the page with the section highlighted, and
  choosing a section starts the main screen on it; Back finishes.
- `NarratorsBrowseViewTest`: the grid lists narrators; pressing one opens the
  page for them. `MainViewTest`: Narrators is a view.
