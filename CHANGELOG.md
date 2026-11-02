# Changelog

Every release of Cochilo, newest first. Releases are on
[GitHub](https://github.com/gabrielbr/Cochilo/releases); the family build updates itself from them.

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
