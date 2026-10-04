# Back keeps playing; Stop from Now playing (#155)

Reverses "Back stops playback" (#96, spec section "Back"). Back from the player
leaves it and keeps playing; the screen behind shows, and Home's rail carries the
Now playing entry (#140). Stopping moves to a **Stop** action on that entry.

## Steps (each test first)

1. **Playback.end** - stop and clear, the one way to end a listening session
   (the closing report and session close ride `LeavingGuard.stop`, unchanged).
   `Playback.leave(controller, finishing)` goes: `switchTo` (About's other book,
   Previous/Next episode) and `Playback.stop` (sign-out) call `end`.
   - PlaybackServiceTest: "leaving by Back empties the queue" becomes "ending
     playback empties the queue"; "leaving for Home keeps the queue" becomes
     "the screen letting go of its controller keeps playing" (released controller,
     queue and playWhenReady intact).
2. **PlayerActivity.onStop** releases its controller and nothing else; `handedOver`
   goes (nothing to guard any more). "Go to podcast" keeps the episode playing too.
3. **Stop on the entry** - in the rail's open state, a Stop button under the entry
   (`NOW_PLAYING_STOP_TAG`); none when closed (a closed rail holds no focus).
   NowPlayingEntryTest: open shows Stop and choosing it reports the entry; closed has none.
4. **NowPlayingSlot stops the player it reads** (`Playback.end`), then tells the
   main screen, which moves focus to the selected section - the focused button is
   about to disappear. RailEntrySourceTest-style test with a TestExoPlayer: Stop
   empties the queue and the entry goes.
5. Spec and KDoc: Back keeps playing; Stop is on the entry.

## Device checks (in the PR; no hardware in this lane)

Play a book, Back: Home with Now playing, audio on. Open the entry: player where it
was. Stop from the entry: audio stops, server session closed at the right position,
entry gone, focus on a section. Same for a podcast episode, and Go to podcast.

## Review fix pass

Known limits recorded in the spec, left for follow-ups: no rail (so no Now playing
or Stop) on the book and podcast screens; with "Play next episode" on, after Back
only the one episode already queued behind plays on. Stale "Back stops" comments
updated.
