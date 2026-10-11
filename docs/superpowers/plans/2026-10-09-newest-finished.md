# Finished marks on Home's Newest episodes (#192) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** A finished episode on Home's Newest episodes row is marked as one: dimmed, with the podcast page's finished tick. Mark, do not hide - the server's shelf lists finished episodes on purpose, as its web client does.

**Architecture:**
- `CoverProgress.index` (fed from the shared progress store, #207) keeps finished episodes too, keyed `itemId/episodeId`, and answers `finished(card)`; `of(card)` still answers only started ones. Book cards are unchanged: `finished` is false for them.
- `FinishedTick` (`ui/elements`): the podcast page's tick, moved out so both screens draw the same one.
- `LibraryItemCard(finished = …)`: the cover dimmed (as the podcast page's rows, until focused), and the tick leading the line under the title. `ShelfRow` passes `progress.finished(item)`.
- The mark comes from the store, so an episode finished in the player shows on Back (#207) with no reload of its own.

## Tasks

1. Red/green `CoverProgress`: a finished episode is finished and not started; a started one keeps its fraction and time left; one finished episode never marks another of the same podcast, nor the podcast's card; book cards are not marked.
2. Red/green `LibraryItemCard`: a finished episode shows the tick and is dimmed; an unplayed one neither.
3. Red/green Home: a Newest episodes shelf marks only the finished episode.
4. Gate, review, PR (Closes #192).

## From review

- A podcast library's Listen again comes as episodes, all finished: it is not marked (`HomeShelves.Behaviour.marksFinished`), or the whole row would grey.
- The fade is the grid's (0.45), not the podcast rows' (0.55); the comment says so. The "Finished" text stays hardcoded, as it was on the podcast page; strings are for a separate pass.

## Device check (the coordinator)

- Finish an episode on the stick, return to Home: its card on Newest episodes is dimmed and carries the tick; its neighbours do not.
- Focus that card: it is no longer dimmed while focused.
- In a podcast library, Listen again shows no ticks and no greyed covers.
