# Mocks

Design drafts for screens, drawn at a real 1920x1080 and shown at half scale.
Open one in the Orca editor with `orca file open docs/mocks/login.html`, or in a
browser.

They use the app's own palette from `ui/theme/Theme.kt` and the constraints in
`../ui-style-guide.md`, so what they show is what the device can render. Every
frame marks which numbers are real and which are samples.

- `login.html` — sign-in, built around the Fire TV keyboard owning the bottom
  45% of the screen. Server discovery, a returning user, error states, light.
- `screens.html` — the navigation rail (#58), Home, item detail (#57), the
  player, search.
- `stats.html` — the stats screen, before and after (#54, shipped in #60).
