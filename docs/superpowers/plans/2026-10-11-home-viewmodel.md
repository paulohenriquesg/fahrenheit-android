# Main screen state into a ViewModel (#208) - Implementation Plan

**Goal:** `MainScreen` keeps no business state. Home's shelves are already in
`HomeViewModel`; what is left in `remember { mutableStateOf }` - the libraries
and their stats, the current library, the view and the highlighted rail row,
the refresh flag, series, collections and listening stats with their loading
flags - and the `LaunchedEffect`s that load them move into a ViewModel, as one
immutable `MainUiState` in a `StateFlow`, with events as methods.

No behaviour change: same screens, same loading and refresh moments, same focus.

## Boundary: a new `MainViewModel` for the shell

`HomeViewModel` stays what it is: the Home view's shelves, their reload rules
(#197) and the progress drawn on them from the shared store (#207). The shell -
which library, which view, the rail's highlight, the browse views' lists - is a
different concern, and folding it into `HomeViewModel` would rebuild the
one-big-file this issue is meant to end. So a new `MainViewModel` holds it.

The two meet at one point: opening a library (at start-up, and on a switch)
opens Home's shelves. `MainViewModel` takes that as a constructor function,
`openHome: (String) -> Unit` (wired to `HomeViewModel::open`), called at the
same moment as today, before the Library list opens. The shell shows no
progress itself (Latest Episodes keeps taking `HomeViewModel`'s), so the
progress store stays injected in `HomeViewModel` only.

Dependencies come through the constructor as suspend functions (libraries,
library stats, series, collections, listening stats, library items, the saved
library id), with a small factory building them from `ApiClient` and
`SharedPreferencesHandler`. Each returns null when there is no API, which is
today's "skip" when `ApiClient` gives none.

`LibraryList` moves into the ViewModel's scope and reports each change through
a callback, so its items, query and loading are part of `MainUiState`;
`LibraryListTest` stays as it is.

## What stays in the composable

Focus requesters, `railHasFocus`, the list's scroll state and the scroll to the
top, the lifecycle observer (it tells the models; it decides nothing), toasts,
sign-out and the activities it starts. Settings' own state (#208 later) is not
touched.

## Tasks

1. **Red**, `MainViewModelTest` (Robolectric, an unconfined scope, fakes):
   - initial load: the saved library is opened (Home and the Library list), and
     saved; a saved id no longer on the server falls back to the first;
   - a failed libraries load leaves nothing open and says so once;
   - library switch: following a newly saved library opens it, goes back to
     Home with Home highlighted, and drops the old Library list; following the
     same one does nothing and says so (so the caller reloads Home);
   - a switch saved before the libraries arrive is applied when they do;
   - Series, Collections and Stats: cleared and loading on choosing, filled
     and sorted after, and loading ends on a failed load with nothing shown;
   - Switch Library reads each library's stats when it opens, and a failed
     one is left out;
   - the highlight follows the row chosen, See all highlights the screen the
     tile opens.
2. **Green**: `MainViewModel`, `LibraryList` callback, `MainScreen` reads
   `MainUiState` and calls the model.
3. **Gate, review, PR** (Closes #208).

## Device check (the coordinator)

Focus is what is most likely to regress:

- Cold start: focus lands on Home in the rail; RIGHT reaches the first shelf.
- Rail to Library, Series, Collections, Stats: each loads, focus lands on that
  row in the rail, RIGHT reaches the first card / tile.
- A Home "See all" tile: the rail highlights the screen it opens.
- Switch Library: tiles show their counts; choosing another library returns to
  Home with Home highlighted, and its shelves.
- Back from a card -> rail row -> Home -> out of the app.
- Back from the player: Home's shelves reload, focus stays where it was.
