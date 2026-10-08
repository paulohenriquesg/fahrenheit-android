# Screensaver: a Try it button in Settings (#190) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Settings → Screensaver style gets a **Try it** button that shows the chosen style at once, full screen, as the real screensaver draws it. Any key closes it and focus is back on Try it. With nothing queued it is disabled and reads **Play something to try it**.

**Architecture:** The overlay's "draw the screensaver for this item" part (art fetched once, the now-playing line, `ScreensaverScreen`) becomes one composable, `ListeningScreensaver`, used by the overlay and by the trial. `SettingsView` takes a `ListeningSource?`; `MainScreen` passes the playback one (`PlaybackListening`, now `internal`). The trial is drawn in a full-screen `Popup` that is **not focusable**: keys keep going to the activity's window, so the screensaver's own key gate still counts them as activity, and focus never leaves Try it, so there is nothing to hand back. While the trial shows, Settings eats every key (Back included) in `onPreviewKeyEvent` and closes on the release of a key pressed while it showed.

## Rulings

1. **Under the Style choices**, in the same row's trailing column, a TV `Button` like Settings' other actions, tagged `screensaver_try_it`.
2. **Nothing queued:** the button is disabled and its label is "Play something to try it". No sample content.
3. **The item is the one queued when Try it was pressed;** the trial stays up if playback stops under it (black behind until the line is known).
4. **Animations:** the trial is the real `ScreensaverScreen`, which moves only while composed; once closed, the compose clock goes idle again. Tests hold the clock while it shows.

## Tasks

1. **Red** (`ScreensaverSettingTest`, Compose + Robolectric):
   - something queued, Wall chosen: clicking Try it shows `screensaver` with the wall;
   - choosing Bouncing and trying again shows the bouncing cover;
   - a D-pad key closes it, focus is still on Try it, and the key moved nothing behind it; Back closes it too;
   - after closing, the UI goes idle with the clock running;
   - nothing queued: Try it is disabled and reads "Play something to try it".
2. **Green:** `ListeningScreensaver`, the row, the popup, the key handling, the strings, `MainScreen` wiring.
3. **Gate, review, PR** (Closes #190).

## Device check (the coordinator)

- Playing something, Settings → Try it with Wall: the wall shows at once, full screen over the rail; any key returns to Settings with focus on Try it.
- Choose Bouncing, Try it: the bouncing cover shows; Back returns to Settings (and does not leave Settings).
- Nothing queued (stop playback): Try it is greyed and reads "Play something to try it".
- Leave a trial up past the screensaver delay while playing: the first key may only wake the real screensaver drawn under it; the next closes the trial.
