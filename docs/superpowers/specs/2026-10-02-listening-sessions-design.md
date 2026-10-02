# Listening sessions replace the progress PATCH (#92)

Status: draft for review, 2 October 2026. Builds on the playback service of #91.

## The problem

Listening in this app never counts towards Audiobookshelf's listening stats.

- Stats are built from saved playback sessions and their `timeListening`.
- A session is saved only once it has listening time, and only a session sync adds time.
- Books never open a session. Episodes open one (`POST /api/items/:id/play/:episodeId`) that is never synced, so it is never saved.

On a live server, none of 200 recent saved sessions came from this app.

A session sync also saves the position (`currentTime`), the same thing the app's separate `PATCH /api/me/progress` does. Audiobookshelf's own clients use the sync alone.

## Decision

Listening sessions replace the PATCH. There is one writer of the position.

1. **Open** a session when something starts playing: `POST /api/items/:id/play` for a book, `/play/:episodeId` for an episode.
2. **Sync** it while playing (`POST /api/session/:id/sync`).
3. **Close** it when playback stops, the queue changes or the screen leaves (`POST /api/session/:id/close`, carrying a final sync).

The PATCH remains only as the fallback described below.

## What a listener gets

- **TV listening appears in stats**, alongside the other clients.
- **Finishing a book or episode on the TV marks it finished.** The server's sync applies the library's "mark as finished" thresholds; the PATCH never set `isFinished`.
- **Positions are saved exactly as today.** The sync reports the same whole-book `currentTime`.

## Design

### What changes

The change is to how a report is delivered. What is measured stays the same.

`PlaybackReporting` keeps every rule it has:
- one reporter per item
- a closing report before stop or replace
- nothing sent for an item never played
- whole-book time

Its `send` changes from a PATCH to a session sync. That goes through a new `ListeningSession`, one per queued item:

| step | when | call |
|---|---|---|
| open | first time the item plays | `POST /api/items/:id/play[/:episodeId]` with `forceDirectPlay = true`, the app's MIME types and `mediaPlayer = "ExoPlayer"` |
| sync | each reporting round | `POST /api/session/:id/sync` with `{currentTime, timeListened, duration}` |
| close | the closing report | `POST /api/session/:id/close` with the same body |

**`forceDirectPlay = true`.** Today the app asks with `forceDirectPlay = false` and no supported MIME types. The server may then set up a transcode stream the app never plays. The app plays the files directly, and the session must say so.

**`timeListened` is the seconds actually played since the last delivered sync.** It is measured by the clock while the player reports playing, not from position changes, so seeking does not count as listening. A failed sync keeps its seconds and adds them to the next one, so no listening time is lost.

**The position still follows `ProgressSync`'s rules.** An unmoved position is not sent, and 0 is never sent before anything else.

**A sync with time to add but a position that has not moved is still sent**, for example after buffering. Stats need the time. This is new: `ProgressSync` skipped such rounds. The rule becomes: send when the position moved **or** there is listening time to deliver.

### Losing the session

Sessions live in the server's memory. A server restart, or 36 hours without a sync, drops them, and the next sync then fails with 404.

On 404 the app:
1. opens a new session for the same item;
2. sends the pending sync to it once;
3. if that fails too, drops back to the PATCH for that round.

**When a session cannot be opened at all** (server error, or no connection on that one request), the round falls back to `PATCH /api/me/progress`. The position is still saved; only that round's stats are lost. The next round tries to open a session again.

### Episodes

The `openSession` call in `PlayerActivity` is removed. Sessions belong to the playback service, not the screen.

### Sign-out

As today, playback stops first. The closing sync usually goes out after the session is cleared and is lost. The server's 36-hour cleanup removes the session.

## Out of scope

- Transcoding (HLS) for formats the device cannot play.
- Local or offline sessions (`/api/session/local`).
- The "which position?" prompt (#90). It reads the server's position, and is unaffected by how that position is written.

## Testing

Each test is written first and watched fail.

- **`ListeningSession`** against MockWebServer:
  - open sends `forceDirectPlay = true` and returns the session id
  - sync and close bodies
  - 404 reopens once and resends
  - a failed open falls back to the PATCH
  - the bearer token goes with every call (the same client as today)
- **Listening time:**
  - only time while playing counts
  - a seek adds nothing
  - a failed sync's seconds roll into the next one
- **`PlaybackReporting`** with sessions:
  - a session opens on first play and not before
  - a closing report closes the session
  - replacing book A closes A's session, never B's
- **`ProgressSync`:** a round with listening time but no movement is sent.
- **Device check on the Fire Stick:**
  - play a book, pause, then Back
  - the server's listening sessions now include one from this app, with roughly the time played
  - the position is correct
