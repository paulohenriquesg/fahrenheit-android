# Book page: descriptions, series labels, the full title, the details at the bottom (#194) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** On the book screen and About (`BookOverview`): a literal `\n` in a description is a line break; series cards are labelled by their place in the series; the series row shows part of the next cover and Right reaches every book; the full title is readable; the description's focus border is drawn whole; the series and the facts sit at the bottom of the screen and the description fills the space above them.

**Architecture:**
- **`RichText.fromHtml`**: before rendering, a literal `\r\n` or `\n` (backslash, n) becomes `<br>`; two in a row are then a blank line (a paragraph break). Real HTML is untouched.
- **`SeriesBook.sequence`**: from the item's series (the server sends the filtered series with its sequence). `SeriesBook.label(book)`: "Book 2.5" from the sequence, the title without one.
- **`SeriesShelf`**: each card's label is that label. The cover size comes from the row's width (`SeriesRow.coverSize`): the largest number of whole covers that still leaves half a cover showing at the right edge, so the row reads as scrollable.
- **`BookOverview`**: the right column fills the height; the description box takes `weight(1f)` (no 230 dp cap), the lower section (series + facts, tagged `BOOK_LOWER_TAG`) sits at the bottom; with no description a spacer takes its place. The focus border moves from the scrolled text to the box itself, drawn outside the fade layer, so it is never scrolled or faded away.
- **Title**: three lines. When it is still cut, the description box opens with the full title as its heading (the full title is then on the page, where the most room is).
- **Genres**: `DetailHeaderModel.book` skips blank genres too; `AboutFacts.book` pinned for empty and blank.
- **Optional marquee**: a focused card whose label is cut scrolls it with `basicMarquee(iterations = 3)`; only while focused, finite.

## Tasks

1. **Red:** `RichTextTest` (`\n`, `\n\n`, `\r\n`, HTML unchanged); `SeriesBooksTest` (sequence carried; labels); `SeriesRowTest` (cover size leaves a peek); `AboutFactsTest`/`DetailHeaderModelTest` (empty and blank genres); `BookOverviewTest` (lower section at the bottom and description above it, with and without series and description; Right reaches the last book; the focus border is whole at the bottom of a long description; a cut title is shown in full in the description box; series labels).
2. **Green**, task by task.
3. Optional marquee, test-first.
4. docs/ui-style-guide.md, gate, review, PR (Closes #194).

## Device check (the coordinator)

- A long description shows many more lines than before, fades at the bottom; the focus border is whole.
- A description holding `\n` shows line and paragraph breaks, not the characters.
- Series cards read "Book 1", "Book 2"…; part of the next cover shows at the right; Right reaches the last book.
- A long title shows on three lines and, when still cut, in full at the top of the description.
- Series and facts sit at the bottom margin; a book with no series still has its facts at the bottom.
