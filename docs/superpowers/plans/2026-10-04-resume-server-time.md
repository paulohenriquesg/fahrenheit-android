# Continue-from-where: server time against server time - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Decide "the server's position is newer" without comparing the TV's clock with the server's (#145, a follow-up to #90 / #142).

**Architecture:** `ServerKnowledge` keeps, per item, the latest thing this player knows of the server's copy, as one of three kinds. `ResumeOffer.of` asks a different question of each:

| Known | Recorded when | Newer means |
|---|---|---|
| `ServerCopy(lastUpdate)` | the item was queued from the server's position; a question was answered | the server's `lastUpdate` is later (server clock against server clock) |
| `Wrote(position)` | a report of this player's reached the server | the server's position differs from it by more than 1 s: the server keeps the last write, so a different one came after (no clock at all) |
| `Since(deviceTime)` | a start was chosen on the details screen (nothing read or written yet) | `lastUpdate` is more than 60 s past the TV's clock (fallback; the margin absorbs skew) |

Nothing known still counts as newer. The latest event wins, whatever its kind: events arrive in order, and times of different kinds do not compare.

**Why not read `lastUpdate` back after each report** (the issue's proposal): the session sync returns no body, so it would be one more GET every 5 seconds while playing. The written position answers the same question with no request.

## Global Constraints

- Branch `fix/resume-server-time` from `origin/main`; PR `Closes #145`. #144 stacks on it.
- Test-first, watched red; gate `./gradlew :app:testDebugUnitTest :app:lintDebug` by exit code; zsh, no `PIPESTATUS`.
- No real `PlayerActivity` in Robolectric; plain functions and the service's listener.
- No bot trailer; public repo fixtures.

## Tasks

- [ ] **1. `Known` and `ServerKnowledge`:** `read(item, episode, lastUpdate)`, `wrote(item, episode, position)`, `since(item, episode, deviceTime)`, `known(item, episode): Known?`. Tests: the latest event wins across kinds; items and episodes apart.
- [ ] **2. `ResumeOffer.of(here, server, known, latestDevice, thisDevice)`:** the table above. Tests: at the position this player wrote, nothing is asked however the clocks stand; a different position is asked even when its `lastUpdate` reads older than the TV's clock (the #145 case); the `Since` margin both sides; `ServerCopy` as before.
- [ ] **3. Recording:** `ProgressReporter` hands each delivered report on; `PlaybackReporting.delivered(file, position)`; the service records `wrote`; `PlayerStart` records `read` (queued from the server) or `since` (chosen start); `ResumeCheck.answered` records `read(offer.listenedAt)`. Tests: a delivered report records its position, a failed one nothing; queue and chosen start as listed.
- [ ] **4. Gate, review, PR.**
