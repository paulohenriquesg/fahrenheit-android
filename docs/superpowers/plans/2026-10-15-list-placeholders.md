# Placeholders while lists load (#224) - Implementation Plan

**Goal:** Home, Library, Series, Collections, Authors and Latest Episodes draw
still grey cards in the shape of what they are about to show, under their real
title, instead of a "Loading…" line on an empty screen. Mock:
`docs/mocks/list-placeholders.html`.

Narrators has no screen yet (the rail shows "coming soon"), so there is nothing
to convert there.

## One component: `ui/elements/Placeholders.kt`

- `PlaceholderShape(modifier, round)`: a box filled with
  `CoverPlaceholderTone.of(colorScheme)`, semantics cleared to a test tag
  (`PlaceholderTags.SHAPE`). Not focusable, no animation.
- Layouts built from it, each sized as the real card:
  - `ShelvesPlaceholder` - Home: two shelves, a heading bar over a row of
    200x300 cards (as `LibraryItemCard`).
  - `GridPlaceholder(look)` - the browse grids: `Covers` (Library, 200 min,
    200x300), `Groups` (Series, Collections, 200x260), `People` (Authors,
    180 min, 180x240 with a 120dp circle).
  - `RowsPlaceholder` - Latest Episodes: a grey heading bar and 120dp rows with
    an 88dp square.
- Lazy rows and grids with fixed keys and scrolling off, so only what fits is
  composed and the key rule holds.

## Screens

- `PersonalizedHomeView`: `isLoading && nothing to show` -> `ShelvesPlaceholder`.
- `SeriesBrowseView`, `CollectionsBrowseView`, `AuthorsBrowseView`:
  loading -> `GridPlaceholder`. Empty and failure unchanged.
- `LibraryBrowseView`: loading with no items -> `GridPlaceholder(Covers)`; the
  title's trailing text drops "Loading…" and shows no count while loading. In
  rows, a `CoverRowPlaceholder` padded as `LibraryItemsRow`.
- `LatestEpisodesView`: loading -> `RowsPlaceholder`.

## Rule and guide

- `HouseStyleTest`: a `R.string.loading*` reference outside the allow-list
  fails. Allow-list, each with the issue that converts it: `DetailActivity`
  (#225), `PlayerActivity` (#223, if not merged), and Stats' `loading_stats`
  (not in #224's list; flagged in the PR). An entry that no longer excuses
  anything fails too, so `AuthorDetailScreen` (a spinner, not a string; #225)
  is not on it.
- The rule's matcher is a function the test also feeds a bare-loading sample,
  so the rule is shown to fail.
- Delete `loading_series`, `loading_authors`, `loading_collections`,
  `loading_episodes`.
- `docs/ui-style-guide.md`: a `## Loading` section, and the two mocks in the
  Mocks table (`player-loading.html` only once #223 has merged).

## Tests first

Per screen: while loading, placeholders and the title show, no node is
focusable, the old text is gone; loaded data replaces the placeholders;
empty/failure unchanged. The shape's colour is the theme's placeholder tone.
