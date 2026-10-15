# The player's own loading frame (#223) - Plan

**Goal:** the player draws its own layout from the first frame instead of a
black screen with "Loading…" (mock: `docs/mocks/player-loading.html`, frames
1-3). The screen that opens it hands over what it already shows.

## Pieces

1. **`PlayerPreview(title, kicker?, coverUrl?)`** (player package): written to
   and read from intent extras. `PlayerActivity.createIntent` takes an optional
   `preview`; `PlayerActivity.previewOf(intent)` reads it (null when no title).
   Builders, one per kind of opener, so each entry point is one call:
   `book(itemId, title, author, hasCover)`, `episode(podcastId, title, show)`,
   `of(LibraryItem)` (Home cards: the title and author the card shows),
   `of(RailEntry)` (Now playing rail and bar).
   `coverUrl` is `coverUrlOf(itemId)`, the same function `CoverImage` uses for
   its default, so the URL - Coil's memory-cache key - is the one every screen
   requests.
2. **`PlayerLoading(itemId, preview, animated = true)`**: PlayerScreen's layout
   (60/32 dp padding, 250 dp cover, 36 dp gap, kicker, title, then the bar and
   the row where the buttons go). Preview: cover (or the placeholder with the
   title when it has none), kicker, title, two grey description lines. No
   preview: a ♪ cover placeholder and grey title lines. A thick track with a
   sweep instead of a fill, then a spinner and "Starting…". Nothing focusable.
   `animated = false` draws the sweep and the spinner still; tests use it.
3. **`PlayerStage(state, failed / loading / ready)`**: the `when` that was in
   `PlayerActivity.Player`, moved to a top-level composable so the switch from
   loading to ready is testable without building the Activity.
4. **Ready**: `PlayerScreen` fades the wash in from the background over 300 ms
   (`coverWash(wash, fadeIn = true)`); About keeps today's behaviour.
5. **Entry points**: Home book card, Home episode card, Home rail Now playing,
   book/podcast Play/Resume, a book's chapter, the book/podcast Now playing
   bar, Latest Episodes, `switchTo` (previous/next episode, a series book),
   the notification's Play (`MediaMetadata.title`, cover from the item id).

## Tests first (each watched red)

- `PlayerPreviewTest`: createIntent round-trips the preview with and without
  kicker / cover; no title, no preview; builders.
- `PlayerLoadingTest`: preview title and kicker shown, no focusable node, no
  Play; with no preview the placeholders, and not "Loading…".
- `PlayerStageTest`: failed shows the message; not ready shows the loading
  frame; ready replaces it with Play focused; an animated frame leaves nothing
  running once ready (the compose clock idles). Teardown drains the looper.
- Entry points: `HomeShelfPressTest` / `ContinueListeningPressTest` assert the
  preview's title; `PlayerIntentTest` the Detail and Latest Episodes intents
  and the rail entry; `PlayerActivityTest` `switchTo`; the notification's
  intent through an extracted function.
- Wash: fades in from the background on the ready screen (pixel at t=0 vs 300 ms).

## Out of scope

Anything after ready; the failed frame; the cover sizes of the opening screens.
