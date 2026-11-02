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

## Remember actions (project rule)

What was done last is shown and offered first, so the person sees it before choosing, and a repeat
costs the fewest taps. Audit of every entry type (2026-11-02):

| Entry | The log row shows | The slide marks | The sheet opens with | "Last time" line |
|---|---|---|---|---|
| Feed | last breast ("last: right breast"), even if a bottle came after | that breast (LAST) | the last feed's kind: breast side, or bottle with its last amount | ✅ |
| Diaper | last kind | that kind | the last kind | ✅ |
| Daylight | last kind | that kind | the last kind | ✅ |
| Routine | last kind | that kind | the last kind | ✅ |
| Pump | last amount ("last: 90 ml") | (one option) | the last amount | ✅ |
| Medicine | last medicine and when ("Luftal · Today 14:20") | (no slide) | the last medicine's name | ✅ |
| Sleep | (the Sleep panel shows awake/asleep since) | (no slide) | the last half hour, as before | ✅ |
| Note | nothing: notes are one-offs | (no slide) | empty | ✅ |

Code: `lastOf`, `lastKind`, `lastKindAmong`, `Event.rememberedFrom` in `domain/QuickLog.kt`
(tested in `QuickLogTest`); the rows in `Today.kt` (`QuickLog`); the sheet's line in `Sheets.kt`.
This replaced the earlier "next: other breast" suggestion: the app shows what happened, she decides.

## The Log button (0.7.0)

The log rows sat below the first fold of Today: reaching them meant scrolling first, and they
weren't on the other tabs at all. Wunderlist's lesson was to put the main action where the thumb
already is: its big add button sat in the bottom corner, in reach without moving the grip. Most
people hold a phone one-handed with the thumb doing the work, and the bottom of the screen,
towards the thumb's side, is the easy zone (Hoober). So the **Log button** (`ui/kit/LogButton.kt`,
items in `ui/screens/LogMenu.kt`) is a navy circle in the bottom-right corner of every tab.

**One move, with a cadence:**

1. **Press** the button: the kinds of entry rise in a column above it. The commonest is nearest the
   thumb: Feed, Diaper, Sleep, Medicine, Pump, Daylight, Routine, Note.
2. **Without lifting, drag up**: the kind under the finger lights, with a tick. On a kind with
   choices, they appear beside it, to the left where the thumb can reach: Feed shows
   Left · Right (the last breast says LAST) and the last bottle ("120 ml"); Diaper shows
   Wet · Both · Dirty (the last one says LAST); Sleep shows Start sleep or Woke up; Medicine shows
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
- **Remember actions.** The last breast and the last diaper kind say LAST; the bottle chip carries
  the last amount; Medicine repeats the last name.
- **Navy, not teal.** The screen's one mint action (Start sleep / Woke up) stays the only teal fill.
  The Log button is the brand's ink, like the snackbar.
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

## Smart defaults

- See "Remember actions" above: every sheet opens on what was done last.

## Next candidates (not built yet)

- A home-screen widget or quick-settings tile for Start sleep / Woke up and the two breasts, so
  the commonest entries don't even need the app open.
- Long-press on Start sleep to log a sleep that began a few minutes ago (5 / 10 / 15 min), for when
  the baby fell asleep in your arms before you could reach the phone.

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
