# Player state into a ViewModel (#221) - Implementation Plan

**Goal:** `PlayerActivity`'s `Player` composable keeps no business state. The
item, the episode shown, the cover wash, ready / failed, the series, the
heart, Mark finished, the resume question and the sleep timer - with the
`LaunchedEffect`s and `DisposableEffect`s that load and follow them - move
into a `PlayerViewModel`, as one immutable `PlayerUiState` in a `StateFlow`,
with events as methods. Same shape as `MainViewModel` (#220).

No behaviour change: the same moments for reading progress (each connect,
before queueing; the next episode's start with auto-advance; the resume
check on reattach and on Play), the same moments for writing it (none from
the screen: the service writes; the screen only tells `ServerKnowledge` what
it queued from, through `PlayerStart`, and asks the service to mark finished).

## Boundary

Playback stays in `PlaybackService` / Media3. The ViewModel never builds,
owns or releases a player. It is handed the connected controller as a
`PlayerLink` and lets go of it on `disconnect()`:

```kotlin
interface PlayerLink {
    /** The service's player: queue, position, playing, media-item transitions. */
    val player: Player
    /** What the service reports beyond the player: the sleep timer. */
    val sessionExtras: Bundle
    /** A command to the service: Sleep, Mark finished. */
    fun send(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult>
}
```

`player` is Media3's `Player` interface, not `MediaController`: the existing
helpers (`PlayerStart`, `BookPlayback`, `QueueEndWatch`, `ListeningControls`,
the transport) take a `Player` and stay as they are. What only a
`MediaController` has - its extras and custom commands - is the rest of the
interface. The Activity wraps its controller (`ControllerLink`); tests pass a
`TestExoPlayer` with fake media and a recording `send`.

The ViewModel listens on `player` for media-item transitions (the episode
shown, #108), the queue's end (`QueueEndWatch`, #179) and playing starting
(un-finishes, as `rememberFinished` did), and removes them on disconnect.

### Stays in the Activity

- Connecting and releasing the controller (`ControllerSlot` in
  `onStart` / `onStop`), telling the model `connect(link)` / `disconnect()`,
  and forwarding `onExtrasChanged` to `extrasChanged`.
- Key handling: `onKeyDown` / `onKeyUp` ask `model.remotePlay(down)`.
- Navigation: `switchTo` (Play instead, Previous / Next episode),
  `leaveForPodcast`, `closeAtQueueEnd` (on the model's `QueueEnded` notice).
- Toasts for the model's notices (progress unreadable, mark failed, heart).
- Reading the intent into the factory (`PlayerViewModel.of`).

### Stays in the composable

Focus (the play-again nudge after a resume answer), the open panel
(`rememberPlayerPanels`, keyed on the player as before), chapter spans derived
from what is shown, and the transport, which keeps polling the player for its
own position and speed.

### Not injected: the progress store

The player shows no stored progress: it reads the server's at the moment it
queues (#90), on purpose, and the service's `ProgressWrites` already feeds the
store. Injecting it would be dead weight or a behaviour change.

### Small additions to existing helpers

- `ResumePrompt` gains an optional `shown: (ResumeOffer?) -> Unit`, called when
  its question appears or goes, so the state can carry it. Nothing else changes.
- `rememberFinished` is no longer used by the player; it is deleted with its
  test, whose cases move into the ViewModel's tests.

## Tasks

1. **Red**, `PlayerViewModelTest` (Robolectric, unconfined scope, TestExoPlayer):
   - opening a book queues it at the saved position, paused; opened to play, plays;
   - an unreadable progress read says "starting from the beginning" once; a
     read cancelled by leaving says nothing;
   - the item failing to load, or nothing to play, shows the error;
   - the shown episode follows a media-item transition to the next episode;
   - coming back to what is already queued and paused asks the resume
     question (the prompt), and to what is playing does not;
   - with auto-advance, the next episode is queued at its own saved position;
   - Mark finished: marking while asked, finished on success, a notice on
     failure; playing again un-finishes;
   - the queue's end says `QueueEnded`;
   - the sleep timer from the extras, on connect and on change;
   - the heart and the series load for what is shown.
   - `ResumePromptTest`: `shown` hears the question and its answer.
2. **Green**: `PlayerLink`, `PlayerViewModel` (+ `of`), `ResumePrompt.shown`,
   `PlayerActivity` reads `PlayerUiState` and calls the model.
3. **Gate, review, PR** (Closes #221).

## Device checks (the coordinator, on the stick)

- [ ] Open a book from Continue Listening, and from its detail page: it opens at its saved position.
- [ ] Resume position: listened elsewhere, the resume prompt asks; both answers do what they say.
- [ ] Next episode at the end, with auto-advance on: the screen follows; at the newest one it closes.
- [ ] Leave (Back / Home) while playing and come back: still playing, no prompt.
- [ ] Sleep timer: set, counts down on the chip, cancel.
- [ ] Remote play/pause and skip keys; Play from a pause after listening elsewhere asks first.
