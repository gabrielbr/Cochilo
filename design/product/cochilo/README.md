# The Cochilo layer

Everything about Cochilo's look that isn't the core's and isn't the brand's.

- **[layer.json](layer.json)**:
  - `layer`: the themes (light, dark, night, with night falling back to dark), the brand it wears
    (`tata-slings`), and each group (color, type, spacing, radius, size, motion). Core tokens are
    pulled with `"@name"`; Cochilo's own are objects with a `usage` line that says why they exist.
  - `contrast`: every pair that must pass, checked in all three themes.
- **[components.md](components.md)**: the components and patterns Cochilo's kit implements. Which
  rows are Cochilo's and which are the core's is explained at its top.

## Cochilo's own tokens, and why

- **Kind colours** (`event-*` and `event-*-soft`): her fabrics as data colours. Used only on data,
  always next to a word.
- **App roles**: `panel` (the night panel), `snackbar`, `fab` and `on-fab` (the Log button),
  `selected` (a picked chip or tab, a blush), `frame-line` (the screen's hem).
- **Illustration** (`illustration-*`): the baby avatar's colours, the same in every theme.
- **Night light** (the `night` theme): 19:00–07:00, amber on plum-black.
- **Sizes:**
  - `kind-badge` and `kind-badge-lead`: sleep leads, a size up;
  - `outline` for the Last mark, `avatar` for the baby avatar;
  - `header`, `tabbar`, `button`, `row-min`, `timeline`;
  - `rail`, `content-max`, `width-medium` and `width-expanded` for wide screens.
- **Motion:** `motion-standard` 150ms and `motion-emphasised` 250ms (core), plus `motion-stagger`
  35ms for the Log menu's cascade.

## Changing it

1. Edit `layer.json` (and `components.md` for a component).
2. Run `python3 design/build.py`, then `python3 design/build.py --check`.
3. Re-record the screenshots, look at them, and write the audit in `docs/audits/<version>-<topic>.md`.
4. Docs and CHANGELOG as AGENTS.md says.

A new colour role needs a value (or a fallback) in all three themes and a contrast pair if text
sits on it. The build fails otherwise.
