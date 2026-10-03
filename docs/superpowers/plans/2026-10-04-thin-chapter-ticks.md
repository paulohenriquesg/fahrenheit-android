# Thin the book bar's chapter ticks (#143) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** On a book with very many short chapters (one seen with about 158), the slim book bar's chapter ticks read as a comb. A tick that would sit closer than a few dp to the last one shown is dropped. The first tick is kept, and the rest are thinned evenly, so the bar stays readable at any chapter count. The chapter bar and the Chapters panel are unchanged.

**Architecture:**
- **`ChapterClock.thinned(ticks, widthPx, minGapPx)`** (pure): keeps the first tick, then each one at least `minGapPx` from the last kept.
- **`PlaybackBar`** applies it where it draws, since only there is the bar's width known. The minimum gap is 6 dp: three times a tick's 2 dp width.

## Rulings

1. **The minimum gap is 6 dp.**
2. **Thinning is greedy from the start.** For evenly spaced chapters that keeps every n-th one; uneven chapters keep the ones that fit (even only for even chapters; the device check on the 158-chapter book decides whether a stride is worth it). No tick sits within the minimum gap of the bar's end, and half a pixel of slack keeps rounding from deciding a gap of exactly the minimum (review).

## Tasks

1. **Red:** `ChapterClockTest` gets these cases:
   - ticks far apart are all kept;
   - 158 even ticks over 800 px keep gaps of at least 6 px, and the first one;
   - nothing in, nothing out;
   - a zero width keeps nothing but the first.
2. **Green:** `thinned`, then use it in `PlaybackBar`'s draw.
3. **Gate, review, PR** (Closes #143).

## Device check (the coordinator)

- **The book with about 158 chapters:** the book bar shows spaced ticks, not a comb.
- **A book with about 20 chapters:** every tick is still there.
