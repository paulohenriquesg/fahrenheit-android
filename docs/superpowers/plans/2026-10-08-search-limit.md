# Search asks for 50 per kind and says when a list was cut (#193) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Search asks the server for 50 matches of each kind instead of 10, and a kind that comes back with exactly the limit says so under its row: "Showing the first 50. Add a word to narrow it." The server has no offset for search, so this is the honest answer, not paging; nothing fetches the whole library.

**Architecture:**
- **`BrowseRepository.SEARCH_LIMIT = 50`**, passed explicitly to `BrowseApi.searchLibraryItems` (whose default goes to 50 as well, so no other caller asks for 10).
- **`SearchResults`** gains `itemsCut` / `authorsCut`, each true when that kind came back with at least the limit. Items are judged on the raw match count, before matches with no library item are dropped, since that is what the server cut.
- **`SearchContent`**: `ResultGroup` takes `cut`; when true, a line under the row in `bodySmall` `onSurfaceVariant` (secondary text, per the style guide). The rows stay `LazyRow`s with stable keys.

## Rulings

1. Only the kinds the screen shows (books/podcasts and authors) are judged; series, narrators, tags and genres are not drawn on this screen.
2. The line sits under the row, as the issue says, not in the header.

## Tasks

1. **Red:**
   - `BrowseRepositoryTest`: the search asks for 50; 50 items mark items cut, 49 do not; 50 authors mark authors cut and leave items alone (each kind on its own).
   - `SearchContentTest`: a cut kind shows the line under its group; an uncut one does not; with items cut and authors not, the line appears once.
2. **Green:** the limit, the flags, the line.
3. **Gate, review, PR** (Closes #193).

## Device check (the coordinator)

- A broad one-letter search on the stick: sections that hit 50 show "Showing the first 50. Add a word to narrow it." under their row; a narrow search shows no such line.
- D-pad right through a 50-item row moves without jank, and Down moves between groups.
