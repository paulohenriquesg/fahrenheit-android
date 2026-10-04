# Login in the app's look (#161)

Mock: `docs/mocks/login-style.html`. Look only: every field, button, rule, error
and focus order stays, and the login tests that pin them stay green unchanged.

## Steps

1. **Pin first** (watched red):
   - Welcome back with no cached covers, or no loader, draws the colour field;
     with cached covers it draws the cover wall. First run always draws the
     field and never asks the loader.
   - `RecentCovers` remembers the covers drawn, per server, newest first,
     capped; `cachedCovers` returns only what Coil already has on the device
     and sends no request.
   - The account card's avatar shows the username's initial.
   - No login button and no server row grows on focus.
   - "Fahrenheit" sits above the title on every step.
2. **Build**: `CoverWall` (ui/elements, reusable by the screensaver, #156),
   `RecentCovers` noted from `CoverImage` on a successful load,
   `LoginBackdrop` (wall + left shade, or the colour field), the left column
   with the brand line, the round avatar, `ButtonDefaults.scale(1f)` and
   `ListItemDefaults.scale(1f)`.
3. **Fit**: the keyboard tests (everything above 55% of 540dp, with an error
   showing) decide how large the type can get.

## Limits

- The wall only drifts while there are covers; a screen with no covers has no
  endless animation (tests would never idle).
- Covers are only as cached as the server's headers allow Coil to keep them;
  with none cached, Welcome back shows the colour field. Device check decides.
