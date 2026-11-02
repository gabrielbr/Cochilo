# Changelog

Every release of Cochilo, newest first. Releases are on
[GitHub](https://github.com/gabrielbr/Cochilo/releases); the family build updates itself from them.

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
