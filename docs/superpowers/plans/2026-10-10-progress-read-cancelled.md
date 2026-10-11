# A cancelled progress read is not "couldn't read" (#217) - Implementation Plan

**Goal:** the player no longer says "Couldn't read where you left off" when the
read was only cancelled. `PlayerActivity` reads inside
`LaunchedEffect(connected, playing)`, which restarts when either changes; the
cancelled read became `Unreadable` and toasted, because `SavedProgress.read`'s
`runCatching` caught the `CancellationException` too.

## Change

- `SavedProgress.read` rethrows `CancellationException`; any other exception
  is still `Unreadable`.
- It reports why a read was unreadable through `whyUnreadable`, and both call
  sites (`PlayerActivity`, `ResumeSources`) log the exception's class at debug,
  so a device run shows what failed.

## Tasks

1. **Red**, in `SavedProgressTest`:
   - a fetch that throws `CancellationException` propagates it, and nothing is
     reported;
   - a read cancelled from outside, mid-fetch, ends cancelled rather than
     `Unreadable`;
   - an `IOException` is still `Unreadable`, and is reported.
2. **Green**, then the debug log at the two call sites.
3. **Gate, review, PR** (Closes #217).

## Device check (the coordinator)

- Right after launch, open several episodes in a row from Home: no toast.
- With the server unreachable, opening an episode still shows the toast, and
  logcat shows the exception's class.
