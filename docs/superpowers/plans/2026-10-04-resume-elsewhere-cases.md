# Continue-from-where: progress without a session, and Play from outside - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Cover the two cases #142 left open (#144): progress written without a listening session, and Play that does not come through the player screen.

**Stacked on** `fix/resume-server-time` (#145): it uses `KnownProgress`.

## 1. Progress written without a session

A web-UI progress edit, or an app using the plain progress PATCH, writes `lastUpdate` but no session, so the latest session is still this TV's and the same-device check said "this TV".

- `ItemListeningSessions.latest(): LatestSession?` - the latest session's `deviceId` and `updatedAt`.
- `ResumeOffer.of(..., latestSession, thisDevice)`: this device's session excuses the server's copy only when it is at least as recent as that copy (`updatedAt >= lastUpdate - 10 s`; both on the server's clock, written together by a sync). A copy newer than this TV's latest session was written elsewhere, and is asked about.
- This TV's own fallback PATCH (no session could be opened) is not mistaken for someone else: what it wrote is known by its position (#145), so the copy is not newer to begin with.

## 2. Play from outside the player screen

The player screen asks before it plays (#142). Every other Play reaches the service through the media session: the remote's Play key on Home and the other screens (the Now playing entry only opens the player), or with the app in the background.

- `PlaybackSessionCallback.onPlayerCommandRequest`: a play/pause from a controller that is not this app, while paused, is held, and handed to `OutsidePlay`.
- `OutsidePlay.request()`: checks the queued item (same `ResumeCheck`, capped at 1.5 s). Nothing to ask: plays. Something to ask and the app on screen: opens the player screen on the item, which asks as it reattaches (#142), and does not play. App not on screen (an activity cannot be started from the background): plays, as before - noted as a limit.
- `AppVisibility`: started-activity count, kept by the Application.
- `ResumeSources`: the progress and session reads, shared by the player screen and the service.

## Global Constraints

- Branch `fix/resume-elsewhere-cases`, stacked on #145; PR `Closes #144`.
- Test-first, watched red; gate by exit code; zsh.
- No real `PlayerActivity` or `MainActivity` in Robolectric; the session callback is tested with `ControllerInfo.createTestOnlyControllerInfo`, as `PlaybackServiceTest` does.

## Tasks

- [ ] 1. `LatestSession` and the session rule. Tests: this TV's session as recent as the copy excuses it; one older by more than 10 s does not; the latest session is picked by `updatedAt`.
- [ ] 2. `OutsidePlay`. Tests: nothing to ask plays; something to ask opens the screen and does not play; cannot open plays; slow plays.
- [ ] 3. The session callback holds an outside play while paused; this app's own commands, and anything while playing, pass. `AppVisibility` counts.
- [ ] 4. Wiring in `PlaybackService` and `FahrenheitApplication`; gate, review, PR.
