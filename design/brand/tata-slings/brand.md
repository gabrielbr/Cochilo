# Tatá Slings: the brand Cochilo wears

Cochilo is a Tatá Slings product. This brand dresses it inside the Gabriel Design System's rules:
**a brand swaps colour roles and the one type family, nothing else**. Spacing, radii, sizes,
motion, the type scale, the frame, components, icons, data colours and every rule stay the
system's. The limits are a check (`check_brands` in the core, run by `python3 design/build.py`).

- **Values:** [brand.json](brand.json) (`replaces` names the role, with a light, dark and night
  value).
- **Textures:** [textures/](textures/), her fabrics, with their source and permission.
- **Voice and the brand study:** [docs/tata-slings/](../../../docs/tata-slings/), especially
  `brand-voice.md` and `textures.md`.
- **Type:** Libre Franklin (SIL Open Font License 1.1); the faces are in `app/src/main/res/font/`.
- **The mark:** the moon in a sling, [cochilo-tata-mark.svg](cochilo-tata-mark.svg); in the app as
  `res/drawable/mark_cochilo.xml`, the launcher and the notification icon.

Moved here from the design system's `docs/brands.md` in Cochilo 0.40.1 (design/README.md says
why). History below is kept as it was written there.

## Tatá Slings

Measured from tataslings.com.br on 2026-11-02 (its theme CSS and logo): teal `#5DC2BB` on the
logo and buttons, navy `#13293D` as the accent, Libre Franklin across the site. The warm neutrals
take her fabric names (Pérola, Cereja). Full brand study, voice and rationale: the Cochilo repo,
`docs/tata-slings/`.

| Role | Light | Dark | Contrast that matters |
|---|---|---|---|
| page | `#FBF7F2` Pérola | `#10161C` | |
| surface | `#FFFFFF` | `#171D24` | |
| ink | `#2B2233` plum-grey (was navy `#13293D` until 0.7.0) | `#F3EEF2` | 14:1 on page |
| muted | `#5F6B78` | `#A3B0BC` | 5.1:1 / 8.2:1 on page |
| line | `#E9E2D8` | `#26303A` | never text |
| mint (brand accent) | `#5DC2BB` teal | `#5DC2BB` | fill only: 2.1:1 on white, so never text |
| on-mint | `#13293D` | `#10161C` | 7.0:1 / 8.6:1 |
| coral / coral-text / coral-soft | `#E07A6B` / `#B5473A` / `#FBE9E5` | `#FF9C8E` / `#FF9C8E` / `#3A2220` | coral-text 5.0:1 on page, 4.6:1 on coral-soft |
| panel / on-panel / on-panel-muted | `#13293D` / `#FBF7F2` / `#A3B0BC` | `#0B1A27` / `#EDF1F4` / `#A3B0BC` | 13.9:1, 6.7:1 (light) |
| snackbar / snackbar-action | `#13293D` / `#5DC2BB` | `#EDF1F4` / `#1E6E68` | 7.0:1 / 5.3:1 |

Data colours are the cochilo layer's, not the brand's. From 0.7.0 that layer chose her fabrics
for them (below), so the two read as one.

### 0.7.0, "Tecidos": warmer, made for her

Most people who log a baby's day in Cochilo are mothers, and the first look (navy lines on cream)
read cold and masculine. 0.7.0 keeps every rule and warms the values:

- **Brand (colour roles):** ink is a warm plum-grey `#2B2233` (14:1 on page) instead of navy; the
  page takes a blush of Rosa Chá (`#FBF5F1`); `line` is rosé; the sleep panel is a plum night
  (`#2E2238`); dark theme is plum-black (`#141017`). New role `fab` = Amora `#8A3566` (dark
  `#E59AC6`) for the Log button, with `on-fab` 7.2:1. Teal is still the one action.
- **Cochilo layer (data, shape):** each kind of entry wears one of her fabrics as its data colour
  (Lavanda night, Imensidão nap, Rosa Chá feed, Amora pump, Trevo diaper, Mel daylight, Turquesa
  routine, Cereja medicine, Capuccino note), and its icon sits in a soft round of that colour
  (`event-*-soft`, ≥ 3:1 for the line on it). Corners are rounder: `radius-s` 8, `radius-m` 16.

The brand rule didn't bend: the brand still swaps only colour roles; data colours and radii
changed in the layer, where they belong.

**Type:** Libre Franklin 400 / 600 / 700 (`assets/fonts/libre_franklin_*.ttf`, SIL Open Font
License 1.1, `assets/fonts/OFL-LibreFranklin.txt`). The roles' sizes and weights are the layer's;
the timer and meta roles use tabular figures so running times don't jitter (app-side, Theme.kt).

**Mark:** `assets/logos/cochilo-tata-mark.svg`: a crescent moon resting in a sling's curve, navy
on teal. It is the launcher icon (adaptive, symbol inside the 66dp safe zone) and the header's
32dp mark in place of the `CO_` monogram.

## Night light (0.14.0)

Cochilo has a third theme, **night**, chosen by the app (19:00–07:00), not by the system. The brand
spells out its night values for the roles it swaps; the layer for its data colours; everything
else falls back to dark (`theme_fallback`). Research: light at night suppresses melatonin even
dim; red and amber disturb least (Cochilo research §2.3).

## Softer (0.11.0)

A review for "feminine and maternal" (audit v0.11.0): under the brand teal is only the one
action. `selected` (a blush) marks what is picked, `frame-line` (a Rosa Chá hem) is the frame; the
brand still swaps colour roles only; the caption, display weight and pill buttons are the
`cochilo` layer's.

## Textures (0.8.0)

A brand may also bring **textures**: photos of the partner's own product, as assets in
`assets/textures/<brand>/` with their source and permission written beside them. They are
pictures, not colours, so they carry no meaning and pass no contrast check; the rule that keeps
them safe is that **no text is ever drawn on a texture**. An app shows one only as the band of a
Fabric frame or as a picker swatch (components.md), and the person picks it.

`tata-slings` brings eleven: eight plain fabrics (Pérola, Rosa Chá, Capuccino, Mel, Trevo, Cereja,
Crepúsculo, Chumbo) and three prints (Balão, Folhagem, Tropical), cropped from her product photos
(`assets/textures/tata-slings/README.md`). In Cochilo they wrap the Today card.

Why not a washed texture behind the page: to keep 12–13sp muted text at 4.5:1 over the darkest
part of a print, the page colour has to cover 93–96% of it, so the fabric no longer reads. A band
with no words on it shows the fabric at full strength.

