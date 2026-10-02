# UI style guide

This is a TV app driven by a D-pad from across a room. Most of the rules below
exist because breaking them shipped a bug; the issue number is on each one.

## The screen is 540dp, not 1080

These sticks report 1920x1080 at density 2.0, so **every dp is two pixels** and a
whole screen is **960 x 540dp**. A design that looks roomy in dp is half the size
you think in px.

- Budget 540dp of height, minus the top bar.
- **Any screen whose content can exceed that must scroll.** Four 104dp cards and
  three gaps did not, and the fourth was unreachable (#54).
- Over-tall and scrollable beats exactly-fitting and clipped: server strings grow.

## Safe area

TVs overscan. Keep content inside `Space.screenH` (24dp) horizontally and clear of
the top bar vertically. Full-width text blocks get `Space.readingH` (48dp).

Every section reached from the rail has a `ScreenTitle` as its first child, so the
title does not move as you switch sections (#82):

- `headlineMedium`, `onBackground`, one line with ellipsis. `ScreenTitle` does this;
  its trailing slot holds anything that belongs beside it, such as Library's count.
- Inside a container padded `horizontal = Space.screenH` and `vertical = Space.gap`,
  on the same band as the search icon and greeting.
- Outside any scroller, so it stays put while the content scrolls under it.
- Shown in every state, loading and empty included.

Item titles next to a cover (detail screens and players), Login and the Update
screen are not sections and keep their own.

## Focus

Nothing on a TV responds to the remote until something holds focus.

- Every screen requests focus on arrival with `rememberInitialFocus`, attached to a
  **real focusable target** (#53).
- Never request focus on a container. Until a child is focusable the focus group
  takes it itself, highlights nothing, refuses `moveFocus(Enter)`, and sends the
  first press to the second item.
- Every focusable box shows focus by itself: `Border.focus` (3dp primary) against
  `Border.rest` (1dp surfaceVariant). A focus state that only changes background
  is invisible at viewing distance.
- Touch targets are irrelevant here; focus targets are not. Anything a viewer must
  reach is `focusable()`, not a `Box` with a click.

## Back

Back walks up a level, it does not leave: `BackAction.decide(drawerOpen, view)`
closes the drawer, else goes Home, else lets the system have it (#53).

## The sections are always on screen

The menu is `androidx.tv.material3.NavigationDrawer`: a rail of icons that stays in the layout
and widens to labels when it holds focus. The phone `ModalNavigationDrawer` it replaced was
invisible until opened and covered the screen when it was, so the left edge had nothing for a
D-pad to reach (#58).

The drawer widens itself when focus *enters* the sheet, which does not happen when focus starts
there, so `NavigationRail` sets the state from `hasFocus` instead of relying on that.

## Focus does not move things

Focus is a 3dp border and nothing else. The TV `Card` grows by a tenth when focused, and the
lazy row or grid holding it clips the result — the focused cover is cut off at its own edges —
while every other focus in the app stays put. `CardFocus.noGrowth` is what every card passes as
its `scale`.

## Lists

**Every lazy list item carries a key**, and `HouseStyleTest` fails the build if one
does not. Keys that can repeat (podcast shelves list a library item once per
in-progress episode) go through `StableKeys.of`, which makes them unique by
construction — duplicate keys crashed the app on launch (#55).

## Test ids, so a script can name things

A screen's root carries `Modifier.semantics { testTagsAsResourceId = true }`, which turns every
`testTag` into a `resource-id` in the accessibility tree. `uiautomator dump`, `adb` and Maestro
can then address an element by name.

Navigation targets carry one each, from `menuItemTestTag(id)`: `menu_item_home`,
`menu_item_stats`, and so on. Before this, driving the app from a script meant counting D-pad
presses and hoping focus started where you assumed — which, twice in one session, logged the
user out instead of opening the screen under test.

Anything a device script or a flow needs to reach gets a tag.

## Text

- Body 16sp and up, secondary 14sp and up. Below that is unreadable at 3 metres.
- Every string that came from the server gets `maxLines` and
  `TextOverflow.Ellipsis`. Titles run long and wrap into the next box otherwise.
- Server descriptions may contain HTML; strip it before display (#57).

## Numbers and dates

Formatters pass an explicit `Locale` — `ROOT` where the suffix is English —
because the default locale renders Latin digits in other numerals: a Persian
device would show "۲ h ۱۱ min". `HouseStyleTest` enforces this.

Durations: `shortDuration` for a tile ("181 h", "2 h 11 min"), `formatDuration`
for inline text ("3h 5m"). A real listen shorter than a minute says "under a
minute" rather than rounding to zero.

## Components

Prefer **`androidx.tv.material3`**: its components are built for focus. Reach for
`androidx.compose.material3` only where TV has no equivalent — text fields,
progress indicators, icons.

Most screens predate this rule and still import the phone components; they are
being moved over screen by screen rather than in one sweep.

## Colour

Use `MaterialTheme.colorScheme` roles, never literal colours, so the app keeps
following the device's dark/light setting. Scrims are the exception.

| role | for |
|---|---|
| `background` / `onBackground` | the screen behind everything |
| `surface` / `onSurface` | a box sitting on it |
| `surfaceVariant` | a box's border, a bar's track |
| `onSurfaceVariant` | labels, secondary text |
| `primary` | focus, and the one value a chart is pointing at |
| `secondary` | the rest of a chart |

## The box pattern

Boxes next to each other, not cards stacked down the page:

- `Radius.panel` (10dp) corners, `surface` fill, 1dp `surfaceVariant` border.
- `Space.gap` (16dp) between boxes, `Space.inset` (14dp) inside one.
- A label in `labelLarge` `onSurfaceVariant`, its value in `headlineSmall`
  `onSurface`.

`stats/StatsBoard.kt` is the reference implementation.
