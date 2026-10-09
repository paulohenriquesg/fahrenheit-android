# Favourites on the show screen (#180, part 2)

Part 1 (#189) gave the setting, the player's heart and the Home shelf. This
adds frame 2 of `docs/mocks/podcast-actions.html`: the heart on the focused
episode row, beside Mark finished (#181), and a **Favourites** tab. Both only
while the library has a Favourites playlist chosen.

## Rules

- **The tab**: "Favourites · N" after the other tabs, N being this show's
  episodes in the playlist. A user who is not an admin has no tabs today; with
  a playlist chosen they get "All" and "Favourites". With None there is no tab,
  and a screen left on it shows All.
- **The tab keeps its rows while open**: an episode taken out while on the tab
  stays listed, its heart empty, until the tab is chosen again, so focus never
  falls off a row that vanished. Its count follows at once.
- **The row heart**: a round button after Mark finished, the same size and
  style, on the focused row only and only for an episode on the server (a
  playlist holds what the server has). Filled while in; read as "Favourite" or
  "Remove from <playlist>". A press confirms with the same note as the player,
  and a failure leaves it as it was.
- **One holder per library** (`LibraryFavourites`): the playlist as last read,
  which episodes of a show are in it, and a toggle for any item. The player's
  `FavouriteHeart` becomes a view of it for one item.
- Read again on every return to the screen, as the progress is, so a heart
  changed in the player shows here.

## Easy to move (#205)

The row heart is its own composable (`EpisodeFavouriteButton`), fed only
`filled`, the playlist's name and a click. The row takes the hearts as one
optional value (`EpisodeHearts`), so a redesigned page passes the same thing.

## Steps, each test first

1. `LibraryFavourites`: episodes of a show in the playlist; toggling two
   episodes in turn; busy and failure as the player's heart. `FavouriteHeart`
   delegates; its tests stay green.
2. `PodcastScreenModel`: the Favourites tab and count, its rows, the
   non-admin case, falling back to All, and the rows kept while open.
3. `PodcastEpisodesView`: the heart after Mark finished on the focused row,
   Right reaches it, its descriptions, a press, none with None, none on a feed
   only row.
4. `DetailActivity` wiring: load on open and on each return, toggle with the
   note, the tab snapshot.
