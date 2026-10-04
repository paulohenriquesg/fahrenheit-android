# Polish from the device passes (#170) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Five small things from the device passes of 2026-10-04: Now playing shows the play *state*, the no-cover placeholder has its own tone, About's description has room inside its focus border, episode lengths round as everywhere else, and Playback is the second Settings section without hiding a waiting update.

## Rulings

1. **Now playing's state** (`CoverWithRing`, shared by the rail entry and the bar): playing draws **equaliser bars**, paused a still **⏸ badge**. The bars move on a *finite* run, restarted each time the entry changes - the slot recomputes it every poll (5 s) while playing - so nothing animates forever, a paused or idle entry is still, and an off-screen one is not composed at all. The run lasts one poll, so the bars keep moving while the poll keeps coming. Their heights are a pure function, `Equaliser.levels(phase)`, tested on its own.
2. **The placeholder's tone** is `surfaceVariant` halfway to `surface`: a darker, greyer purple in the dark theme, so it reads as a cover inside a focused card (whose fill is `surfaceVariant`) and still apart from an unfocused one (`surface`). One function, `CoverPlaceholder.tone(scheme)`.
3. **The description's inner padding** grows from 4 dp to 12 dp inside the 3 dp focus border (the book screen and About share `FullDescription`). The "still at the top" slack grows with it.
4. **Episode lengths**: `EpisodeDetails.line` and the player's "Up next" use `listeningLength`. The running counters keep `PlaybackPosition.spoken`. About's Length fact is not in the issue and is left as it is.
5. **Settings order**: Appearance, **Playback**, Updates, Account. A waiting update's "is ready / Install" row moves to the **top of Settings**, above Appearance, so it is the first thing seen; Updates keeps "Check for updates". There is one Install row, not two.

## Tasks (each red first, then green)

1. `NowPlayingEntryTest`: playing shows the equaliser (tagged, "Playing") and no ⏸; paused shows the ⏸ badge ("Paused") and no equaliser; the bars move during a run and are still after it (pixels, time moved by hand). `EqualiserTest` for the levels. Same in `NowPlayingBarTest`.
2. `CoverImageTest`: the placeholder's tone is not the focused card's fill nor the unfocused one, in both themes; the placeholder is drawn in it.
3. A description test: the text sits at least 12 dp inside its border.
4. `EpisodeDetailsTest`, `PlayerScreenTest` / `UpNextTest`: "30 min", "28 min"; under ten minutes keeps seconds.
5. `SettingsViewTest`: the section headings in the new order; a waiting update's row is above Appearance and displayed without scrolling.
6. Style guide and settings mock follow; gate, review, PR (Closes #170).

## Device check (the coordinator)

- Rail and book/podcast bar while playing: moving bars; paused: a still ⏸.
- A book without a cover, focused on Home/Library: the placeholder's edge shows inside the purple card.
- About and the book screen: focus the description; the border clears the text.
- An episode of 10 min or more: the details line and "Up next" say whole minutes.
- Settings: Playback second; with an update waiting, its Install row is at the top.
