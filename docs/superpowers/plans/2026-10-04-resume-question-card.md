# The "continue from where?" card (#158)

Redesign of the question from #90/#142, to `docs/mocks/resume-question.html`.
The rule, the two answers, focus on the newer position, Back = Stay and
"nothing plays until a choice" stay as they are: `ResumePrompt` and the
wiring in `PlayerActivity` keep their behaviour.

## Steps (each test first)

1. **The device's name.** `ItemListeningSessions.latest()` also reads the
   session's `deviceInfo.deviceName`, else `clientName`, into
   `LatestSession.deviceName`. (`ItemListeningSessionsTest`)
2. **Which device moved it.** `ResumeOffer.of` sets `ResumeOffer.device` to
   that name when the latest session is another device's and as recent as the
   server's copy (the slack the same-device check uses); otherwise null, which
   the card says as "elsewhere" (no session behind the newer position, or
   sessions unreadable). (`ResumeOfferTest`)
3. **The wording**, as a plain function of the offer, the item (`ResumeItem`:
   title, length, chapter spans, book or episode) and now (`ResumeWording`):
   - headline: "You listened further on your iPhone" / "This book|episode
     moved on elsewhere", and "went back" when the newer position is behind;
   - under it: "10 minutes ago · 1 h 20 min there, 1 h 05 min here";
   - books name chapters on the buttons and marks ("Continue from Chapter 12",
     detail "1 h 20 min · where the iPhone left off"); episodes, books without
     chapters, and two positions in one chapter use times.
   (`ResumeChoiceTest`)
4. **The card**: cover, headline, a timeline with "here" and "there" marks
   placed by position over the item's length, and two buttons at least as tall
   as the transport's Play. Over the dimmed player (its wash stays behind).
   Compose tests: marks placed by position; focus, centre, Stay, D-pad held
   and Back = Stay unchanged. (`ResumeChoiceTest`)
5. **Wiring**: `PlayerActivity` passes the `ResumeItem`; the old one-sentence
   string goes.

"Back stops" is not pinned by any test of the question: Back on the question
has always meant Stay, and Back from the player keeps playing since #155.

No device here: the device checks go in the PR.
