# The show's auto-download settings in a Downloads panel (#182) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** A **Downloads** button on a podcast's screen opens a side panel with
the show's own auto-download settings as the server keeps them, saved as each is
chosen: download new episodes (on/off), how often to check, how many to keep,
how many per check. Mock: `podcast-actions.html`, frame 3.

## The server (checked in the Audiobookshelf server source, not from memory)

- Podcast media fields: `autoDownloadEpisodes` (bool), `autoDownloadSchedule`
  (cron string), `maxEpisodesToKeep` (int, 0 keeps all: the server only trims
  when it is set), `maxNewEpisodesToDownload` (int, 0 is no limit: the web app
  says "use 0 for unlimited").
- Saved with `PATCH /api/items/{id}/media`. The podcast's `updateFromRequest`
  changes only the keys present, so one field goes per change. An invalid cron
  gets a 400.
- That route needs `canUpdate`: the user's `permissions.update`, else 403.
- The web app's presets: every hour `0 * * * *`, every day `M H * * *` (default
  `0 0 * * *`), and a weekly schedule as `M H * * d`.

## Architecture

- **`DownloadSchedule`** (pure): cron to choice and back. `M * * * *` is Every
  hour, `M H * * *` Every day, `M H * * d` (one day) Every week, and anything
  else, or none, is **Custom**. Picking sends `0 * * * *`, `0 0 * * *` or
  `0 0 * * 0`. A schedule that already reads as the choice picked is left
  as it is.
- **`DownloadSettings`** (pure): the four values from the item's media, the
  options for Keep and Per check (with the server's own value added when it is
  not one of ours), and `mayChange(me)`.
- **`PodcastDownloads`**: holds the settings; `change` shows the new value at
  once, sends only that field, and on failure puts that field back and says so.
- **`PodcastSettingsApi`**: its own one-method interface, so the existing
  `PodcastApi` fakes are untouched.
- **`DownloadsPanel`**: `SidePanel` (now with a width parameter, 430dp here, as
  the mock's panel is) with the toggle and three rows of choices. It scrolls,
  and Custom is shown but cannot be picked.
- The header line says "New episodes download every hour / day / week" when on,
  and "Automatic downloads on" for a custom schedule.

## Rulings

1. **Hidden, not read-only, for others.** The header line already tells anyone
   whether and how often episodes download, so a panel that cannot be changed
   would add nothing but a dead end.
2. **The failure message shows in the panel**, under its title. It is cleared
   by the next change.
3. **The podcast's screen now keeps its margin inside its content**, as a book's
   does, so the panel reaches the screen's edges.

## Tasks

1. **Red:** `DownloadScheduleTest`, `DownloadSettingsTest` (mapping, options,
   permissions), `PodcastDownloadsTest` (one field per change as JSON, shown at
   once, failure restores only that field, an unchanged pick sends nothing),
   `DownloadsPanelTest` (current choices ticked, Custom shown and not
   pickable, a press asks for the change, the failure line), and
   `PodcastScreenModelTest` (the header line).
2. **Green**, then wire it in `DetailActivity.PodcastEpisodes`.
3. **Gate, review, PR** (Closes #182).

## Device check (the coordinator)

- As an admin: Downloads opens the panel from the right, with the show's current
  settings ticked. Each change sticks after closing and reopening the screen,
  and the web app shows it.
- A show whose schedule was set to something else in the web app shows Custom,
  and the schedule stays as it was until a choice is picked.
- As a user without update rights: no Downloads button.
- With the server unreachable: a change goes back, with the message.
- Back closes the panel and focus returns to Downloads.
