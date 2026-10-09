# Series row: the focused card shows "Book N · title" (#204) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** "Book 1" is never cut, so #194's marquee never ran and a card's title was nowhere on the row. The focused card reads "Book N · <title>", which the existing finite marquee scrolls; unfocused cards keep "Book N" (or the title with no sequence).

**Architecture:** `SeriesShelf` picks the label from the card's focus: focused with a number, `R.string.series_book_number_titled` ("Book %1$s · %2$s"); otherwise as now. The marquee is unchanged (focused card only, `iterations = 3`). `BookOverview` serves the book screen and About alike.

## Tasks

1. **Red:** `BookOverviewTest`: the focused card's label holds the number and the title, an unfocused one only "Book N"; with no sequence the focused label is the title. The marquee test stays.
2. **Green**, style guide line, gate, review, PR (Closes #204).

## Device check

- Focus a series card: its label scrolls through "Book N · title" a few times and rests; the others read "Book N".
