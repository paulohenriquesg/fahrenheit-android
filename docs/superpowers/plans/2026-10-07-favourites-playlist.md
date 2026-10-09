# Favourites: a playlist chosen in Settings (#180)

Mock: `docs/mocks/podcast-actions.html`, frames 0, 1 and 4 in this PR; frame 2
(the row heart and the Favourites tab) sits on #181's row buttons and follows it.

## Server endpoints (read from the Audiobookshelf server source, not memory)

| what | call | answer |
|---|---|---|
| a library's playlists | `GET /api/libraries/:id/playlists` | `{results: [playlist], total, limit, page}`, the user's own only |
| one playlist | `GET /api/playlists/:id` | playlist; 404 when gone, 403 when not the user's |
| create | `POST /api/playlists` `{libraryId, name}` | playlist; `items` may be left out, so it can start empty |
| add | `POST /api/playlists/:id/batch/add` `{items: [{libraryItemId, episodeId?}]}` | playlist; an item already in is skipped |
| remove | `POST /api/playlists/:id/batch/remove` `{items: [...]}` | playlist; an item not in is skipped |

A playlist is `{id, name, libraryId, userId, items: [{libraryItemId, episodeId?, episode?, libraryItem}]}`
in playlist order. Batch add/remove, not `POST /:id/item`, because the web app uses them and they are
idempotent (the single add answers 400 for an item already in).

**The server deletes a playlist when its last item is removed.** Taking the last
favourite out would silently turn Favourites off, so the app makes an empty one
of the same name again and keeps it chosen.

## Steps, each test first

1. `PlaylistApi` + `Playlist` model: payload shape test from a server-shaped JSON.
2. `FavouritesChoice`: the chosen playlist id per library, on this device (prefs).
3. `Favourites` (repository + rules, fake API):
   - `current(libraryId)` is null with no choice; a 404 clears the choice (None);
   - `contains(item, episode)`; `toggle` adds or removes and returns the new state;
     a failure leaves the state as it was; emptying recreates and re-chooses;
   - `options(libraryId)`: None, Create "Favourites" unless one is named that, then
     playlists by name with counts.
4. Settings → Playback row "Favourites playlist · <library>" showing the choice;
   Center opens a `SidePanel`: None, Create, playlists; focus on the current choice;
   a search field above more than 12; Center picks and closes.
5. Player: a heart chip after Go to podcast / Chapters, only with a playlist chosen;
   filled when in; a Toast confirms; failure says so and leaves it.
6. Home: a shelf after Continue Listening named after the playlist, in its order,
   none when empty or with None; an episode plays, a book opens its screen.

Not here: the episode-row heart and the show's Favourites tab (after #181).
