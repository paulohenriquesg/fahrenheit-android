# Screensaver: the wall fills the screen, no glow over it, time left in the line (#172) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix three findings from the device check of the screensaver (#156, #169): the wall stops short of the right and bottom edges, the playing cover's colour shows as a stain in the middle of the wall, and the now-playing line can show only the title.

**Architecture:**
- **`WallFrame`** (pure, in `CoverWall.kt`): the wall's size for a screen, a drift and a tilt, and where its centre sits at each point of the drift. The drift is centred on the screen (half each way), and the wall turns round its own centre.
- **`CoverWall`** draws the covers into a layer that *is* the wall: sized by `WallFrame`, centred, turned and moved as layer properties (`rotationZ`, translation). Today the layer is screen-sized and the covers are drawn past its bounds, and the strips on the device are as wide as the drift (120 x 80 dp of 960 x 540). The likely cause is that the TV drops what a layer draws outside its own bounds. This fix does not rely on drawing outside a layer. The login screen's wall gets the same fix.
- **Wall style:** an even dim over the wall, and no radial wash. **Bouncing style:** the wash as a soft glow behind the one cover, moving with it.
- **`nowPlayingDetail`** (pure, given `Resources`): "Chapter · M min left in chapter" for a book, "M min left" for an episode or for a book without chapters. The minutes are whole (`minutesLeft`), with hours past 60 ("1 h 5 min"). The screensaver's source uses it.

## Rulings

1. **Coverage test:** for 101 points over the drift's full range, all four screen corners lie inside the turned wall.
2. **Time left drops the part minute**, as the main screen's rail does, so the two never disagree (review). Below one minute it reads "under a minute" rather than "0 min" or seconds that tick. The helper is `minutesLeft`, named in the style guide.
3. **The bouncing style's caption** keeps `detail ?: title`, so it now also reads "M min left" on an episode.

## Tasks

1. **Red:** `WallFrameTest` (corners covered across the drift; a smaller wall would not cover them). A `ScreensaverScreenTest` case: the wall's layer is at least the frame's size. Wall style with a wash: no glow node, a dim node. Bouncing style with a wash: a glow node. `NowPlayingDetailTest`: book with a chapter title, book with only a number, book without chapters, episode, a part minute, under a minute, over an hour.
2. **Green:** `WallFrame` and `CoverWall`'s layer; the wall's dim and the bouncing glow; `nowPlayingDetail`, used by `PlaybackListening.line()`. Update the mock's wall tint.
3. **Gate, review, PR** (Closes #172).

## Device check (the coordinator)

- **Wall style, left to drift for over a minute:** covers reach every edge at every point of the drift, with no black strip on the right or at the bottom.
- **Wall style with a colourful cover playing:** an even dim, with no coloured blob in the middle.
- **Bouncing style:** a soft glow of the cover's colour behind the moving cover.
- **The now-playing line:** a book shows "Chapter … · N min left in chapter", and an episode shows "N min left".
- **The first key press** only wakes the screen: no skip, no pause.
- **The login screen's wall** still covers its screen.
