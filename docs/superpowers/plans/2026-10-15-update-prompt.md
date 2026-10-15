# Update prompt (#229)

Mock: `docs/mocks/update-prompt.html`. A question over Home (Ignore / No / Yes)
replaces the full-screen `UpdateActivity` and its one-day snooze.

## Steps (each test-first)

1. **Ignore replaces snooze.** `SnoozeStore` → `IgnoreStore` (`ignoredVersionCode`,
   `ignore`). `ManifestUpdateChecker.check()` hides a version at or below the
   ignored one; `checkNow()` (Settings) still offers it. No clock any more.
   `PreferencesIgnoreStore` over `UserPreferences.updateIgnoredVersionCode`;
   the snooze fields go, and saving removes the old `update_snooze_*` keys.
   Tests: `ManifestUpdateCheckerTest`, `PreferencesIgnoreStoreTest` (an old
   snooze value is read past and dropped).
2. **Cancel really stops a download.** `UpdateService.downloadApk` checks for
   cancellation in its read loop and rethrows it (it used to swallow every
   exception), leaving `Cancelled` and no file. `Downloading` carries bytes done
   and the total, for "11.4 MB of 27.2 MB". Tests: `UpdateServiceTest`.
3. **The dialog.** `UpdatePrompt` (stateless, in a `Dialog` window so Home's own
   focus requests behind it cannot take focus): question with focus on No, Back
   = No; downloading with a bar, MB of total, percent and Cancel; failed with
   Retry and Close. `UpdateOffer` drives it from an `UpdateDownloads` (the
   service in the app, a fake in tests) and hands a finished APK to
   `onInstall`. Tests: `UpdatePromptTest`.
4. **Wiring.** `MainActivity` holds the offer: the launch check fills it, and
   Settings' Install fills it through a new `MainScreen` callback. The
   installer hand-off (permission, FileProvider, pending-install note) moves to
   `AppUpdates.handOver`. `UpdateActivity`, its manifest entry, its strings
   and the commonmark dependency go.
5. **Docs.** Style guide: Mocks table row; "the Update screen" mentions go.

Out of scope: anything else in the update flow; the launch check still runs only
from `MainActivity`, so never over the player.
