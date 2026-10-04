# The player, grown up (#107, #108)

Status: draft for review, 3 October 2026. Mocks: `docs/mocks/player.html`. The frames named here are drawn there, and the page is the visual source of truth.

## Why

The player shows what it is playing and little else, so the controls a long book needs are missing:
- there is no time left in the chapter;
- there is no chapter skip, speed or sleep timer;
- an episode has nothing to say about itself;
- once playback outlives the screen (#91), there is no way back to the player.

The details screen answers "should I play this, and from where?". The player answers "where am I, and how do I steer it?". So the player takes the facts you want while listening, and keeps the material you use to decide behind panels.

## Decided with the maintainer

- **Layout C** for books and episodes: frame C's large cover with everything you read beside it, then full-width bars, then the transport and the actions on one line.
- **Two bars, stacked:** the chapter (the one you seek with) and a slim one for the whole book.
- **Listening controls:** speed and a sleep timer. Skip lengths become a setting.
- **More of what it tells you:** series, narrator, time left in the chapter, and time left in the book at the current speed.
- **About panel:** the description, the series, the facts, and Mark finished.
- **Episodes:** a details line, the first lines of the show notes, previous and next episode, and Up next.
- **What "next" means:** the next newer episode. Audiobookshelf has no server-side play queue; the web player's queue lives only in that browser.
- **Moving on when an episode ends** is a setting, off by default.
- **The player stays full-screen.** A **Now playing** entry in the rail leads back to it.

## The screen

### Layout (frame "C. A on top, B underneath")

- **Top:** a 500 px cover. Beside it:
  - the **series line**: "Series · Book N of M", or the genre for a standalone book;
  - the **title**;
  - **author · read by narrator**;
  - the **chapter name**.
- **Middle:**
  - the **chapter bar**, with the time into the chapter and "M left in chapter";
  - the **book bar**, slimmer, with chapter ticks: "elapsed of total" and "left at <speed>".
- **Bottom line:**
  - the **transport**: previous chapter, back N s, play/pause, forward N s, next chapter;
  - the **actions**: Chapters, Speed <value>, Sleep [<minutes left>], About.
- **Background:** a wash taken from the cover's colours (Palette), falling back to today's surface colour.

### Times

All lengths use `PlaybackPosition.spoken` (#93).
- **"Left in chapter"** is the chapter's end minus the current position, ÷ speed: real listening time, as for the book (decided on #120).
- **"Left at <speed>"** is (total − position) ÷ speed. At 1× it reads just "left".

### Focus and keys

- Focus lands on **play**.
- **Up** from the transport reaches the **chapter bar**. Left and Right there seek in 10 s steps; holding a key speeds up. **Down** returns to the transport.
- **Right from the last transport button** moves into the actions on the same line.
- **Back** closes an open panel first. Otherwise it leaves the player and **keeps playing** (#155, reversing #96): the screen it was opened from shows, and Home's rail carries Now playing. "Go to podcast" keeps playing too.
  - *Known limits of #155, since followed up:* the book and podcast screens had no rail, so nothing there showed what plays or stopped it (#159: a Now playing bar with Stop). With "Play next episode" on, only the player screen queued the episode after next (#160: the playback service queues it after each move).

### Chapter skip

- **Previous chapter:** more than 3 s into the chapter, it goes to the chapter's start; otherwise to the previous chapter.
- **Next chapter:** goes to the next chapter's start, and is disabled in the last chapter.
- **A book without chapters** has no chapter bar or chapter buttons. The single bar is the book, and the outer buttons are hidden, as on a one-file episode without chapters.

### Panels

The same panel slides in from the right, the player stays visible behind it, and Back closes it.

| panel | contents |
|---|---|
| **Chapters** | The book's chapters with start times and the current one marked; choosing one plays from its start. The same component as the details screen's list (#105), built once. |
| **Speed** | 0.75×, 1×, 1.1×, 1.25×, 1.5×, 1.75×, 2×. Remembered **per book or show** on the device; the default is 1×. |
| **Sleep** | Off, End of chapter, 15, 30, 60 min. While it runs, the chip shows the minutes left. When it fires, playback pauses; the closing progress report is sent as on any pause. |
| **About** (frame "A, with About open") | The **description** (formatted, as on the details screen; focus lands on it and Down scrolls it); the **series** as small covers with this book marked, where choosing another asks "Play <title> instead?"; the **facts** (narrator, publisher, year, length, genres), one line each; **Mark finished** / Mark unfinished. |

For an episode, About is the show notes and the publish date. It has no series and no Mark finished.

### Episodes (frame "C, playing an episode")

- **Series slot:** "<show> · <when published>". The date wording is the existing `EpisodeDate`.
- **Details line:** a badge for the episode type when it is not a regular one (Bonus, Trailer), then "Season S · Episode E · <length>". A missing part is left out.
- **Notes:** the episode's `subtitle`, or the plain-text start of its `description`, at most three lines.
- **One bar:** the episode, with "<elapsed>" and "M left of <length>".
- **Previous and next episode** take the outer transport buttons:
  - order is by `publishedAt`, among the episodes the server has audio for;
  - Next on the newest episode, and Previous on the oldest, are disabled.
- **Up next · <title> · <length>** under the transport names what Next plays. It is hidden when there is none.
- **Actions:** Go to podcast, Speed, Sleep, About.

### Moving on when an episode ends (#108)

A setting, **"Play the next episode automatically"**, off by default.

**When on,** the next newer episode is queued after the current one in the player. Media3 then moves on without a gap, and its progress and listening session belong to it. The existing per-item reporting already sends each report to the item it measured. The player screen queues the first one; after that the **playback service** queues the next after each move, with or without a screen (#160), and drops a queued next once the setting is off.

**When off,** an episode that ends stops there.

### Skip lengths

Two settings, **Skip back** and **Skip forward**: 10, 15, 30 or 60 s, defaulting to 30 and 30. The transport icons show the number.

### Now playing in the rail (frames "the rail, closed / open")

- **While something is queued in the playback service,** the rail shows a Now playing entry above the menu, separated from it by a line.
  - **Closed:** the cover thumbnail, a ring for progress through the book (or episode), and the play state.
  - **Open:** the title, and "Chapter · M left" (or "M left" for an episode).
- **Centre** opens the player, which reattaches (#91). **Back** from the player returns to the screen it was opened from.
- **Open, a Stop button sits under the entry** (#155). It ends the listening session - the closing report and session close, as Back used to - and the entry disappears. Focus moves to the selected section.
- **The entry disappears** when nothing is queued: after Stop, a switch to another book, or sign-out.
- **It is not a second set of transport controls.** The remote's play/pause already works everywhere; Stop is the one control, since nothing else ends a session.
- **The rail learns what is playing** from a `MediaController` held by the main screen while it is visible. That is the same connection the player uses, so there is one source of truth.

## Data

Everything comes from what the app already reads, except where marked new.

| fact | source |
|---|---|
| series name, sequence | `media.metadata.series[].name / .sequence` |
| other books in the series, and how many | **new:** `GET /api/libraries/:id/items?filter=series.<base64 id>&sort=sequence`. The series endpoint lists only item ids, and choosing another book needs titles (decided on step 4). |
| narrator, publisher, year, genres, description | `media.metadata.narrators / publisher / publishedYear / genres / description` |
| chapters | `media.chapters` |
| episode season, number, type, subtitle, notes, date, length | `episode.season / episode / episodeType / subtitle / description / publishedAt / audioTrack.duration` |
| mark finished | **new:** `PATCH /api/me/progress/:id` with `isFinished` (episodes: `/:id/:episodeId`) |

## Out of scope

- Bookmarks.
- A queue taken from a server playlist. Possible later: if playback starts from a playlist, Next could follow it, and that works across clients.
- A full mini-player with controls on other screens.

## Delivered in steps

Each step is its own PR, test-first, and checked on the stick before the next one starts.

1. **Layout C and its bars and times.** The new layout, the stacked bars, chapter-relative times, and the five-button transport with chapter skip.
2. **Speed and Sleep.** The panel component and both panels. Speed is remembered per item; the sleep timer runs in the playback service, so it works with the screen closed.
3. **Chapters and About.** The shared chapter list (also #105), About with the series row, and Mark finished.
4. **Episodes.** The details line, the notes, previous and next episode, Up next, and the auto-advance setting (#108).
5. **Settings.** Skip lengths, and the auto-advance row if it is not already in step 4.
6. **Now playing in the rail.**
7. **The cover-coloured background.**

## Testing

- **Pure logic, tested on the JVM:**
  - chapter-relative position and time left;
  - time left at a speed;
  - the previous-chapter rule (the 3 s restart);
  - next and previous episode by date, among episodes with audio;
  - the Up next text;
  - the sleep timer's end time (end of chapter, N minutes);
  - speed remembered per item.
- **Compose UI under Robolectric:**
  - focus on play on arrival;
  - Up reaches the chapter bar, and Right reaches the actions;
  - Back closes a panel before it leaves;
  - chapter buttons are hidden for a book without chapters.
- **The playback service, with a real ExoPlayer:**
  - speed applies;
  - the sleep timer pauses the player;
  - with auto-advance on, the next episode plays after the current one, and its report goes to its own id.
- **Device, on the Fire Stick, each step:**
  - read from the screen and the server;
  - a book with chapters, a book without, an episode;
  - Home, the rail entry, and back.
