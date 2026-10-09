# The podcast page in the book page's layout (#205), its state in a ViewModel (#208) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** A podcast's screen laid out as a book's: a fixed left column (cover,
title, "Podcast · genre", stacked buttons, facts at its foot) and a right column
that scrolls (the description as a card, the tabs that stick, the episodes).
The screen's state moves out of `DetailActivity` into a `PodcastViewModel`
with an immutable `PodcastUiState` (#208).

## Layout

- **Left column**, 240dp (first built at the book's 180dp; on the stick the
  actions wrapped to three lines and Resume cut the episode's name, so it
  widened to the mock's), 40dp from the right one:
  - the cover, the title (`headlineSmall`, two lines), "Podcast · genre";
  - then, stacked as the book's actions are (TV `Button`):
    - **Resume <episode>** or **Play newest episode**, which takes focus on arrival;
    - **Check for new episodes** (admins), its state as a second line: "Up to 3
      to the server", then checking, found or failed. This replaces the hint
      beside today's button;
    - **Automatic downloads** (users with the update right, #182), with "On ·
      every day" or "Off" as its second line. It opens the Downloads panel.
  - At the foot of the column: "N of M on the server" and "Feed checked <date>".
- **Right column**, one lazy list:
  - Now playing, then the description card, then the note when the list is not
    the whole story;
  - the tabs, pinned once the card has scrolled away;
  - the rows.
- **The description card** shows four lines and fades out when there is more.
  Center opens the whole description full screen, in the book layout
  (`BookOverview`). Back closes it and returns focus to the card.
- **Rows** change only:
  - ▶ shows only on the focused row. Its slot is kept, so nothing moves;
  - the line reads "date · length · time left".
  The rest stays (tick, dimming, Mark finished, download words), because the
  favourites lane is adding a heart to these rows at the same time.
- **Left from the right column lands on the primary button.** The right column
  is a focus group whose Left exit goes there.

## Rulings

1. **The cover gives way, never a button.** Three buttons, two of them two
   lines high, under a long title do not fit 508dp. So the cover takes what is
   left, up to the column's width, and every button and the facts stay on screen. A test at
   font scale 1.3 checks this.
2. **The schedule stays visible without the button.** The Automatic downloads
   button is only for users the server would take a change from (#182). Anyone
   else gets the state as a third fact at the foot of the column, so the
   information the header used to give is not lost.
3. **The title is no longer pinned over the list.** The left column always
   shows the show's name.
4. **`DetailHeader` and `DetailHeaderModel.podcast`/`previewOf` go.** The
   podcast page was their only user; the primary label moves to the ViewModel.

## The ViewModel (#208)

`PodcastViewModel(itemId, item, podcastApi, settingsApi, reloadItem,
markFinished, now, serverFormat, favourites, scope)` holds
`StateFlow<PodcastUiState>`. `favourites` is the library's Favourites playlist
(#211), which reached main during this work: its hearts and its tab move in too.

- **State** covers:
  - the item and its progress (from `GET /api/me`, until the progress store
    of #207 exists);
  - the feed, the tab, the download queue, the feed check, the marks and the
    show's download settings.
- **Events:**
  - `chooseTab`, `refresh` (on returning to the screen), `mark`, `download`,
    `checkFeed`, `changeDownloads`, `downloadsSeen`, `markFailureShown`.
- **Composables:** `PodcastPage` is stateless apart from what belongs to the
  UI: the panel and the full description being open, and focus.
- **Wiring:** `DetailActivity` builds the ViewModel and does the navigation
  (play intents, the toast).
- **Tests:** they use `Dispatchers.Unconfined`, since the project has no
  `kotlinx-coroutines-test`, with fake `PodcastApi`/`PodcastSettingsApi`.

## Tasks

1. **Red:**
   - `PodcastViewModelTest`:
     - progress and resume from me;
     - the feed is read for an admin only;
     - a mark shows at once, then is read again, and a refused mark goes back
       and says so;
     - a tab;
     - a download request;
     - the feed check's states;
     - the download settings offered by the right;
     - the primary label;
     - facts with and without the button.
   - `PodcastPageTest`:
     - the left column keeps its position while the list scrolls;
     - the facts are at the column's foot;
     - Left from a row lands on Resume;
     - the buttons stay on screen with a long title at 1.3;
     - Downloads opens the panel and closing it returns focus;
     - Center on the description opens it full screen and Back returns.
   - `PodcastEpisodesViewTest`:
     - ▶ only on the focused row;
     - an unfocused finished row is dimmed, a focused one is not;
     - the tabs stay pinned.
   - `PodcastScreenModelTest`: "Feed checked <date>".
2. **Green**, then wire `DetailActivity`.
3. **Gate, review, PR** (Closes #205).

## Device check (the coordinator)

- A show with finished and not-downloaded episodes, as an admin. Take a
  screenshot, and check that:
  - the left column stays put while the list scrolls;
  - the tabs pin;
  - ▶ shows only on the focused row, and finished rows are dimmed until focused;
  - Right on a row reaches Mark finished;
  - Left from any row lands on Resume.
- Check for new episodes shows its state in its second line.
- Automatic downloads opens the panel, and its second line follows a change.
- Center on the description opens it full screen, and Back returns to the card.
- As a user without update rights: no Automatic downloads button, and the
  state is listed among the facts.
- Finish an episode in the player and go Back: the row shows it finished.
