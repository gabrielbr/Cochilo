# Logging flows: how many taps each entry costs

Audited 2026-11-02. The rule: the entries a tired parent logs many times a day must cost one move.
Details (amounts, notes, past times) stay one tap away in the sheet, never in the way.

## Before and after

A "tap" is a touch on a target; a "slide" is one touch that moves sideways and lets go.

| Entry | Before | Now | How |
|---|---|---|---|
| Start / end a sleep | 1 tap / 1 tap | same | Start sleep / Woke up on Today (already one action) |
| Breastfeed, same side as last | 2 taps (Feed, Save) | **1 slide**, or 2 taps | Slide Feed toward the side; the sheet opens on the last side, marked |
| Breastfeed, other side | 3 taps (Feed, side, Save) | **1 slide** | Slide left = left breast, right = right breast |
| Switch side mid-feed | 4 taps (Finish, Feed, side, Save) | **1 slide** | Slide the running feed: this side ends, the other starts |
| End a feed / walk / pump | 1 tap | same | Finish on the running row |
| Diaper, wet | 2 taps (Diaper, Save) | **1 slide** | Diaper row: Wet · Both · Dirty, left to right |
| Diaper, dirty or both | 3 taps | **1 slide** | as above |
| Daylight walk | 2–3 taps | **1 slide** | Outdoors · By a window |
| Routine | 2–3 taps | **1 slide** | Nap routine · Bedtime routine |
| Pumping | 2 taps | **1 slide** | Slide either way: the timer starts |
| Bottle with ml, note, past entry | 4+ taps | same | Tap the row: the sheet, where typing belongs |
| Bottle, same amount as last | 4+ taps | **1 drag** | Log button › Feed › the bottle chip ("120 ml") |
| Medicine, same as last | 3+ taps | **1 drag** | Log button › Medicine › its name |
| Any of the above, from any tab, without scrolling | scroll + the row | **1 drag** | The Log button (below) |
| Undo any of the above | — | 1 tap | Undo in the snackbar that says what was logged |
| First run: add the baby | 1 tap + name + Save | name + **Next**, then **Done** or **Skip** | Two short steps; the look step is skippable ([onboarding.md](onboarding.md)) |
| Log a sleep or wake that began a few minutes ago | open the sheet, edit the time, Save | **1 tap** | "Fell asleep 5 · 10 · 15 min ago" under the action |
| Start sleep / Woke up from the notification shade | — | **1 tap** | The quick-settings tile |
| Start sleep / Woke up from the home screen | open app + 1 tap | **1 tap** | The widget's mint button ([widget.md](widget.md)) |
| Breastfeed a side / switch / finish, from the home screen | open app + 1 move | **1 tap** | The widget's Left / Right; the running side reads Finish |
| Start a daylight walk when nudged | open the Daylight row | **1 tap** | "Go outside" on the nudge row ([night-light.md](night-light.md)) |
| A routine with its steps ticked | — | tap Routine, a tap per step, Save | The checklist in the sheet ([rituals.md](rituals.md)); sliding still logs without it |
| Pick a fabric for the Today card | — | 2 taps | Settings (gear), then a swatch: applied at once, no Save |

## Remember actions (project rule)

**The Last mark (0.10.0).** What was used last isn't a word any more: the option has an outline in
its kind's colour and a small dot of that colour before its name, and the row's or sheet's "last"
line starts with the same dot ("● right breast", "● Right breast · Today 14:20"). The colour ties it
to the kind; the dot reads at a glance, at 3 a.m., without reading. TalkBack still says it ("Right
breast, used last"; "Last time: …"). Replaced the LAST / ÚLTIMO caption and the "last:" prefix.

What was done last is shown and offered first, so the person sees it before choosing, and a repeat
costs the fewest taps. Audit of every entry type (2026-11-02):

| Entry | The log row shows | The slide marks | The sheet opens with | "Last time" line |
|---|---|---|---|---|
| Feed | last breast ("● right breast"), even if a bottle came after | that breast (Last mark) | the last feed's kind: breast side, or bottle with its last amount | ✅ |
| Diaper | last kind | that kind | the last kind | ✅ |
| Daylight | last kind | that kind | the last kind | ✅ |
| Routine | last kind | that kind | the last kind | ✅ |
| Pump | last amount ("last: 90 ml") | (one option) | the last amount | ✅ |
| Medicine | last medicine and when ("Luftal · Today 14:20") | (no slide) | the last medicine's name | ✅ |
| Sleep | (the Sleep panel shows awake/asleep since) | (no slide) | the last half hour, as before | ✅ |
| Note | nothing: notes are one-offs | (no slide) | empty | ✅ |
| Sleep place | asked on Woke up, the last place outlined with a dot (0.28.0) | — | the last place (crib, arms, sling…) | ✅ |
| Feed kind, bottle ml, pump ml, daylight kind | asked when the timer ends if missing, the last answer marked or filled in (0.28.0) | — | the last one | ✅ |
| Widget | the breast used last has the Last mark | — | — | — |
| Fabric (a setting) | the Today card wears it | — | the picker opens with it ringed | — |

Code: `lastOf`, `lastKind`, `lastKindAmong`, `Event.rememberedFrom` in `domain/QuickLog.kt`
(tested in `QuickLogTest`); the Log menu in `LogMenu.kt` (Today's rows until 0.39.0); the sheet's line in `Sheets.kt`.
This replaced the earlier "next: other breast" suggestion: the app shows what happened, she decides.

## Less is more (0.43.0)

The owner: "reduce clutter and less valuable information and actions." Every element went
through the five questions in [`.claude/skills/less-is-more/SKILL.md`](../.claude/skills/less-is-more/SKILL.md);
the full list is in [audits/0.43.0-less-is-more.md](audits/0.43.0-less-is-more.md).

| What | Before | Now | Taps |
|---|---|---|---|
| "Today" title | Always | Gone (the tab and header name it) | 0 |
| Header status on Today | "Awake · 40m" | Hidden on Today (the card says it); shown on Day and Patterns | 0 |
| Sleep card "i" | Always | Gone | 0 |
| How the next nap is guessed | Always open (two meta lines) | "How it's guessed" | +1 to read, 0 to act |
| Day ring caption | Always | Gone | 0 |
| "Hold a sleep and drag" hint | Always | Until the first drag | 0 |
| Daylight nudge in a night waking | Could show | Never | 0 |
| Patterns › Questions about sleep | A row | Gone (the header's chat icon) | 1 (unchanged) |
| Patterns week rows | Total first, 8 rows | Night first, bedtime added, feeds + diapers one row | 0 |

No common entry costs more.

## The night as one thing (0.42.0)

[nights.md](nights.md). Start sleep stays one tap: Cochilo guesses nap or night.

| Flow | Taps |
|---|---|
| Start a nap or the night | 1 (Start sleep; the guess is shown) |
| Correct the guess | 1 ("It's a nap" / "It's the night") |
| A night waking | Woke up (1) |
| Back to sleep | 1 (Today, the notification, the widget) |
| Up for the day | 1 (Today, the notification); asks where she slept, once |

## Share to Cochilo (0.41.0)

From Napper (or any app), Export or Share, then pick **Cochilo** in Android's share sheet: the file
is imported at once and the usual message says what was added. Two taps in the other app, none in
Cochilo; before, it was six steps through Settings. Details: [backup.md](backup.md).

## History on Today, and Ask's chats (0.40.0)

The owner: "move everything from the History page to the recent items on the home page. We don't
need both. Make sure filters are kept." Today's own "Today" timeline and list were the first day
of History, twice. Now Today ends with **Day by day**:
- the filter chips (All, then each kind that has entries) and the legend;
- each day, today first, with its summary, timeline and entries grouped asleep and awake (a plain
  list when filtered).

The History tab is gone: three tabs, Today, Day and Patterns. On a wide screen, Today stays at the
start with Day beside it.

| Flow | Before | Now |
|---|---|---|
| See an earlier day's entries | History tab (1), scroll | scroll Today |
| Show one kind | History tab (1), a chip (1) | a chip on Today (1) |
| Open an entry | 1 tap | 1 tap |
| Ask a new question | the header icon (1) | the header icon (1): it opens a new chat |
| Go back to an old chat | — | the header icon (1), Chats (1), the chat (1) |
| Delete a chat | — | Chats (1), its ✕ (1); Undo (1) |

## Today without the log rows (0.39.0)

The owner: "remove the register items of the home screen; they are not needed there anymore and
are confusing." Since 0.7.0 the Log button logged everything the rows did, from every tab, and
marked what was used last. The rows on Today repeated it, a second way to do the same thing, and
pushed the timeline and today's entries below the fold.

What Today keeps:
- the sleep panel, with Start sleep or Woke up and the Earlier chips;
- the next-sleep card and the daylight nudge;
- running timers, with Finish and the breastfeed's slide to switch side;
- the timeline and today's entries.

| Flow | Before | Now |
|---|---|---|
| Log a breastfeed side | slide on Today's Feed row (1 move) | Log button: press, drag to Feed › Left (1 move), from any tab |
| Log a diaper | slide on the row (1 move) | press, drag to Diaper › Wet (1 move) |
| A kind's full sheet | tap its row (1) | press and let go on the kind, or tap + then the kind (2 taps) |
| What a kind is ("i") | the row's "i" (1) | the sheet's "i" (2 taps away) |

No common entry costs more; it's one move from anywhere instead of from Today only. The empty
state now points to the + ("Start a sleep, or tap + to log a feed or a diaper"). The widget and
the quick-settings tile are unchanged.

## The Log button (0.7.0)

The log rows sat below the first fold of Today: reaching them meant scrolling first, and they
weren't on the other tabs at all. Wunderlist's lesson was to put the main action where the thumb
already is: its big add button sat in the bottom corner, in reach without moving the grip. Most
people hold a phone one-handed with the thumb doing the work, and the bottom of the screen,
towards the thumb's side, is the easy zone (Hoober). So the **Log button** (`ui/kit/LogButton.kt`,
items in `ui/screens/LogMenu.kt`) is a round button in the bottom-right corner of every tab (navy in 0.7.0, Amora since 0.8.0).

**One move, with a cadence:**

1. **Press** the button: the kinds of entry rise in a column above it. The commonest is nearest the
   thumb: Feed, Diaper, Sleep, Medicine, Pump, Daylight, Routine, Note.
2. **Without lifting, drag up**: the kind under the finger lights, with a tick. On a kind with
   choices, they appear beside it, to the left where the thumb can reach: Feed shows
   Left · Right (the last breast has the Last mark) and the last bottle ("120 ml"); Diaper shows
   Wet · Both · Dirty (the last one has the Last mark); Sleep shows Start sleep or Woke up; Medicine shows
   the last medicine's name; Pump shows Start.
3. **Slide sideways onto a choice and let go**: logged at once, with Undo in the snackbar.
   **Let go on the kind** itself: its sheet, opened on the last choice. **Let go anywhere else**:
   nothing happens.

A **tap** opens the same menu to use with taps: tap a kind to show its choices, tap it again for
the sheet, tap a choice to log. TalkBack reads the button ("Log an entry: hold and drag…") and
every kind and choice as a button. The scrim closes it; the button turns into a cross.

The design rules it keeps:

- **The same move each time.** The order never changes, so after a few days the thumb knows that
  Feed › Right is "up a little, then left". This is a marking menu's promise: novices look at the
  menu, experts flick without looking.
- **Remember actions.** The last breast and the last diaper kind have the Last mark; the bottle chip carries
  the last amount; Medicine repeats the last name.
- **Amora, not teal.** The screen's one mint action (Start sleep / Woke up) stays the only teal fill.
  The Log button has its own role, `fab`: Amora, her berry fabric (navy until 0.8.0).
- **The old rows stay.** Today's slide rows still work for whoever has scrolled there; the button
  is the shortcut, not a replacement.

| Entry from the Log button | Moves |
|---|---|
| Breastfeed, either side | 1 drag (press, up to Feed, onto Left or Right, let go) |
| Bottle, same amount | 1 drag |
| Diaper, any kind | 1 drag |
| Start sleep / Woke up | 1 drag (or the panel's one tap) |
| Same medicine again | 1 drag |
| Start pumping | 1 drag |
| Any entry's sheet | 1 drag, letting go on the kind (or tap, tap) |

## The gesture

**Slide to log** (`ui/kit/Slide.kt`, `SlideRow`) comes from *marking menus* (Kurtenbach and Buxton):
the same movement works whether you look or not. When your finger starts sliding, the row turns
into its options side by side, and the one under the finger lights up with a haptic tick. That's
the menu, which teaches beginners. Experienced hands flick without looking; in Kurtenbach and
Buxton's study a mark was about 3.5× faster than choosing from the menu. Letting go logs at once.
Sliding back to where you started cancels.

The rules it follows, from what the research says goes wrong with gestures:

- **Never hidden.** Each row says it can slide ("Slide: Left · Right; next: right"), and tapping
  still opens the full sheet. Swipe gestures without a visible cue mostly go undiscovered (NN/g).
- **Undo, not "Are you sure?".** A slip is taken back from the snackbar in one tap; nothing asks
  first.
- **Accessible.** TalkBack lists each option as an action on the row ("Log Feed · Right breast").
- **One direction, one meaning.** Left is always the left option, right always the right one.
  Swipe is not used for delete anywhere, so it never surprises.
- **Thumb-friendly.** Rows are full width, so the thumb slides along the bottom half of the screen
  where it already rests.

## Earlier, and the quick-settings tile (0.16.0)

**Earlier.** The baby fell asleep in your arms, or woke, before you could reach the phone. Under
Start sleep / Woke up, one short sentence: "Fell asleep [5] [10] [15] min ago" ("Dormiu há … min");
one tap logs the sleep (or the wake) that long ago. Only the chips that make sense are shown: a
sleep never starts before the last wake, and a wake never comes before the sleep began
(`domain/Earlier.kt`, `EarlierTest`). TalkBack reads each in full ("Fell asleep 10 minutes ago").

**Quick-settings tile** "Cochilo sleep" (`tile/SleepTile.kt`): add it to the phone's quick
settings, then Start sleep / Woke up is one tap from the notification shade on any screen. Lit
while the baby sleeps, with "Asleep since 14:50" underneath (Android 10+). It updates whenever the
log changes and goes through `Entries`, like every other button.

## Reading the data (0.21.0)

- **History filters** (on Today's Day by day since 0.40.0): one row of chips, "All" then each kind that has entries
  (Sleep, Feed, Pump, Diaper…). One tap shows only that kind's entries; the day's timeline stays
  whole, so the context is never lost. Days without that kind are left out.
- **The expected window on the ring:** on today's Day ring, while the baby is awake, the predicted
  window for the next sleep is a faint band of the nap colour, under the real sleeps, with
  "Expected next sleep" in the legend and in what TalkBack reads ("Next sleep expected between
  15:51 and 16:11").

## Asleep and awake (0.35.0)

The owner's ask: the timelines should read as the two main states, sleeping and awake, with what
happened during each set in under it. `domain/Stretches.kt` (tested in `StretchesTest`) splits the
day into stretches: the awake time before the first sleep (from the night's end, or "awake" with
no start when the log doesn't know), each sleep, and the awake time after each. Every other entry
goes in the stretch where it started.

- **Day, hour by hour:** newest first. A sleep is a main row as before (its bar, the bigger round,
  drag to adjust). An awake stretch is a main row with a hollow ring on the rail, "Awake" and its
  range and length ("09:10–11:40 · 2h 30m", or "Since 14:20 · 45m" while it runs). Entries inside a
  stretch are set in by `space6`; the time column shows only the stretches' starts, so it reads in
  order, and each nested entry keeps its time in its meta. It replaces the 0.28.0 "Awake 1h 30m"
  line between sleeps.
- **History:** with "All", the day's list is grouped the same way (the ring in the icon slot, nested
  rows set in). Filtered to one kind, it stays a plain list: grouping one kind says nothing.
- **TalkBack:** each awake stretch is a heading, so a stretch can be skipped to.

| Flow | Taps |
|---|---|
| Read what happened during a nap or a waking | 0 (it's set in under it) |
| Edit an entry | 1, as before (awake rows aren't entries and aren't buttons) |

No common entry costs more.

## The Log menu's cascade (0.35.0)

The menu used to rise as one block. Now its rows come in one after another, nearest the button
first, each rising a little and fading in (`motion-emphasised`, `motion-stagger` 35ms apart), and
the column bows in a soft arc (up to `space3`, deepest in the middle), so it feels like a gesture
of the thumb and not a grid. Closing is one sink, all together. The rows are where the finger
expects them as soon as they start to appear, so press-and-drag still works in one move; with
Remove animations they appear at once. `cascadeIn` in `ui/kit/Motion.kt`, `arcOffset` in
`ui/kit/LogButton.kt` (tested in `ArcTest`).

## Notes by age (0.34.0)

No flow changes. Once Tatá approves them, a note sits at the end of Patterns; "Got it" closes it
for that age band in one tap.

## Window reminders (0.33.0)

| Flow | Taps |
|---|---|
| Start the routine from the reminder | 1 (Start routine), from the shade, without opening the app |
| Turn reminders off / pick 30 or 15 min | Settings › Notifications: 1 |

Remembers: the leads chosen. The routine logged is the one for what's next (nap or bedtime).

## The first live Tatá note (0.36.0)

No new flow. Naps in arms is the first note a parent can see. It shows only in Patterns, after the
week's data. Its link is one tap and "Don't show again" is one tap and final. Settings ›
Notifications now shows the "Tatá's suggestions" switch (one tap turns them all off). Nothing
changes on Today, on the Log button or at night.

## Tatá's notes (0.32.0)

No flow changes: a note (when Tatá approves one) sits at the end of Patterns, never on Today or
the Log button. Its link is one tap; "Don't show again" is one tap and final.

## The fabric's shop line (0.31.0)

No logging flow changes. From Settings, the picked fabric's sling in the shop is one tap ("See it
in the shop ›"). It isn't on Today, the Log button, any sheet or the night.

## Getting the on-device AI (0.37.0)

The owner: "the download of the Gemma model is terrible UX; the app should download it when the
user decides to use it." Before, the family build sent her to Hugging Face for a 550 MB file,
behind a sign-in and Google's terms, and then to the file picker. Now the app does it:

| Flow | Taps |
|---|---|
| Get the model, from Ask | ask something beyond the list › "Answer questions like this on the phone" (1) › Download (1) |
| Get the model, from Settings | On-device AI (1) › Download (1) |
| On mobile data | it waits for Wi-Fi; "Download now on mobile data" (1) if she'd rather not wait |
| Stop | Stop the download (1), or cancel it from the notification |

It's Android's download manager: it keeps going with the app closed, shows its progress in the
notification and resumes after a dropped connection. When it arrives it's checked (a `.task`, the
right size, the published SHA-256) before Ask uses it. "Choose a file instead" stays, one tap
further, for a phone with the file already on it. The play build was already one tap (the Play
Store downloads it); it now says when it's waiting for Wi-Fi too.

## On-device AI (0.30.0)

| Flow | Taps |
|---|---|
| Get the model (play) | Settings › On-device AI (1) › Download (1); the Play Store does the rest |
| Get the model (family build) | Settings › On-device AI (1) › Choose the model file (1) › the file (1); since 0.37.0, Download (1) (above) |
| Ask something beyond the list | type, Ask (1); the answer appears when the phone has written it |

Nothing else changes: the topics (14 since 0.38.0) answer at once from the library, as before.

## Ask Cochilo (0.29.0)

| Flow | Taps |
|---|---|
| Open it | the chat icon in the header, from any tab (1, since 0.38.0); or Patterns › Questions about sleep (1) |
| Ask a suggested question | 1 chip |
| Ask your own | type, then Ask (1) |

Nothing on Today or the Log button changes. The suggested questions put what stands out in her
log first, so the likeliest question is one tap. [assistant.md](assistant.md).

## Details after (0.28.0)

Start fast; say what it was once it's over. A timed entry starts with one tap and nothing else;
the one question it still has is asked when it ends (`domain/Details.kt`, `ui/screens/Details.kt`):

| Ended | Asked | Taps |
|---|---|---|
| A sleep (Woke up, the Earlier chips) | "Where did Bebê sleep?" Crib · In arms · Sling · Pram · Car seat | 1 (a place) or 1 (Not now) |
| A feed started with no side | "Which feed was it?" Left · Right · Bottle · Solids; a bottle then asks its ml | 1, or 2 + typing for a bottle |
| A bottle with no amount | "How much was the bottle?" filled with the last bottle's ml | Save (1) |
| Pumping | "How much was pumped?" filled with the last amount | Save (1) |
| Daylight started with no kind | "Where was the daylight?" Outdoors · By a window | 1 |

- **Starting costs less**: Start sleep is one tap with nothing to decide while holding a baby
  who is falling asleep; the sleep panel no longer shows the place chips.
- **Remember actions**: the answer used last is outlined in the kind's colour with a dot (its
  word only spoken: "used last time"); amounts are filled in.
- **Never nags**: asked once per entry (`Prefs.detailsAsked`); "Not now" or a swipe down leaves
  it as it is, still editable from its sheet. Something ended outside the app (widget,
  notification, tile) is asked the next time the app opens, only within two hours.

## Timeline order (0.28.0)

The Day tab's hour-by-hour timeline lists the newest first, like History: what just happened is
at the top. (Since 0.35.0 the awake time is its own row; see "Asleep and awake".)

## Drag to adjust (0.27.0)

| Flow | Before | Now |
|---|---|---|
| "She woke 15 minutes later than I tapped" | open the sleep, the end time, pick, OK, Save (5 taps) | hold the sleep on Day's timeline, drag down three steps, let go (one move) |
| Undo it | open and edit again | Undo on the snackbar (1 tap) |

- Only a finished sleep moves, by its end, in 5-minute steps (12dp each). It never ends in the
  future, never runs into the next sleep, and never gets shorter than 5 minutes (`domain/Adjust.kt`).
- While held, the row turns blush, the bar grows or shrinks with the drag, and the line under the
  name says the new end ("Woke up 6:55 AM · +15m"), with a light tick at each step.
- A tap still opens the sheet, so nothing is lost for those who don't drag. A line above the
  timeline says how ("Hold a sleep and drag to change when it ended").
- TalkBack: two actions on each finished sleep, "Ended 5 minutes later" and "Ended 5 minutes sooner".

## Tablets and foldables (0.25.0)

Phones: no change. Expanded (840dp and up): Today stays on screen beside the picked tab, so Start
sleep / Woke up and the Earlier chips cost one tap from History or Patterns too, instead of a tab
switch plus the tap. The Log button keeps its one move on every width.
[tablet.md](tablet.md).

## The last nap in the prediction (0.24.0)

No tap changes. The prediction card adds one line, only when the last nap moved the window by 5
minutes or more, so it is read, not acted on. [prediction.md](prediction.md).

## Locked backups (0.23.0)

| Flow | Taps | Remembers |
|---|---|---|
| Export a backup | 1 (Export backup) + the system's save | the passphrase, so a locked export costs the same as a plain one |
| Import a locked backup on the same phone | 1 + the system's picker | the passphrase opens it with no question |
| Import it on another phone | 1 + picker + type the passphrase + Open and import | nothing (by design: the passphrase is the key) |
| Set or change the passphrase | Protect backups, type, Save | the current passphrase is filled in |

A wrong passphrase keeps the sheet open with "That passphrase doesn't open it. Try again." and the
words still typed, so fixing a typo costs one tap.

## Large text (0.22.0)

No tap counts change. At 200% font size every one-move entry keeps its cost: the Log button's
choices wrap to a second line instead of losing the last one (Dirty), and the Earlier chips wrap
between chips, so "Fell asleep 15 min ago" is still one tap. [accessibility.md](accessibility.md).

## Motion (0.10.0)

Every action answers with a small movement, so the hand knows it worked without looking twice
(`ui/kit/Motion.kt`, design system "Motion"):

| Action | Motion |
|---|---|
| Any press (rows, buttons, chips, tabs, swatches, Log menu rows) | dips to 97% under the finger |
| Open the Log button | the scrim fades in, the menu's rows come in one by one on a soft arc (0.35.0), the plus turns into a cross; held, the button dips |
| Drag over the Log menu or a slide row | the lit row or option cross-fades to its fill |
| Start a slide | the options fade in over the row, and out when it lets go |
| Log something | the list opens for the new entry instead of jumping |
| Start sleep / Woke up | the card cross-fades to the night panel and back |
| Switch tab, pick a chip, pick a fabric | the pill, chip or ring cross-fades; a new fabric cross-fades in |

Two durations only (`motion-standard` 150ms, `motion-emphasised` 250ms) and the system's easing.
With Remove animations on, all of it snaps. None of it adds a tap or a wait.

## Sleep leads (0.10.0)

Sleep is what Cochilo is for, so it's a size up wherever kinds are listed: a 44dp round
(`kind-badge-lead`, the others 36dp) and its name in the label role (bold) in History, Today's
lists, the Day tab's hour by hour (with a wider bar), the log rows and the Log menu. The 24-hour
bars and the ring already give sleep the full height.

## Smart defaults

- See "Remember actions" above: every sheet opens on what was done last.

## Next candidates (not built yet)

The full plan is [roadmap.md](roadmap.md).


- (Shipped: the widget in 0.11.0, Earlier and the tile in 0.16.0.)

## Sources

- Kurtenbach, G. and Buxton, W. *User learning and performance with marking menus*, CHI 1994:
  [billbuxton.com/MMUserLearn.html](https://billbuxton.com/MMUserLearn.html)
- Kurtenbach and Buxton, *The limits of expert performance using hierarchic marking menus*:
  [research.autodesk.com](https://research.autodesk.com/app/uploads/2023/03/the-limits-of-expert.pdf_recyw5wL6ekPqcE4Q.pdf)
- Li et al., *M3 Gesture Menu* (recall-based gesture use after short practice):
  [research.google.com](https://research.google.com/pubs/archive/46567.pdf)
- Nielsen Norman Group, *Contextual swipe*:
  [nngroup.com/articles/contextual-swipe](https://www.nngroup.com/articles/contextual-swipe/)
- Hoober, S. *The thumb zone* (Smashing Magazine, 2016):
  [smashingmagazine.com](https://www.smashingmagazine.com/2016/09/the-thumb-zone-designing-for-mobile-users)
- Wunderlist's design (the big add button in the thumb's corner, the main action always in
  reach): [6wunderkinder on Medium](https://medium.com/@6wunderkinder) and
  [Material Design, floating action button](https://m3.material.io/components/floating-action-button/overview)
- Material Design, *FAB menu* (speed dial: the main button opens related actions above it):
  [m3.material.io](https://m3.material.io/components/fab-menu/overview)
- Android gesture patterns (long press is for selection, not menus):
  [stuff.mit.edu mirror of the Android design docs](https://stuff.mit.edu/afs/sipb/project/android/docs/design/patterns/gestures.html)
