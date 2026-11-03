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
(tested in `QuickLogTest`); the rows in `Today.kt` (`QuickLog`); the sheet's line in `Sheets.kt`.
This replaced the earlier "next: other breast" suggestion: the app shows what happened, she decides.

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

- **History filters:** one row of chips under the title, "All" then each kind that has entries
  (Sleep, Feed, Pump, Diaper…). One tap shows only that kind's entries; the day's timeline stays
  whole, so the context is never lost. Days without that kind are left out.
- **The expected window on the ring:** on today's Day ring, while the baby is awake, the predicted
  window for the next sleep is a faint band of the nap colour, under the real sleeps, with
  "Expected next sleep" in the legend and in what TalkBack reads ("Next sleep expected between
  15:51 and 16:11").

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
at the top, and "Awake 1h 30m" sits between a sleep and the newer one above it.

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
| Open the Log button | the scrim fades in, the menu rises from the button, the plus turns into a cross; held, the button dips |
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
