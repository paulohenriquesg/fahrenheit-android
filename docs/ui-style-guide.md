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
  rather than printing tags or stripping them: emphasis survives (#57). It also
  turns a written `\n` (backslash, n), which some descriptions hold, into a line
  break, two into a paragraph break (#194).
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

- **A length or time left** on a card, a row or a cover, and an episode's
  details line and "Up next" in the player: `listeningLength`.
  From 10 minutes up it rounds to whole minutes ("29 min", "4 h 12 min", "1 h");
  below 10 it keeps seconds ("9 min 59 s"). Past ten minutes seconds are noise
  (#119, #114).
- **Time left on the screensaver**, glanced at and never ticking: `minutesLeft`.
  Whole minutes with the part minute dropped, as the main screen's rail counts
  them ("18 min", "1 h 5 min"), and "under a minute" below one (#172).
- **The player's running counter**: `PlaybackPosition.spoken`, which keeps
  seconds under an hour, so a short episode visibly moves. Never round it.
- **A stats tile**: `shortDuration` ("181 h", "2 h 11 min"). A real listen
  shorter than a minute says "under a minute" rather than rounding to zero.
- `formatDuration` ("3h 5m") is what the book screen's facts, "Resume at" and
  podcast episode rows still use; new text should use one of the above.

## Animation

An animation that never ends keeps Compose from ever being idle: every UI test
that waits for idle hangs and fails with `AppNotIdleException`, and on the stick
it spends frames on a screen nobody may be watching. So no
`rememberInfiniteTransition` and no `while (true) { delay() }` in composition;
animate only while visible and needed, in runs that end.

Now playing's equaliser (#170) is the pattern: each entry the slot hands over -
one per poll while playing - runs the bars for one poll and no longer, so a
paused or stalled entry comes to rest by itself. While playing it runs on
continuously, by design; a test whose player position moves with the compose
clock must move that clock by hand. Where a drift has to run
for as long as it is shown (`CoverWall`), its tests move the clock by hand.

A label cut by its width may scroll with `basicMarquee` only while its own
item has focus, and for a few runs: pass `iterations`, never an endless count.
The book screen's series cards do this (#194).

## Now playing shows the state

The rail entry and the bar on the book and podcast screens show what *is*:
moving bars while playing, a still ⏸ while paused. The player's own button and
the remote show the *action*, so a ▶ while playing read as the opposite (#170).
Bars only while play is wanted *and* the player is ready or buffering (a seek
does not flash ⏸); an ended player still wants to play, and shows no bars.

At the end of the queue - the newest episode, the end of a book - the session
ends as Stop ends it, and the entry and the bar go (#179). One that goes while
it holds focus first hands focus on, as Stop does (#53). With the player open,
it closes and goes back to where it was opened from - the book's or podcast's
screen, or Home - adding nothing to go Back through.

The open entry and the bar say "Chapter · N min left" in two texts: only the
chapter's name is shortened, and the time left always shows (#198).

The cover is a **circle** inside the progress ring (#177): 36 dp in a 44 dp
ring with a 3 dp stroke, so a 1 dp gap all round. A square's corners ran into
the arc. Placeholders are clipped the same; the state badge stays at the
bottom-right, over the ring.

## Components

Prefer **`androidx.tv.material3`**: its components are built for focus. Reach for
`androidx.compose.material3` only where TV has no equivalent — text fields,
progress indicators, icons.

Most screens predate this rule and still import the phone components; they are
being moved over screen by screen rather than in one sweep.

## Buttons

One action on a screen is the one you most likely want. It is a **filled**
button in `primary`; every other action is **outlined** (1dp
`onSurfaceVariant` border over a 60% `surface` glass, so the label holds over a
backdrop). Neither grows on focus: `ButtonDefaults.scale(focusedScale = 1f)`. The filled one **inverts on focus** — `onPrimary`
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
- **About is its own screen**, not a panel, for a book and an episode alike:
  `AboutScreen` lays it out as the book screen does (`BookOverview`), over the
  same cover wash, drawn over the player so the player keeps its state (#134,
  #137, #178). An episode has its show as the byline and among the facts, and
  no series or Mark finished; with no description, focus lands on the facts.
- **A description box that scrolls fades out at the bottom** while there is
  more below, so it reads as scrollable rather than clipped (#178).
- **The book layout's details sit at the bottom** (#194): the series row and the
  facts at the bottom margin, the description box filling the height above
  them. Its focus border is the box's, drawn outside the fade, not the
  scrolled text's. The title takes three lines; when even that cuts it, the
  description opens with the full title in bold, or with no description the
  full title stands where the box would be.
- **A series row reads as scrollable**: its covers are sized so half the next
  one shows at the right edge (`SeriesRow.coverSize`), and each is labelled
  by its place, "Book 2.5", or its title when the server gives no sequence.
  The focused card reads "Book 2.5 · <title>" and scrolls it (#204).

## The player's look, on every full screen

Every full screen should look like the player: a **cover or the cover wash
behind it, not flat black**. The wash is `CoverWash`: the cover's most colourful
colour, darkened until `onSurfaceVariant` text keeps 4.5:1; a grey cover gives no
wash rather than a muddy one. It is **dark theme only** (`CoverWash.appliesOn`):
under the light theme's dark text it would be a dark blob. Apply it with
`Modifier.coverWash`.

Login does it without a cover to wash from (`LoginBackdrop`, #161): Welcome
back sits on a drifting `CoverWall` of the covers already on the device
(`RecentCovers`, nothing fetched before sign-in); a first run, or no cached
covers, gets a soft field in the theme's own colours.

Known gaps — only the player, `AboutScreen` and Login do this today. Settings,
the library lists (Library, Series, Collections, Authors, Narrators, Latest
Episodes, Switch Library), Home, Stats, Search, the book, podcast, author and
series/collection screens, and the Update screen are still a flat `background`.

## Covers

`CoverImage` draws a cover the way the server's web client does (#149):

- **No cover, or one that failed to load**: a placeholder with the title in the
  middle and the author near the bottom, never an empty box. It is drawn in its
  own tone, `CoverPlaceholderTone` (a darker `surfaceVariant`), not in a focused
  card's fill, or the cover's edge disappears on a focused card (#170). An item that says it
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
| `podcast-actions.html` | Favourites (the Settings choice, the player's heart, the Home shelf), episode row actions, the show's Downloads panel |

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
