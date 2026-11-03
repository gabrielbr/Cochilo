# Accessibility

Cochilo is used one-handed, at night and by tired eyes, so it follows the design system's
`docs/accessibility.md` (WCAG 2.1 AA in all three themes). This is how the app keeps each rule and
where it's checked.

## Large text (0.22.0)

Android lets the font size go up to 200%. Every screen must still show every word that matters:

- **Nothing has a fixed height.** The header, the pushed screens' header and the tab bar use
  minimum heights, so a status line ("Awake · 40m") grows instead of being cut off.
- **Chip rows flow.** The Earlier chips ("Fell asleep [5] [10] [15] min ago"), the sleep panel's
  Where chips and the Log button's choices (Wet · Both · Dirty) wrap onto a second line between
  chips, never inside one. Before 0.22.0, the Log menu lost Dirty at 200%.
- **Numbers stay with their words.** "Naps: 4" and "Age: 12 wk" use a non-breaking space.
- **The label scale cap (150%).** Two kinds of label stop growing at 150%, and only these:
  - the tab bar's words, because each sits under an icon that says the same thing;
  - chart ticks (the hours under a timeline, the day names on Patterns' rhythm and the ring's
    00 · 06 · 12 · 18), because the chart has a spoken summary, and their band widens with them.
  `CappedFontScale` and `LABEL_SCALE_CAP` in `ui/kit/Kit.kt`. Everything else grows in full.
- **Checked by screenshots.** `ScreensSnapshotTest` keeps 200% shots of Today (awake and asleep),
  the open Log menu, Day, History, Patterns and Settings (`*_font200_light`).

## Elsewhere

- **Targets**: 48dp everywhere; small visuals (32dp chips) sit inside a 48dp area.
- **TalkBack**: every chart and ring has a spoken summary, the header's status is a live region,
  Earlier chips read in full ("Fell asleep 10 minutes ago"), checklists are checkboxes.
- **Motion**: Remove animations makes everything appear in place (`ui/kit/Motion.kt`).
- **Not colour alone**: event colours always sit next to a word.
