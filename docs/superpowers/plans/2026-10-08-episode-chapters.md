# Episodes with chapters get the book's chapter features (#183) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** An episode whose file carries chapters gets what a book with chapters gets: marks on the scrubber and the chapter bar, the **Chapters** chip and panel, previous/next chapter on the outer transport buttons, End of chapter in Sleep, and "Chapter · N min left" in Now playing and on the screensaver. An episode without chapters stays as today.

**Server:** `PodcastEpisode.toOldJSON` sends `chapters` (copied from the audio file's, else the feed's) on every episode of a podcast item (`server/models/PodcastEpisode.js`).

**Architecture:**
- **`Episode.chapters`**: `List<Chapter>?`, read from the item response.
- **`NowPlaying.of`** for an episode sets `chapters` to the episode's, null when it has none. Everything the player draws from `NowPlaying.chapters` (spans, the chip, the panel, the chapter bar, the outer buttons, Sleep's End of chapter) then follows as it does for a book.
- **`PlayerActions`**: the player's action row moves out of `PlayerActivity` into a composable that can be tested: Go to podcast where there is a podcast, Chapters when there are chapters, then Speed, Sleep, About.
- **`rememberRailEntry`'s `chaptersOf`** takes `(itemId, episodeId)` and is asked for an episode too; `RailChapters.known` is keyed by item and episode. `queuedChapters` (and the screensaver's copy) return the episode's chapters when given an episode.
- **`nowPlayingDetail`** names an episode's chapter as it does a book's; its `episode` parameter goes (a chapter is what decides).

## Rulings

1. **Outer buttons:** an episode with chapters skips chapters with them, as a book does; previous/next episode stays for an episode without chapters. That is the book's behaviour, which the issue asks for.
2. **Resume question:** unchanged; an episode's places stay times (the issue does not ask for it).

## Tasks

1. **Red:**
   - `NowPlayingTest`: an episode with chapters carries them; an episode without has none.
   - `PlayerActionsTest`: an episode with chapters shows Go to podcast and Chapters; without, Go to podcast and no Chapters; a book with chapters shows Chapters and no Go to podcast.
   - `RailEntrySourceTest`: a queued episode with chapters names its chapter, and is asked for with its episode id.
   - `NowPlayingDetailTest`: an episode with a chapter names it.
2. **Green:** `Episode.chapters`, `NowPlaying.of`, `PlayerActions`, the rail's `chaptersOf`, `nowPlayingDetail`.
3. **Gate, review, PR** (Closes #183).

## Device check (the coordinator)

- **An episode with chapters:** the player shows the chapter bar with ticks and "left in chapter"; the action row reads Go to podcast, Chapters, Speed, Sleep, About; Chapters opens the panel and choosing one seeks there; the outer buttons go to the previous/next chapter; Sleep offers End of chapter.
- **Now playing (rail and the bar on the book/podcast screens) and the screensaver** name the episode's chapter with its minutes left.
- **An episode without chapters:** as before - one bar, Go to podcast and no Chapters, outer buttons go to the episodes either side.
