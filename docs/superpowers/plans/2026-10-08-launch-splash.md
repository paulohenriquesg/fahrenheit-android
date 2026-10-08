# Launch splash and session check (#191)

What happens today: LoginActivity reads the stored host and token and, if both
are there, starts MainActivity at once. It sends nothing to the server, so
nothing is refreshed there. An expired access token is refreshed later, by the
401 authenticator, on Home's first request. The black stretch is the window
background (`#121212`) until LoginActivity starts, then until MainActivity
draws, and after that Home is empty until the libraries and shelves arrive.

## Steps

1. **Pin first** (watched red):
   - `SessionCheck`: the probe answering means Ready. No answer (IOException)
     means the sign-in form with `Unreachable`. 401/403 (the refresh failed too)
     means the form with no error, since Welcome back asks for the password
     anyway. Any other HTTP code means `ServerError(code)`, anything else
     `Unexpected`.
   - `LaunchScreen`: shows the app name and "Signing in…", and "Connecting to
     your server…" once 3 s have passed. The stored server address appears
     nowhere on it.
   - `LaunchGate`: while the check runs it shows the launch screen, not the
     form. When the check says Ready it goes Home once and never draws the
     form. When it says sign in, it draws the form and passes the error on.
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
  session. In exchange, any refresh happens on the launch screen, and with an
  unreachable server you get the sign-in form instead of an empty Home.
- An unreachable server waits out the client's 15 s connect timeout, with
  "Connecting to your server…" on screen the whole time.
- The stored session is kept when the check fails, so the next launch tries
  again.
- The icon is still the template launcher icon. Replacing it is a separate
  job.
