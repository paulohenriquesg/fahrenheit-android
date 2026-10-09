# Screensaver wall: whole tilted covers on the device (#176) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** On the Fire TV Stick the wall from #173 drew only on the left half of the
screen, each cover chopped by untilted lines. #173 turned the wall as a layer
(`graphicsLayer { rotationZ }`) on an oversized layer and clipped each cover with a
`clipPath` under that turn. Keep #172's goal (covers to every edge at every point
of the drift) but draw it the way the device renders.

**Drawing approach:**
- The **layer is not turned**: it only moves (`translationX/Y`, read in the layer,
  so a frame redraws nothing). Its size is the screen plus the drift, centred, so
  at every point of the drift it covers the screen.
- The **tilt is in the drawing**: `rotate(TILT)` round the layer's centre on the
  canvas, as before #173 (which the device drew whole), over a grid big enough to
  cover the layer's whole rect once turned.
- **Nothing is drawn past the layer's bounds**: the drawing is wrapped in an
  axis-aligned `clipRect` of the layer's own size, outside the turn.
- **No `clipPath`**: each cover is a plain square (at this dimming the rounded
  corners barely showed). So there is no clip of any kind under a turn.

## Tasks

1. **Red:** `WallFrameTest`: the unturned layer covers the screen's corners over the
   drift and is no bigger than needed; the turned grid covers the layer's corners and
   is no bigger than needed. `ScreensaverScreenTest`: the layer is the frame's size and
   is not turned (its top edge stays level in root coordinates). From review:
   `CoverWallTest` renders the wall to pixels and checks cover in every corner and
   no bare row or column at both ends of the drift.
2. **Green:** `WallFrame.size` (screen + drift), `WallFrame.grid` (turned cover of a
   rect), `CoverWall` drawing as above.
3. **Gate, review, PR** (Closes #176).

## Device check (the coordinator)

- Wall style, 2 min timeout: captures at both ends of the drift show whole tilted
  covers across the full screen, no black areas.
- Check the bottom-left and top-right corners at the drift's ends in particular:
  at -8 degrees those are where the drawn grid has the least to spare.
- The login screen's welcome-back backdrop: the same.
- If the right half is still black, the turn was not the cause: the layer is
  still larger than the screen (screen plus drift, via `requiredSize`), the
  issue's other suspect, and is the next thing to change.
