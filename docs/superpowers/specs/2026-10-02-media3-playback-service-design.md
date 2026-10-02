# Media3 playback in a service (#16)

Status: approved design, 2 October 2026. Next step: implementation plan.

## The problem

A book made of several audio files plays only its first file. The player
screen hands Android's `MediaPlayer` the first track's URL and nothing else, so:

- playback stops when the first file ends;
- a saved position, or a chapter, past the end of the first file plays nothing;
- the progress bar is scaled to the whole book but follows only the first file.

Playback also lives inside the player screen: leaving it kills the audio, so
there can be no app-wide player or mini-player.

What already exists:

- Media3 1.11.1 (`media3-exoplayer`) is a dependency.
- `TrackTimeline` translates between whole-book time (what Audiobookshelf
  stores for positions, chapters and progress) and a file plus a position in it.
- `BookPlayback` uses the timeline to seek and read positions on a Media3
  `Player`. It is tested against a real ExoPlayer under Robolectric, but is not
  wired to the screen.

## Scope

In:

1. Play every file of a book back to back with ExoPlayer; resume, chapter
   jumps, the scrubber and progress sync all in whole-book time.
2. Move playback into a Media3 `MediaSessionService`, so it outlives the player
   screen.

Out, as follow-ups that build on this:

- Moving to the next podcast episode when one finishes. The queue built here
  carries each item's own ids, so it can hold episodes later; what "next" means
  (newer or older, skipping heard or not-downloaded ones) is a separate decision.
- A mini-player.
- Player frame 4 (time remaining, chapter in the times row). The transport keeps
  its current layout.
- Closing or syncing the episode `/play` session. It is opened exactly as today.

## Design

### Components

All in `player/` unless noted.

| unit | responsibility | depends on |
|---|---|---|
| `PlaybackQueue` (pure) | Builds the `MediaItem` list, start index and start offset (ms) from a `NowPlaying` and a start position in book time. Each item's `RequestMetadata` carries `mediaUri` and extras: `itemId`, `episodeId` (absent for a book), `startOffset` (seconds into the book), `bookTotal`. Title in `MediaMetadata`. Null when there is nothing to play. | `NowPlaying`, `TrackTimeline` |
| `QueuedFile` (pure) | Reads those extras back from a `MediaItem`; `bookTime(positionInFile) = startOffset + positionInFile`. | - |
| `PlaybackService` | A `MediaSessionService` owning the ExoPlayer and the `MediaSession`. Its session callback's `onAddMediaItems` rebuilds each item's URI from `RequestMetadata.mediaUri`. Runs one `ProgressReporter` while playing, reporting the current item's whole-book time to that item's own ids. | `QueuedFile`, `ProgressReporter`, `ApiClient` |
| `Playback` | The app's entry point to the service: `connect(context)` returns a `MediaController`; `stop(context)` connects if needed, stops and clears the queue. | media3-session |

Why the URI travels in `RequestMetadata`: Media3 strips `localConfiguration`
from items a controller sends to a session, and the default
`MediaSession.Callback.onAddMediaItems` fails any item without one
(`UnsupportedOperationException`). The service resolves them itself.

Changed:

- **`PlayerActivity`** connects a controller in `onStart` and releases it in
  `onStop`. It loads the queue, or reattaches (see Lifecycle). It drops
  `MediaSessionCompat`, its own progress-reporting job and `playing()`.
- **`MediaPlayerController`** keeps its layout but takes a `Player` (the
  controller) instead of using `GlobalMediaPlayer`. Seeks, skips and the scrubber
  go through `BookPlayback.seekToBookTime`; the shown time is
  `BookPlayback.bookPosition()`, polled once a second; play/pause state comes
  from a `Player.Listener`.
- **`BookPlayback`** loses `load` (bare `MediaItem.fromUri` items would be
  rejected through a controller; `PlaybackQueue` replaces it) and keeps its seek
  and position translation. Episodes use it too, as a one-track timeline, so the
  transport has a single path.
- **`ProgressReporter`** gains a final report when playback stops (see below).
  Its inputs come from the service: `position` from `QueuedFile.bookTime`,
  `total` from `bookTotal`. This also fixes it reporting a position within one
  file as a position in the book.
- **`ApiClient`** exposes its authenticated `OkHttpClient` for audio.
- **`MainScreen`** sign-out calls `Playback.stop` instead of
  `GlobalMediaPlayer.release()`, before the session is cleared.

Removed: `GlobalMediaPlayer`, and the `androidx.media` (compat) dependency if
nothing else uses it - established by lint and the build, not by text search.

Added dependencies: `media3-session`, `media3-datasource-okhttp`.

Manifest: the service, declared with `foregroundServiceType="mediaPlayback"`
and the `androidx.media3.session.MediaSessionService` intent filter;
permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
(targetSdk 34) and `WAKE_LOCK`.

### Data flow

1. The player screen fetches the item and its saved progress, then builds
   `NowPlaying` and the start point (`ResumePoint`), as today.
2. `PlaybackQueue` builds the items.
3. The controller calls `setMediaItems(items, index, offsetMs)`, `prepare()`,
   and `play()` if the screen was opened to play.
4. The service's `onAddMediaItems` makes them playable; ExoPlayer plays the
   files back to back.
5. On `onIsPlayingChanged`, the service starts or stops the reporter.
6. The reporter sends `startOffset + position` against the current item's ids.

### Audio over HTTP

ExoPlayer fetches audio through `OkHttpDataSource` built on the app's
authenticated client. That client already adds the current access token to
every request and refreshes it on a 401. Access tokens expire after an hour and
are replaced on every refresh, so a long book outlives the token it started
with; the token is read per request, never captured when the queue is built.
The client logs at BASIC in debug builds, which does not buffer bodies.

### Lifecycle

| situation | behaviour |
|---|---|
| Open the player, nothing playing | Load the queue at the `ResumePoint` position; play only when opened with `auto_play`. |
| Open the player for what is already playing (same `itemId` and `episodeId` in the current item's extras) | Reattach: no reload; the saved position and `auto_play` are ignored. |
| Open the player for something else while one plays | Replace the queue, after the final report for the previous item. |
| Back (the player screen is finishing) | `Playback.stop`: final report, stop, clear the queue, leave the foreground. With nothing queued the service stops itself. |
| Home, or another app on top | Release the controller only. The service keeps playing in the foreground; the remote's media keys and system controls reach its session. |
| Paused in the background | Media3 drops the foreground state; the system may stop the service. The position was already reported on pause. |
| App task removed | Media3's default: stop if paused, keep going if playing. |
| Sign-out | `Playback.stop` before the session is cleared. The final report may fail without a token; losing up to five seconds there is accepted. |

**Final report.** Today the reporter sends every five seconds and stops quietly
with playback, losing up to five seconds on every pause. Once Back stops
playback and a new item replaces the queue, that loss happens routinely. The
reporter sends once more when playback stops and before the queue is replaced,
under `ProgressSync`'s existing rules: an unmoved position is not sent, and 0 is
never sent before anything else has been.

Side effect: the screen reads the position from the player instead of receiving
a start position once, so "the transport takes its start position once" no
longer applies. Saved progress is still fetched before the queue is loaded.

### Error handling

| failure | handling |
|---|---|
| Access token expires mid-book | Refreshed and retried by the existing authenticator; invisible to the player. |
| Refresh fails, network gone, 404, undecodable file | ExoPlayer stops with a `PlaybackException`. The screen shows one line, "Couldn't play this", in place of the times row; Play retries with `prepare()` at the same book position. The final report is sent. |
| Short network blip | ExoPlayer's default load retries. |
| No tracks, or an episode without audio | `PlaybackQueue` returns null; the screen shows `item_load_failed`, as today. |
| Controller cannot connect | `item_load_failed`. |
| Progress send fails | Retried next round, as today. |

ExoPlayer settings:

- audio attributes with `AUDIO_CONTENT_TYPE_SPEECH` and audio focus handled,
  so another app taking audio pauses this one;
- `setHandleAudioBecomingNoisy(true)`;
- `WAKE_MODE_NETWORK`, so streaming continues when a TV's screensaver starts.

## Testing

Test-first throughout: each test is written and seen failing for the right
reason before the code it covers.

Unit (JVM and Robolectric):

- `PlaybackQueue`: a three-file book gives three items with the start index and
  offset from the timeline; extras carry the ids, `startOffset` and `bookTotal`;
  `mediaUri` is the resolved URL; an episode gives one item with its
  `episodeId`; nothing to play gives null.
- `QueuedFile`: `bookTime` and reading the extras back.
- Reattach rule, a pure `matches(current, itemId, episodeId)`: the same book
  matches; another episode of the same podcast does not; a book does not match
  an episode with the same `itemId`.
- `ProgressReporter`: final report on stop; none when the position has not
  moved; no final 0 when nothing was sent before.
- Session callback: `onAddMediaItems` rebuilds playable items from
  `RequestMetadata`.
- `PlaybackService` under Robolectric: a real `MediaController` connected to the
  service, a two-file queue, a seek across the boundary, and the reported
  whole-book time checked. If Robolectric cannot host the session connection,
  that is reported, the service's parts are tested against a test ExoPlayer, and
  the connection is left to the device check - not silently dropped.
- Audio auth, against MockWebServer: an audio request answered 401 and then 200
  is retried carrying the refreshed token.
- `PlayerScreenTest`, `TransportFocusTest` and `BookPlaybackTest` updated to
  pass a `Player` instead of using `GlobalMediaPlayer`.

Gate: the full unit suite and lint, exit codes captured.

Device, on an Android TV emulator and then a Fire TV stick:

- a multi-file book plays across the file boundary; resumes past the first
  file; a chapter past the first file jumps there;
- Home keeps playing; Back stops; the remote's play/pause works in both;
- progress is checked by reading it back from the server
  (`GET /api/me/progress/:id`), not from the screen.

The device check writes real listening progress. The book used for it is chosen
by the maintainer, not by whoever runs the check, and no progress is written
back by hand.
