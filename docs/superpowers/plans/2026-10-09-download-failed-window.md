# "Download failed" only after a window, not two empty polls (#215) - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** A requested episode reads "Download failed" only when, two minutes
after it was asked for, it is neither queued, downloading nor on the server.
Until then it reads "Requested…", and the podcast is read again at every poll.
An episode that turns up late turns its row downloaded, even one that already
read "failed".

## What the device showed

- 14:45:48.9: the request answered 200.
- 14:45:49.2 and 14:45:54.3: the queue reply was `{"queue":[]}` both times.
  After those two misses the row said "Download failed" and watching stopped.
- 14:45:58.9: the episode was on the server.

## The server (its source, v2.36.0 here; the device ran 2.37.1)

- `currentDownload` is set synchronously in `startPodcastEpisodeDownload`,
  before the request's 200.
- `getDownloadQueueDetails(libraryId)` leaves it out only when its
  `libraryId` is not the library asked about. That is set from
  `libraryItem.libraryId`, so the source does not explain the empty replies.
- Debug builds now log each reply, `DownloadWatch: episode-downloads for …`,
  so the next device run shows what the server said.

## Design

- **`DownloadRequest`** replaces "misses":
  - `Asked(at)`;
  - `Refused`, when the server turned the request down; it fails at once.
- **`DownloadProgress.state(row, queue, request, now)`:**
  - on the server: no state, whatever was said before;
  - downloading or waiting, as the queue says;
  - `Asked` reads Requested until `FAIL_AFTER_MS` (2 min) has passed, then
    Failed.
- **`DownloadWatch`** takes a clock and keeps each request until its episode
  lands. While anything is awaited it reads the podcast again at every poll,
  within its existing one-hour cap.
- **The ViewModel** keeps a refusal across the watch's updates. The old code
  lost it at the next poll.

## Tests (red first)

- **`DownloadProgressTest`:**
  - Requested at 5 s, at 10 s and just inside the window;
  - Failed at the window;
  - Refused fails at once;
  - on the server, no state, even after Failed.
- **`DownloadWatchTest`:**
  - the device's case: empty queues, with the item read at every poll, until
    the episode lands at 10 s;
  - a late arrival after the window is still seen.
- **`PodcastViewModelTest`:**
  - an empty queue 10 s after asking still reads Requested;
  - a refusal stays failed while another download is watched.

## Device check

Download one episode the server lacks. The row goes Requested, then
Downloading or Waiting if the server reports either, then downloaded. It
never reads "failed".
