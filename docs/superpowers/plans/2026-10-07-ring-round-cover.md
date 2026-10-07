# A round cover inside Now playing's ring (#177) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Now playing's closed entry (`CoverWithRing`, also the Now playing bar on the book and podcast screens) draws a 32 dp square cover in a 44 dp ring with a 3 dp stroke. The square's diagonal (~45 dp) is wider than the ring's inside (38 dp), so its corners run into the arc. The cover becomes a circle that sits inside the ring with a small even gap.

**Architecture:** `CoverWithRing` wraps `CoverImage` in a box clipped to `CircleShape`, so a fetched cover and a placeholder are both round. `CoverImage` itself is unchanged: every other cover stays square.

## Rulings

1. **36 dp**, as the issue says: 1 dp of gap all round inside the 38 dp the stroke leaves.
2. **The badge is unchanged:** 18 dp at the bottom-right, over the ring.
3. **The clip is on a wrapper tagged `NOW_PLAYING_COVER_TAG`**, so tests can find the cover's bounds whatever `CoverImage` draws.

## Tasks

1. **Red:** `NowPlayingStateTest` gets:
   - the cover is 36 dp, inside the ring's 38 dp inner diameter;
   - the cover is round: with no server, the placeholder is drawn; its top-middle pixel is the placeholder's tone, and its top-left corner pixel is not (a square draws the tone there too);
   - the existing badge tests still hold for playing and paused.
2. **Green:** the wrapper, clip and size in `CoverWithRing`.
3. **Docs:** a line in `docs/ui-style-guide.md`'s Now playing section.
4. **Gate, review, PR** (Closes #177).

## Device check (the coordinator)

- **The rail, closed, while playing a book with art:** the cover is a circle with a thin even gap to the ring; the arc is not crowded at the diagonals; the bars badge sits bottom-right.
- **Paused:** the pause badge in the same place.
- **An item with no cover:** the placeholder is round too.
- **The Now playing bar on a book screen and a podcast screen:** the same round cover.
