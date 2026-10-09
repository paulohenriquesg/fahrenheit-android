# Launch splash and session check (#191)

What happens today: LoginActivity reads the stored host and token and, if both
are there, starts MainActivity at once. It sends nothing to the server, so
nothing is refreshed there. An expired access token is refreshed later, by the
401 authenticator, on Home's first request. The black stretch is the window
background (`#121212`) until LoginActivity starts, then until MainActivity
draws, and after that Home is empty until the libraries and shelves arrive.

## Steps

1. **Pin first** (watched red):
   - `SessionCheck`: the probe answering means Ready. Only 401/403 (the
     refresh token spent too) means Rejected. Everything else is Unreachable:
     no answer, no answer within 10 s, a 5xx, or an unexpected error. A TV just
     woken often has no Wi-Fi for its first seconds, and none of these is a
     reason to ask for the password.
   - `LaunchScreen`: shows the app name and "Signing in…", and "Connecting to
     your server…" once 3 s have passed. The stored server address appears
     nowhere on it.
   - `LaunchGate`: while the check runs it shows the launch screen, not the
     form. When the check says Ready it goes Home once and never draws the
     form. Rejected draws the sign-in form. Unreachable stays on the launch
     screen with "Can't reach your server" and a focused Try again, and
     checks again on its own after 2, 4 and 8 s, then waits for a press. A
     press checks at once and starts those waits over. The stored session is
     never touched.
     With no stored session there is no check and the form shows straight away.
   - `StartupTimeline`: each step is logged once, in ms since the process
     started.
2. **Build**: `core-splashscreen` with a `Theme.Fahrenheit.Starting` on
   LoginActivity (the icon on `#121212`), and `installSplashScreen()`. LoginActivity
   runs the gate with `GET api/me` on the authenticated client, so an expired
   token is refreshed here rather than on Home's first request. Timeline marks
   go at app start, login start, check start and end (with whether the token
   changed), main start, libraries loaded and first shelves loaded.
3. **Report**: what the tests and the code can say about the timings. The
   numbers from the stick are a device check.

## Limits

- The check adds one round trip (`api/me`) to every cold start that has a
  session. In exchange, any refresh happens on the launch screen, and an
  unreachable server shows a launch screen that says so and keeps trying,
  instead of an empty Home.
- The check gives up after 10 s and counts as unreachable. Without that
  limit, the client's own timeouts (15 s to connect, 60 s to read) could keep
  the launch screen up for a minute.
- The automatic retries stop after the 8 s wait (four checks in about
  14 s plus the checks' own time), so a server that stays away leaves an
  idle screen waiting for Try again rather than a loop that never ends.
- The icon is still the template launcher icon. Replacing it is a separate
  job.
