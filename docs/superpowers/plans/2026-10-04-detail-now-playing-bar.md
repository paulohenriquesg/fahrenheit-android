# Now playing bar on the book and podcast screens (#159)

Follow-up to #155: the details screen (book and podcast, one `DetailActivity`)
has no rail, so after Back from the player nothing there shows what plays or
stops it.

## Steps (each test first)

1. **NowPlayingBar(entry, onOpen, onStop)** in `main/`, next to the rail entry and
   sharing its parts (cover in a progress ring with the play state, the
   "Chapter · N min left" line). A focusable bar that opens the player, and Stop
   beside it. NowPlayingBarTest: title and time left, play state, bar opens,
   Stop stops and does not open.
2. **NowPlayingBarSlot(player, chaptersOf, onOpen)** reads `rememberRailEntry`;
   nothing when nothing is queued or there is no controller. Stop: focus moves
   down into the screen first (the focused button goes with the bar), then
   `Playback.end(player)`. NowPlayingBarSlotTest with a TestExoPlayer: nothing
   queued, no bar; queued, the bar; Stop empties the queue, the bar goes, focus
   is on the content below.
3. **DetailBody(isBook, nowPlaying)** draws the bar above the content, for a book
   and a podcast. **DetailActivity** holds a controller while visible
   (`ControllerSlot`, as MainActivity does) and passes the slot.

## Device checks (in the PR)

Play a book from its screen, Back: the bar shows, audio continues; open the player
from the bar; Stop from the bar: audio stops, the server shows the session
closed, focus lands in the screen. The same from a podcast's screen.
