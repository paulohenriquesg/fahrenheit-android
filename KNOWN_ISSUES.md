# Known Issues

## Chapter Name Display Bug
**Status**: Unresolved
**Severity**: Medium
**Affected Component**: Book Player

**Description**: When opening the book player screen, the chapter name displays incorrectly (shows "Opening Credits" or the first chapter instead of the actual current chapter based on playback position).

**Reproduction Steps**:
1. Open a book with progress > 0 (e.g., currently at 01:55:23)
2. Navigate to the player screen
3. Observe: Position is correct (01:55:23) but chapter shows "Opening Credits"
4. Start playing → chapter name corrects itself after playback begins

**Key Observations**:
- Chapter name is wrong ONLY on initial screen load (each time you navigate to player)
- Once playback starts, chapter name updates correctly
- Position display is always correct from the start
- Issue occurs every time you open the player screen, not just on cold app start

**Technical Details**:
- **Affected code**: `BookPlayerActivity.kt` lines 353-360 (chapter detection logic)
- **Root cause**: `currentPlaybackTime` state initialized to 0.0 or not properly set from current position on screen load
- **Chapter detection runs before state initialization**: The chapter detection code executes before `currentPlaybackTime` reflects the actual media player position
- **Attempted fix 1**: Added `onCurrentTimeUpdate(currentTime)` in `MediaPlayerController.kt` line 90 (after initial seek in `setOnPreparedListener`)
  - **Status**: Did not resolve - callback timing issue
- **Attempted fix 2**: Added `onCurrentTimeUpdate` callbacks to skip buttons and slider
  - **Status**: Resolved skip/slider chapter updates, but not initial load

**Likely Solution**:
- Get position from media player on screen load/composition
- Calculate and set chapter name immediately based on that position
- Update `currentPlaybackTime` state on screen initialization in `BookPlayerScreen`, not just during playback
- **Possible approach**: Add a `LaunchedEffect` in `BookPlayerScreen` that queries the media player position when the screen first composes and updates `currentPlaybackTime` accordingly

**Investigation Needed**:
- Check when `currentPlaybackTime` gets initialized in `BookPlayerScreen` (line 271)
- Verify if `mediaProgress.currentTime` is available when chapter detection runs (lines 353-357)
- Add logging to trace the order of events:
  1. Screen load
  2. Chapter detection
  3. State initialization
  4. MediaPlayerController preparation
  5. Playback start
- Determine why `currentPlaybackTime` stays at initial value (0.0 or mediaProgress.currentTime) until playback starts

**Related Code Locations**:
- `BookPlayerActivity.kt` lines 353-360: Chapter detection logic
- `BookPlayerActivity.kt` line 271: `currentPlaybackTime` state initialization
- `BookPlayerActivity.kt` lines 383-385: `onCurrentTimeUpdate` callback assignment
- `MediaPlayerController.kt` line 90: Initial `onCurrentTimeUpdate` call (in setOnPreparedListener)
- `MediaPlayerController.kt` lines 97-99: Playback loop `onCurrentTimeUpdate` calls

**Workaround**: Start playing the book briefly to trigger chapter name update, then pause if desired.
