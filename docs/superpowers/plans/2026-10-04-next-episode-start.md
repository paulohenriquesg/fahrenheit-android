# The next episode starts at its saved place, with no blip (#171)

With "Play the next episode automatically" on, the next episode played about a
second from 0:00 before jumping to where it was left: it is queued as a plain
item, ExoPlayer starts it at its window's default position (0), and
`ResumeOnArrival` seeks only after `onMediaItemTransition`, once audio has
rendered.

## Approach

Neither fix the issue lists, but the one underneath both: ExoPlayer starts an
item it moves on to at its window's *default position*. A `MediaSource.Factory`
wrapper (`StartWhereLeft`) gives each queued file whose `QueuedFile.startAt` is
set a timeline whose window default position is that place within the file
(clamped to the file's length, so an episode resumed in a later file passes
straight through the earlier ones). Nothing is clipped: positions, reports and
listening time read as before, and seeking back before the saved place still
works. No pause at the end, no seek after arrival. `ResumeOnArrival` goes.

## Steps (each test first)

1. **StartWhereLeftTest**, a real ExoPlayer over fake hour-long files wrapped in
   `StartWhereLeft`, no screen, no `ResumeOnArrival`:
   - at the automatic move the first position of the next episode is its saved
     place (never 0 when one is saved), and no seek follows;
   - an episode never started starts at 0:00;
   - an episode resumed in its second file starts there;
   - seeking back before the saved place still reaches it (nothing clipped);
   - with `PlaybackReporting`: the finished episode closes at its end, the next
     reports from its saved place, and its listening time is what was played.
2. **StartWhereLeft** in `player/`; **PlaybackService** wraps its
   `DefaultMediaSourceFactory` in it and drops `ResumeOnArrival` (and its test).

## Device check (in the PR)

Setting on, an episode near its end with the next one saved part-way: the next
starts exactly at its saved place, no second of its beginning heard; the server
shows the finished one finished and the next from its saved place.
