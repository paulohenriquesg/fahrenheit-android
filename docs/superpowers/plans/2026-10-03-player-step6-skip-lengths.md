# Player step 6: skip lengths - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Two settings, **Skip back** and **Skip forward**, each 10, 15, 30 or 60 s, defaulting to 30 and 30 (spec §Skip lengths, #107).
- They live in Settings' Playback group.
- The transport's skip buttons jump by them, and their icons show the number.
- The remote's own rewind and fast-forward keys, which Media3 turns into `seekBack` and `seekForward` on the session, follow them too.

**Architecture:**
- **`PlayerSettings`** gains `skipBackSeconds` and `skipForwardSeconds`, plus `SKIP_LENGTHS`. A stored value that is not on offer reads as 30.
- **`SkipLengths`**, a `ForwardingPlayer` in the playback service in front of the ExoPlayer. Its `seekBack`/`seekForward` and the increments it reports read the settings at the moment of the press, so a change in Settings applies without rebuilding the player. ExoPlayer's own increments are fixed when it is built.
- **`MediaPlayerController`** takes `skipBack`/`skipForward`. `SkipIcon` draws the rewind or forward arrow with the number inside. The content description is "Skip back N seconds".
- **`SettingsView`** has two rows in Playback, with the settings screen's own `Choice` buttons.

## Global Constraints

- Branch `feat/player-skip-lengths` from origin/main (a862311).
- Test-first, each test watched red; gate; fresh review; one fix pass; PR.

## Rulings

1. **The number in the icon** is the length in seconds ("10", "15", "30", "60"), drawn over a circular arrow.
2. **A change in Settings applies to the next press,** including on a player already open (the screen reads the settings when it starts; the service reads them on every press).

## Review Focus

1. **The remote's rewind/fast-forward keys follow the setting.** Pinned in `SkipLengthsTest`.
2. **A stored value not on offer reads as 30.** Pinned in `PlayerSettingsTest`.

### Task 1: `PlayerSettings` skip lengths
### Task 2: `SkipLengths` in the service
### Task 3: The transport uses them; `SkipIcon`
### Task 4: The Settings rows; wiring; gate; review; PR

### Device check (the coordinator)

- **Settings:** set Skip back to 10 and Skip forward to 60.
- **The player:**
  - the icons read 10 and 60;
  - presses jump by 10 and 60;
  - the remote's rewind and fast-forward keys jump by the same.
- **Defaults:** 30 and 30.
