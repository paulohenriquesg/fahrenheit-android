# The resume prompt's Stay border (#209) - Implementation Plan

**Goal:** The Stay answer's outline follows its own 16 dp corners, unfocused and focused, and the focused answer's growth never makes the two answers touch.

**Architecture:**
- `ResumeChoice` keeps one `RoundedCornerShape(16.dp)` for both answers and gives the `OutlinedButton` a `border` built on it: the library's default strokes and theme colours (1.5 dp `onSurfaceVariant` at 40 %, focused 1.65 dp `onSurfaceVariant`, pressed 1.5 dp, disabled and focused-disabled at 20 %), only the shape changed.
- The answers' gap grows from 12 dp to 24 dp: a focused TV button grows by a tenth, about 13 dp at this width, more than 12.
- The app's only other `OutlinedButton` (the Cancel of the book screen's question) keeps the library's shape for both container and border, so it already matches.

## Tasks

1. **Red, then green - the outline** (`ResumeChoiceLookTest`, drawn pixels): unfocused, a point on the 16 dp corner's arc carries the outline (a pill leaves it bare); focused, a point on a pill's arc is the container's colour (no stray outline across it).
2. **Red, then green - the gap** (`ResumeChoiceTest`): with one answer grown by a tenth, at least 8 dp is left before the other.
3. **Gate, review, PR** (Closes #209).

## Device check

- The prompt (another device further on), Continue focused: Stay's outline follows its corners; the answers don't touch.
- Stay focused: its outline follows its corners; the answers don't touch. Screenshot of each.
