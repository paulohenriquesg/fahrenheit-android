# The playback service queues the next episode (#160)

Follow-up to #155: with "Play the next episode automatically" on, only the open
player screen queued the episode after next. Back now keeps playing with the
screen closed, so playback stopped one episode early.

## Steps (each test first)

1. **UpNext.after(item, episodeId, progress, resolveUrl)**: the next newer episode
   with audio (`EpisodeNeighbours`), as queue items starting at its saved position
   (`ResumePoint`, carried as `QueuedFile.startAt` for `ResumeOnArrival`); null at
   the newest, or for an episode the item no longer has. UpNextTest.
2. **NextEpisodeQueue(player, scope, enabled, nextOf)**, a `Player.Listener` in the
   service. On each automatic move to an episode: with the setting on, queue what
   `nextOf` gives behind it, unless another episode is queued there already
   (the screen's own queueing stays, and the two never double up: each checks
   and adds on the main thread). With it off, drop another episode queued
   behind. NextEpisodeQueueTest, a real ExoPlayer over fake hour-long files with
   no screen: two moves in a row each queue the following episode; setting off;
   already queued; a book.
3. **PlaybackService** adds it, with `PlayerSettings.playNextEpisode` read at each
   move and `nextOf` reading the item and the saved position from the server.

Reporting is untouched: each item reports as it plays, as today.

## Device check (in the PR)

Setting on, start an episode near its end, Back, wait through two moves: the
server shows progress on each episode, and the third starts where it was left.

## Review fix pass

- The screen (through its controller's lagging view) and the service could both
  queue the same next: the service drops a second copy behind what plays.
- The setting changing acts at once (`PlayerSettings.onPlayNextEpisodeChanged` ->
  `NextEpisodeQueue.settingChanged`), not one episode later.
- An unreadable saved position queues nothing (`ResumeSources.saved`), and an
  unreadable item is logged.
- Not done: a Next from the system's controls (a seek, not an automatic move)
  does not queue the one after; the service-level wiring is left to the device check.
