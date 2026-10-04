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

Back walks up one level at a time, and only leaves the app from Home's rail:
content → the section's rail → Home → out. `BackAction.decide(view, railHasFocus)`
is the table; `MainBackHandler` applies it and stands aside for the last step, so
the system does the leaving (#53, #125/#128).

In the player, Back keeps playing: it lets go of the controller and returns to the
screen behind (`PlayerActivity`, #155/#157). Stopping is a choice, not a side
effect of leaving — **Stop** sits under **Now playing** in the rail
(`NowPlayingEntry`) and ends the session through `Playback.end`.

Anything drawn over a screen — a panel, About — takes Back first and closes
itself (`SidePanel`, `AboutScreen`).

## The sections are always on screen

The menu is `androidx.tv.material3.NavigationDrawer`: a rail of icons that stays in the layout
and widens to labels when it holds focus. The phone `ModalNavigationDrawer` it replaced was
invisible until opened and covered the screen when it was, so the left edge had nothing for a
D-pad to reach (#58).

The drawer widens itself when focus *enters* the sheet, which does not happen when focus starts
there, so `NavigationRail` sets the state from `railHasFocus` instead of relying on that.

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
- Server descriptions may contain HTML. Render it with `RichText.fromHtml`
  rather than printing tags or stripping them: emphasis survives (#57).
- Home's shelf headings are `ShelfHeading`: 16 sp bold (`titleMedium`). At the
  24 sp headline they outweighed the covers they name (#121).

## Numbers and dates

Formatters pass an explicit `Locale` — `ROOT` where the suffix is English —
because the default locale renders Latin digits in other numerals: a Persian
device would show "۲ h ۱۱ min". `HouseStyleTest` enforces this.

Counts a viewer reads as a number are grouped as the device writes them,
"4,966 hours": `NumberFormat.getIntegerInstance(locale)` into a plural string
(`count` in `SwitchLibraryView`, #115). Grouping is the one thing the device
locale decides; a count with an English unit beside it stays `ROOT`.

Durations, by where they appear:

- **A length or time left** on a card, a row or a cover: `listeningLength`.
  From 10 minutes up it rounds to whole minutes ("29 min", "4 h 12 min", "1 h");
  below 10 it keeps seconds ("9 min 59 s"). Past ten minutes seconds are noise
  (#119, #114).
- **The player's running counter**: `PlaybackPosition.spoken`, which keeps
  seconds under an hour, so a short episode visibly moves. Never round it.
- **A stats tile**: `shortDuration` ("181 h", "2 h 11 min"). A real listen
  shorter than a minute says "under a minute" rather than rounding to zero.
- `formatDuration` ("3h 5m") is what the book screen's facts, "Resume at" and
  podcast episode rows still use; new text should use one of the above.

## Components

Prefer **`androidx.tv.material3`**: its components are built for focus. Reach for
`androidx.compose.material3` only where TV has no equivalent — text fields,
progress indicators, icons.

Most screens predate this rule and still import the phone components; they are
being moved over screen by screen rather than in one sweep.

## Buttons

One action on a screen is the one you most likely want. It is a **filled**
button in `primary`; every other action is **outlined** (transparent, 1dp
`onSurfaceVariant` border). The filled one **inverts on focus** — `onPrimary`
fill, `primary` text — because the TV default turns any focused button white,
and the dark theme's light primary to white barely changes (#117).

`LoginButton` (`login/LoginScreen.kt`) is the implementation. Known gap: the
book screen and About (#137) draw Resume and Mark finished as the default TV
`Button` (`ActionChip`), not yet filled-and-outlined as
`docs/mocks/book-screen.html` draws them.

## Panels and screens over the player

- **Speed, Sleep and Chapters slide in from the right** over a scrim, with the
  player visible behind. They are one component, `SidePanel`; `PlayerPanels`
  remembers which chip opened one, and focus goes back to it on close. Back
  closes the panel, and focus cannot leave it for the controls behind (#107).
  The book screen opens the same Chapters panel.
- **About is its own screen**, not a panel: `AboutScreen` lays the book out as
  the book screen does (`BookOverview`), over the same cover wash, drawn over
  the player so the player keeps its state (#134, #137).

## The player's look, on every full screen

Every full screen should look like the player: a **cover or the cover wash
behind it, not flat black**. The wash is `CoverWash`: the cover's most colourful
colour, darkened until `onSurfaceVariant` text keeps 4.5:1; a grey cover gives no
wash rather than a muddy one. It is **dark theme only** (`CoverWash.appliesOn`):
under the light theme's dark text it would be a dark blob. Apply it with
`Modifier.coverWash`.

Known gaps — only the player and `AboutScreen` do this today. Login, Settings,
the library lists (Library, Series, Collections, Authors, Narrators, Latest
Episodes, Switch Library), Home,
Stats, Search, the book, podcast, author and series/collection screens, and the
Update screen are still a flat `background`.

## Covers

`CoverImage` draws a cover the way the server's web client does (#149):

- **No cover, or one that failed to load**: a placeholder with the title in the
  middle and the author near the bottom, never an empty box. An item that says it
  has none is not asked for (`hasCover = false`); a 404 is not cached.
- **A cover that is not square** is fitted whole inside the square over a
  blurred, dimmed copy of itself, not cropped.
- **Series and collections** (`BookGroupCard`) show their books' covers: a
  collection its first two side by side, a series fanned up to three of the
  books that have one.

## Home shelves

The server decides which shelves Home shows; `HomeShelves` decides what each one
does. One table, by shelf id then type: how the row is drawn, what a press and a
long press do, where "See all" leads. A shelf of a type this version does not know
is drawn as plain covers if it holds items. **Adding a shelf is one row there**
(#147).

## Mocks

`docs/mocks` holds the design of record for each screen; build to the mock, and
change the mock when the design changes. Each is drawn at 1920x1080 in the app's
palette (see `docs/mocks/README.md`).

| mock | covers |
|---|---|
| `login.html` | sign-in above the Fire TV keyboard: server discovery, a returning user, errors, the filled/outlined buttons |
| `screens.html` | the navigation rail, Home and its shelf headings, item detail, the first player, search |
| `player.html` | the player (layout C is the one built), the Speed/Sleep/Chapters panel, About, episodes, Now playing in the rail |
| `book-screen.html` | the book screen and About as one layout (option 1 is the one built) |
| `podcast.html` | the podcast screen: every episode, downloads, admin and not, the header that scrolls away |
| `latest-episodes.html` | Latest Episodes grouped by day, and its empty state |
| `settings.html` | Settings and Switch Library as sections, the theme choice, checking for updates |
| `stats.html` | the stats screen, before and after |

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
