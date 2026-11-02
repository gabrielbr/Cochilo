# Changelog

Every release of Cochilo, newest first. Releases are on
[GitHub](https://github.com/gabrielbr/Cochilo/releases); the family build updates itself from them.

## 0.20.0 — 2026-11-02

**A report for the doctor.** Settings › Data › Report for the doctor (PDF): one page with the
week's averages, what changed, where naps happened and two weeks of sleep as bars, made on the
phone and saved where you choose. "From the parent's own log. Not a medical record."
`docs/report.md`.

## 0.19.0 — 2026-11-02

**Routine steps.** Write the bedtime and nap steps once (Settings › Baby › Routine steps; there are
gentle defaults to start from), then tick them as you go when you open a routine; the ticked steps
are saved with it. Sliding the Routine row still logs at once. `docs/rituals.md`.

## 0.18.0 — 2026-11-02

**What changed.** Patterns now opens with what really changed in the last week against the two
before it ("Night sleep is longer · 10h 30m · 9h 00m before"), only when the change is real, in
neutral words: changes, not problems, and never a "regression". **Day and night milk:** pumped
milk says whether it was expressed by day or at night, and Patterns shows the week's split, so
night bottles can be night milk. `docs/changes.md`.

## 0.17.0 — 2026-11-02

**Where the baby slept.** Crib, in arms, sling, pram or car seat: each sleep remembers the last
place, and while the baby sleeps the panel shows "Where" to change it in one tap. Patterns shows
where naps happened this week, side by side ("Naps in arms and in the sling are normal in the first
months"). The first time you pick the sling, Cochilo shows the five T.I.C.K.S. checks for safe
babywearing, once. `docs/places.md`.

## 0.16.0 — 2026-11-02

**"Fell asleep a few minutes ago."** Under Start sleep / Woke up: "Fell asleep 5 · 10 · 15 min ago",
one tap, for when the baby fell asleep in your arms (or woke) before you could reach the phone.
**A quick-settings tile**: add "Cochilo sleep" to the phone's quick settings for Start sleep /
Woke up from the notification shade, lit while the baby sleeps. `docs/flows.md`.

## 0.15.0 — 2026-11-02

**A night light.** From 19:00 to 07:00 the app turns warm amber on black, with no blue light (light
at night is what the research says to keep dim; red and amber disturb least). Settings ›
Appearance › Night light turns it off. **Daylight:** from 14:00, if there's been no daylight
between 12:00 and 16:00, Today offers "Go outside" to start a walk in one tap. **Wind-down:** the
notification says "dim the lights" in the hour before bedtime. `docs/night-light.md`.

## 0.14.0 — 2026-11-02

**Little celebrations.** When what you were trying for happens (a full nap, a sleep right on time,
the longest night of the week, daylight done, the routine before bed, a whole week logged), the
baby you drew lifts its arms in a soft pink card with a few warm words, then goes away by itself.
Never a scolding, never a promise; at most three a day; just the words, dim, at night. Settings ›
Notifications › Little celebrations turns it off. `docs/celebrations.md`.

## 0.13.0 — 2026-11-02

**Meet the baby.** A warm first run: welcome, the baby's name and birth date, then how the baby
looks (skin tone, hair, girl / boy / rather not say), skippable. The baby appears as a little
drawing wrapped in a Rosa Chá sling: in the first run, asleep on Today before the first sleep, and
in Settings › Baby, where the look can change any time. Dates now always read in the app's
language ("qui 9 jul"). Backups are version 3 (older backups still import). `docs/onboarding.md`.

## 0.12.0 — 2026-11-02

**Softer, warmer, made for her** (a design review with the owner's graphic-design-fundamentals and
impeccable skills, `docs/design-review.md`).
- No colour behind the screen titles: they stand on their own, plain and a touch lighter.
- Teal is only for the one main button. The active tab and a picked chip are a soft blush; the
  edge round the screen is a soft Rosa Chá hem instead of a teal line.
- Captions in quiet sentence case ("Awake for", "Running") instead of spaced capitals.
- Every button is a soft pill, like the Log menu and the widget.
- Icon rounds in dark are a little lighter, no longer muddy.
- A feed under a minute reads "<1min", not "0min".

## 0.11.0 — 2026-11-02

**A home-screen widget: Sleep and feed.** Start sleep / Woke up and the two breasts, one tap each
from the home screen, with "Awake since 14:20" and a running clock. The breast used last has the
coloured dot and outline; while a breastfeed runs, its side reads Finish and the other side
switches. Light and dark, English and Portuguese. `docs/widget.md`.

## 0.10.0 — 2026-11-02

**Motion, sleep first, and a quieter "last".**
- Every action answers with a small movement: presses dip, the Log menu rises from its button and
  its plus turns into a cross, lit options and chips cross-fade, the list opens for a new entry,
  the card fades to the night panel when sleep starts. Off with the phone's Remove animations.
- Sleep is a size up everywhere entries are listed (History, Today, the Day tab's hour by hour,
  the log rows, the Log menu): a bigger round and a bold name, so the sleep pattern stands out.
- "LAST" / "ÚLTIMO" is gone: the option used last has an outline in its kind's colour and a small
  dot before its name, and the "last" lines start with the same dot. TalkBack still says "used last".
- Design system 0.9.0 (`design-system/0004-…`). `docs/flows.md` (Motion, Sleep leads, the Last mark).

## 0.9.0 — 2026-11-02

**Her fabrics.** Pick one of Tatá's real slings in Settings › Appearance › Fabric and it wraps the
Today card, the way a sling wraps the baby: Pérola, Rosa Chá, Capuccino, Mel, Trevo, Cereja,
Crepúsculo, Chumbo, and the prints Balão, Folhagem and Tropical, photographed from her shop. One
tap, applied at once; Plain is the default. No text ever sits on a fabric. Design system 0.8.0;
`docs/tata-slings/textures.md`.

## 0.8.0 — 2026-11-02

**Tecidos: warmer, more colourful, made for her.** Cochilo looked cold and masculine (navy lines on
cream). Now each kind of entry wears one of Tatá's fabrics: its icon sits in a soft round of its
colour (Rosa Chá for a feed, Lavanda for night sleep, Imensidão for a nap, Trevo for a diaper,
Amora for pumping, Mel for daylight, Turquesa for a routine, Cereja for medicine, Capuccino for a
note), in every list, the Log menu, the Day tab and the timelines. Text is a warm plum-grey instead
of navy, the page has a blush, the sleep panel is a plum night, corners are rounder, and the Log
button is Amora. Teal stays the one action. Light and dark, contrast checked. Design system 0.7.0;
`docs/tata-slings/design-system.md` §3a.

## 0.7.0 — 2026-11-02

**The Log button.** Logging no longer sits below the fold: a navy button in the bottom-right
corner, where a right thumb rests, on every tab. Press and hold: the kinds of entry rise above it,
the commonest nearest the thumb. Without lifting, drag up to Feed and its choices appear beside it
(Left · Right, the last breast marked LAST, and the last bottle's amount); slide onto one and let
go: logged, with Undo. Let go on a kind to open its sheet; anywhere else, nothing happens. Diaper
(Wet · Both · Dirty), Sleep (start or wake up), Medicine (the last one again), Pump, Daylight,
Routine and Note are there too. A tap opens the same menu to use with taps (and TalkBack). After
Wunderlist's thumb-corner add button and marking menus. `docs/flows.md`.

## 0.6.0 — 2026-11-02

**Remember actions** (now a project rule, with "fewest taps"). What was done last is shown and
offered first: the Feed row says which breast was last and the slide marks it (LAST), so you can
decide whether to continue on that side; Diaper, Daylight and Routine say and mark their last kind;
Pump shows the last amount; Medicine shows the last medicine and when. Every sheet opens on the last
choice (breast side or bottle with its amount, diaper kind, pump amount, medicine name) and says
"Last time: Right breast · Today 14:20". Replaces the "next: other breast" suggestion.
Audit per entry type: `docs/flows.md`.

## 0.5.0 — 2026-11-02

**Icons for every kind of entry**, so entries are known at a glance: a drop for a breastfeed, a
bottle, a spoon for solids, a pump, a diaper, the moon for sleep, the sun for daylight, a bath for a
routine, a capsule for medicine, a pencil for a note. On the log rows, every entry list, the Day
tab's hour-by-hour list (in the entry's timeline colour) and the timers' notifications. Always next
to the name. Icons are in the design system's line set (patch in `design-system/`).

**Medicine is its own entry type**, with its own log row and "i". The Napper import brings
medicine in as Medicine; medicine already imported as notes ("Remédio · Luftal") becomes Medicine
("Luftal") on first launch (database version 2). Backups are format version 2: older backups still
import; a version-2 backup can't be imported into an app older than 0.5.0.

## 0.4.0 — 2026-11-02

**Notifications, like Napper's.** A quiet live notification: asleep or awake, a running clock,
"expected wake 4:47 PM" or "next nap 4:01 PM", a timeline bar for the stretch (with dots for what
was logged meanwhile and the nap window in teal), and Woke up or Start sleep. Each running timer
(feed, pump, walk) gets its own with Finish, and Switch side for a breastfeed. Asks permission once
(Android 13+); Settings › Notifications turns it off. `docs/notifications.md`.

**Fixes**
- Finishing a running timer from its sheet (switching "running now" off) saved it as 0 minutes,
  because the end defaulted to the start time. It now defaults to now, so a timer that crossed
  midnight keeps its real length.
- "Awake 0min" between two sleeps a minute apart: durations now follow the clock as shown
  (01:53 → 01:54 is 1 minute), not the seconds underneath.

## 0.3.0 — 2026-11-02

**Cochilo, da Tatá Slings.** The app wears the Tatá Slings brand, through the Gabriel Design
System's new *brands* concept (design system 0.6.0, patch in `design-system/`).

- Colours: Tatá teal for the one action, the frame and the active tab; navy text; a warm Pérola
  page; Cereja for alerts; a navy sleep panel, in light and dark. Contrast checked in both.
- Type: Libre Franklin (the website's typeface) replaces Ubuntu Mono; times use tabular figures.
- Mark: a crescent moon resting in a sling, navy on teal: the launcher icon and the header mark.
- Settings › About: "Cochilo, da Tatá Slings" opens tataslings.com.br.
- Unchanged: spacing, radii, sizes, motion, components, data colours and every Gabriel rule.
- Docs: `docs/tata-slings/design-system.md` (what's built), `design-system/README.md`, AGENTS.md
  (the brand, and the owner's standing rules: document everything, always merge and release).

## 0.2.2 — 2026-11-02

**Slide to log.** On Today's log rows, slide instead of tapping: Feed left = left breast, right =
right breast; Diaper Wet · Both · Dirty; Daylight, Routine, Pump. Slide a running breastfeed to
switch side. Every quick entry offers Undo. The Feed row and sheet suggest the other breast from
the last feed. Audit of taps per entry: `docs/flows.md`.

## 0.2.1 — 2026-11-02

**Fix:** Import from Napper always failed on the phone ("isn't a Napper logs export"). A label
template was formatted with no numbers and threw before the file was read.

## 0.2.0 — 2026-11-02

- **Self-update** (family build): checks GitHub when opened, shows "Update available", installs
  after checksum and signature checks. Two builds: `github` (family, updates itself) and `play`
  (no internet permission at all). `docs/distribution.md`.
- **Import from Napper:** Settings › Import from Napper reads Napper's logs CSV (naps, nights from
  bedtime to wake-up split by night wakings, feeds, bottles, diapers, medicine notes).
- **Day tab:** a 24-hour ring and an hour-by-hour timeline with awake windows.
- **"i" buttons** explaining every kind of entry, in English and Portuguese.
- **Fix:** Today's log listed diapers and notes from earlier days.

## 0.1.1 — 2026-11-02

Same app as 0.1.0; docs only (the Tatá Slings plans in `docs/tata-slings/`).

## 0.1.0 — 2026-11-01

First build: one-tap sleep timer, logs for feeds, pumping, diapers, daylight, routines and notes,
a next-nap prediction that shows its numbers, History, Patterns, export and import. Built on the
Gabriel Design System, research in `docs/research.md`.
