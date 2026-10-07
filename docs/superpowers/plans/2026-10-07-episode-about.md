# An episode's About in the full-screen layout (#178) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** An episode's About from the player is the same full-screen `AboutScreen` a book's is (#134), not the old 430 dp `AboutPanel`, whose description box clipped mid-line with no fade and left half the panel empty. Art and episode title on the left with the show as the byline; the tall description box that Down scrolls and that fades out at the bottom while there is more; the facts as the two-column list. No series row and no Mark finished.

**Architecture:**
- **`AboutFacts.episode(show, published, length, season, episode)`**: Show, Published, Length, then Season and Episode when the feed gives them. New `AboutFact.Kind`s `Show`, `Season`, `Episode`.
- **`NowPlaying.show`**: an episode's show (null for a book), which `AboutScreen` uses as the byline when there is none. `byline` itself stays null for an episode: the player draws it under the title, and the kicker already names the show there.
- **`BookOverview`**: the description box fades out at its bottom while it can scroll further (a `DstIn` gradient over the box's last 48 dp); the state is a semantics property, `DescriptionFadesOut`, so tests can read it. A new `factsFocus` lets the caller land focus on the facts.
- **`AboutScreen`**: with no description and no Mark finished (an episode), focus lands on the facts.
- **`PlayerActivity`**: About is always `AboutScreen`; the `episodeId == null` branch goes.
- **`AboutPanel`** has no other caller: the composable, `FactLine` and `Landing` go, with `AboutPanelTest`. The facts and `AboutChip` stay, in `AboutFacts.kt`. Behaviour the panel tests covered that still matters (no Mark finished for an episode, Down scrolls a long description) is tested on `AboutScreen` / `BookOverview`.

## Rulings

1. **The fade applies to the book's About and the book screen too**: it is one layout, and the same clipped-looking box would read as broken there as well.
2. **Facts order:** Show, Published, Length, Season, Episode (the issue's order).
3. **The show is both byline and a fact,** as the issue asks.

## Tasks

1. **Red:**
   - `AboutFactsTest`: an episode's facts are Show, Published, Length, Season, Episode; missing ones left out.
   - `NowPlayingTest`: an episode carries its show, its facts include the show and length; a book has no show.
   - `AboutScreenTest`: an episode opens the full-screen About with the show and length facts and no Mark finished; focus lands on the description; with no description focus lands on the facts; Back returns to the About chip.
   - `BookOverviewTest`: a long description fades out at the bottom, a short one does not, and stops fading once scrolled to its end.
2. **Green:** the facts, `NowPlaying`, the fade, the facts' landing, `PlayerActivity`.
3. Remove `AboutPanel` and its test; update docs/ui-style-guide.md.
4. **Gate, review, PR** (Closes #178).

## Device check (the coordinator)

- **An episode's About** from the player: full screen over the wash, art and title on the left with the show under it, the description box tall and fading at the bottom when long, Down scrolls it; the facts list Show, Published, Length (and season/episode where the feed has them).
- **An episode with no description:** focus lands on the facts; Back returns to the player on the About chip.
- **A book's About and book screen:** a long description fades at the bottom, and the fade goes when scrolled to the end.
