# The book, podcast and author pages' own loading frame (#225) - Plan

**Goal:** the book and podcast pages stop opening to "Loading…" on black, and
the author page to a lone spinner (mock: `docs/mocks/detail-loading.html`).
Each draws its own layout from the first frame with what the opening card
showed; the rest is still grey shapes (`PlaceholderShape`). Stats too (#226):
grey boxes in the board's shape.

## Pieces

1. **`PlayerPreview`, reused** (#223's type: title, kicker, cover URL, byline).
   New builders: `page(LibraryItem)` (the item's own title - a podcast's name,
   not its newest episode - the author for a book, the cover unless the server
   says there is none), `of(SeriesBook)`, `show(podcastId, title)` ("Go to
   podcast"), `author(Author)` (name, and the photo's URL when it has one).
   `authorPhotoUrlOf(id)` beside `coverUrlOf`, used by `AuthorImage` too, so
   both screens ask with one URL.
2. **Intents.** `DetailActivity.createIntent(context, itemId, preview, podcast)`
   and `DetailActivity.previewOf / podcastOf(intent)`; `intentFor(context,
   LibraryItem)` for every card. `AuthorDetailActivity.createIntent(context,
   authorId, preview)` and `previewOf`.
3. **`BookLoading` / `PodcastLoading`** (detail): each page's left column with
   the preview's cover (or a grey square), title (or grey lines) and byline,
   grey pills for the buttons; on the right a grey description and the
   series row (book) or the note, tabs and episode rows (podcast).
   `DetailStage(item, failed, …)` is the switch, top-level so it is testable
   without the Activity.
4. **`AuthorLoading`**: `DetailHeader`'s place for the photo and name, grey
   lines for the count and bio, "Books" over a grid of grey cards.
5. **`StatsPlaceholder`**: five grey tiles, two panels, the sessions panel.
6. **HouseStyleTest**: DetailActivity and Stats off the allow-list.
7. **Entry points**: Home shelves (Covers, Episodes without an episode,
   Authors), Library grid and row (also See all), the series/collection screen,
   the book page's series, Search (items and authors), the author page's
   books, the Authors grid, the player's "Go to podcast". Latest Episodes opens
   the player, not these pages; Narrators is "coming soon".

## Tests first (each watched red)

- `DetailPreviewTest`: intents round-trip preview and kind; no preview -> null;
  builders; author intent round trip.
- `DetailLoadingTest`: book / podcast frames show the preview's title, no
  focusable node, no "Loading…"; no preview -> placeholders only; title in the
  same place loading and ready; `DetailStage` lands focus on Resume when ready.
- `AuthorLoadingTest`, `StatsPlaceholderTest` likewise.
- Entry points: `HomeShelfPressTest` the detail intent's preview; the player's
  podcast intent.

## Out of scope

A wash on the detail pages (they have none today); the author page's layout
beyond its loading frame.
