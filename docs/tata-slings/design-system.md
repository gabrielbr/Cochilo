# Tatá Slings design system (for Cochilo and beyond)

> Started as a proposal (2026-11-02), built from what tataslings.com.br ships (its Nuvemshop theme
> CSS and logo), Tatá's product language and the [voice guide](brand-voice.md). A visual preview
> is in [`preview.html`](preview.html).

## Status: what the app ships (Cochilo 0.8.0)

Cochilo wears this brand through the Gabriel Design System's **brands** concept (design system
0.6.0, `docs/brands.md` there; the change waits as a patch in
[`design-system/`](../../design-system/README.md)). A brand swaps colour roles, the type family and
the mark, and nothing else, so the app follows every Gabriel standard and looks like Tatá's.

| Part of this proposal | In the app? |
|---|---|
| §3 brand palette (teal accent, Pérola page, Cereja alerts), both themes | ✅ `brands.tata-slings` → `CTokens.kt`. Since 0.8.0 ("Tecidos") ink is plum-grey and the night panel plum, not navy; see §3a |
| §4 Libre Franklin as the one family | ✅ `res/font/libre_franklin_*.ttf`, `Theme.kt`; tabular figures for times |
| §7 the mark: crescent moon in a sling, navy on teal | ✅ launcher icon and header mark (`mark_cochilo.xml`) |
| §7 "Cochilo, da Tatá Slings" endorsement | ✅ Settings › About row, opens tataslings.com.br (UTM-tagged) |
| §5 softer radii | ✅ 0.8.0: Cochilo's layer uses 8 for buttons and chips, 16 for cards, round pills in the Log menu (the system's own values; the brand still doesn't change radii) |
| §3 fabric palette (Mel, Trevo, Imensidão…) | ✅ 0.8.0: each kind of entry's data colour and icon round (§3a). Still never UI state |
| §3 `eventCarry`, a data colour for sling naps | ⬜ Waits for the "where the baby slept" log (data colours are the system's) |
| §6 the woven texture ("trama") | ⬜ Not built; instead (0.9.0) her real fabrics wrap the Today card, picked in Settings ([textures.md](textures.md)) |
| §8 tip card on `tealSoft` | ⬜ The "i" tooltips use the system's panel colours |

## 1. What the brand looks like today (measured)

| Element | Value on tataslings.com.br | Notes |
|---|---|---|
| Brand colour | **Teal `#5DC2BB`** | Logo, buttons, borders, icons (93 uses in the theme CSS). 2.1:1 on white, so it can't be text. |
| Text | Slate `#34495E` | Body and links. 9.3:1 on white. |
| Accent | Navy `#13293D` | Icons, installment price, progress bar. |
| Type | **Libre Franklin** (Google Fonts, 400 and 700 used) | Whole site. |
| Logo | "Tatá Slings" in teal, decorative high-contrast serif; "Tatá" letters filled with diagonal hatching | The hatching reads as woven fabric. A round version exists (Tatá asked for it on 28 Oct). |
| Shape | Radii 10, 12, 15, 18, 20 px and pills | Soft, rounded. |
| Photography | Real family photos at home, Tatá pregnant, babywearing in daily life | Personal, natural light. |
| Illustration | Line drawings of the physiological position by age and step-by-step wear guides | Teaching, not decoration. |
| Product colours | Chumbo, Pérola, Chocolate, Capuccino, Mel, Trevo, Crepúsculo, Imensidão, Rosa Chá, Cereja, Lavanda, Amora, Turquesa | The fabric palette is warm and muted; the web theme is cool. |

The gap: the website is the stock Nuvemshop "Amazonas" theme in teal. It's clean but generic. The
brand's warmth lives in the words, the photos and the fabric names, not in the UI. The system below
keeps the teal (recognition), adds the warm side from the fabrics, and fixes contrast.

## 2. Principles

1. **Colo.** Everything holds the content softly: rounded corners, generous padding, nothing sharp.
   Surfaces wrap content the way a sling wraps the baby: close, never tight.
2. **Calm at night.** The sleep screen gets darker and quieter, never brighter (Cochilo already does
   this; the night palette uses navy, not black).
3. **Simple first.** One primary action, short labels, the answer before the explanation.
4. **Knowledge you can see.** Every rule, prediction or chart shows its reason (Cochilo's "predictions
   explain themselves" is a brand value here, not only a product rule).
5. **Feito à mão.** The woven hatching from the logo is the brand texture; real photos over stock
   ones; Tatá's own handwriting-like notes can sign the tips.

## 3. Colour

### 3a. "Tecidos" (Cochilo 0.8.0): warmer, made for her

The first build read cold and masculine (navy lines on cream), and the people who log a baby's day
are mostly mothers. Three directions were compared side by side ("Linha" today, A "Tecidos",
B "Rosa Chá", C "Aquarela", side by side in [`directions.html`](directions.html)); the owner chose **A**. What changed:

| | Before | Tecidos |
|---|---|---|
| Ink (text, lines) | navy `#13293D` | plum-grey `#2B2233` (14.1:1 on page) |
| Page | Pérola `#FBF7F2` | Pérola with a blush `#FBF5F1`; dark `#141017` plum-black |
| Hairline | `#E9E2D8` | rosé `#EEDFD8` |
| Sleep panel | navy | plum night `#2E2238` |
| Log button | navy (`ink`) | **Amora** `#8A3566` (new role `fab`; `on-fab` 7.2:1) |
| Entry icons | one-colour navy lines | each kind's line in its fabric colour on a soft round of it (`KindIcon`, 36dp) |
| Data colours | blue/orange/violet set | her fabrics (below), every kind has one |
| Corners | 4 / 8 / 16 | 8 / 16 / round pills |

Teal is still the one action per screen, the frame and the mark. The fabric data colours (light /
dark; the line on its soft round is ≥ 3.9:1, on the page ≥ 4.7:1):

| Kind | Fabric | Colour | Soft round |
|---|---|---|---|
| Night sleep | Lavanda | `#5B4B8A` / `#B7A6E6` | `#E4DDF2` / `#2E2747` |
| Nap | Imensidão | `#3D6FA3` / `#8FB8E8` | `#D9E6F3` / `#1F3046` |
| Feed | Rosa Chá | `#B0503F` / `#F0A898` | `#F6D9D1` / `#47261F` |
| Pump | Amora | `#8A3566` / `#E59AC6` | `#F1D5E4` / `#43213A` |
| Diaper | Trevo | `#3E7350` / `#8CCB9F` | `#D6E9DB` / `#1F3A28` |
| Daylight | Mel | `#8C6A00` / `#FFD25C` | `#F5E6BC` / `#3D3110` |
| Routine | Turquesa | `#1E6E68` / `#7FD6CF` | `#D3EFEC` / `#173A38` |
| Medicine | Cereja | `#A3324A` / `#FF94A8` | `#F6D3DA` / `#48202A` |
| Note | Capuccino | `#7A5A44` / `#D6B9A2` | `#EBDFD4` / `#3A2E25` |

Icon sets looked at first: Phosphor duotone (MIT), IconPark TwoTone (Apache 2.0), Tabler (MIT,
lines only), Fluent Emoji flat (MIT, illustrated). None has a diaper, a breast pump and a sling
together, so Cochilo keeps the design system's own line set and gives it colour and a round.
Design system 0.7.0 (patch `design-system/0002-…`), audit `audits/2026-11-02-v0.7.0-tecidos.md` there.

### Brand palette (as first proposed; see 3a for what ships)

| Token | Light | Dark | Use | Contrast |
|---|---|---|---|---|
| `teal` | `#5DC2BB` | `#5DC2BB` | The one primary action, active tab pill, frame, highlights. Fill only. | Text on it is `onTeal` |
| `onTeal` | `#13293D` | `#10161C` | Text and icons on teal | 7.0:1 / 8.6:1 |
| `tealText` | `#1E6E68` | `#7FD6CF` | Links, selected chip text, teal text on page/surface | 5.7:1 on cream / 10.0:1 on dark surface |
| `tealSoft` | `#DFF3F1` | `#173A38` | Selected chip fill, tip cards | `ink` on it 12.9:1 |
| `navy` | `#13293D` | `#13293D` | The sleep panel, snackbar, the night | `cream` on it 13.9:1 |
| `page` | `#FBF7F2` (Pérola) | `#10161C` | Screen background | |
| `surface` | `#FFFFFF` | `#171D24` | Cards, header, sheets, fields | |
| `ink` | `#13293D` | `#EDF1F4` | Text and icons | 13.9:1 / 14.9:1 |
| `muted` | `#5F6B78` | `#A3B0BC` | Meta text, captions | 5.1:1 / 7.7:1 |
| `line` | `#E9E2D8` | `#26303A` | Hairlines, borders. Never text. | |
| `cereja` | `#E07A6B` | `#FF9C8E` | Stop/destructive fill (text on it `ink`) | |
| `cerejaText` | `#B5473A` | `#FF9C8E` | Errors, destructive outlines | 5.0:1 / 8.4:1 |
| `cerejaSoft` | `#FBE9E5` | `#3A2220` | Behind error chips | |

Why navy for ink instead of the site's slate `#34495E`: navy is already the site's accent, it reads
as one family with the teal, and it gives more contrast at Cochilo's small meta sizes. The website
can keep slate for long text if Tatá prefers; both pass.

### Fabric palette (marketing, prints, themes; never UI state)

Named after her fabrics so the colours carry the brand story. Values are proposals to match against
the real fabric photos.

| Name | Hex | Name | Hex |
|---|---|---|---|
| Pérola | `#FBF7F2` | Mel | `#D9A441` |
| Capuccino | `#B08B6E` | Chocolate | `#5B3E31` |
| Rosa Chá | `#E8B4A8` | Cereja | `#B8323F` |
| Trevo | `#6E9C7A` | Imensidão | `#2F5D8A` |
| Lavanda | `#B7A6D6` | Chumbo | `#4A4F55` |

Use: Instagram backgrounds, the insert card, guide covers, and Cochilo+ themes ("Tema Mel",
"Tema Imensidão"), which swap only `teal`/`tealSoft` for a fabric pair that passes the same contrast
checks. Fabric colours never mean anything in the UI.

### Data colours (Cochilo events)

Keep Cochilo's six event hues (night, nap, feed, pump, diaper, daylight) as they are: they're
already checked for contrast and for telling apart, and they follow the rule "data colours only on
data, always next to a word". Add one:

| Token | Light | Dark | Use |
|---|---|---|---|
| `eventCarry` | `#1E8C84` | `#5DC2BB` | Data: naps in the sling or in arms. Teal on purpose, as the one data colour that is also the brand: contact naps are the brand's subject. |

`eventNap` stays blue, so a nap in the crib and a nap in the sling are told apart by colour and by
the word ("Sling", "Berço") beside them.

## 4. Type

| Role | Family | Size / line (sp) | Weight | Where |
|---|---|---|---|---|
| Display | Libre Franklin | 30 / 36 | 700 | Screen titles ("Hoje") |
| Timer | Libre Franklin, tabular figures | 56 / 64 | 700 | The awake/asleep timer |
| Title | Libre Franklin | 22 / 28 | 700 | Card and sheet titles |
| Label | Libre Franklin | 16 / 22 | 600 | Buttons, row labels |
| Body | Libre Franklin | 16 / 24 | 400 | Text |
| Body small | Libre Franklin | 14 / 22 | 400 | Explanations under predictions |
| Meta | Libre Franklin | 13 / 20 | 400 | Times, values |
| Caption | Libre Franklin | 12 / 16 | 700, caps, +0.5 | Section labels |

Sizes are Cochilo's existing scale, so layouts don't move; only the family changes (Ubuntu Mono →
Libre Franklin). Libre Franklin is OFL, so it can ship in the APK (add its licence to `licenses/`),
and it matches the website. Use its tabular figures for times so the timer doesn't jitter.

The logo's serif is **only the logo**. Don't set headings in a look-alike.

## 5. Shape, space, motion

- **Radius:** S 8, M 12, L 20, round 999. Softer than Cochilo's 4/8/16, matching the site's 10–20.
- **Spacing:** keep the 4-point scale (4, 8, 12, 16, 24, 32, 40, 48, 64).
- **Sizes:** keep 48 dp targets, 52 dp buttons, 56 dp rows.
- **Frame:** Cochilo's 4 dp mint frame becomes a 4 dp teal frame (the "abraço" around the screen).
- **Elevation:** none. Separation by surface colour and hairlines, like fabric layers.
- **Motion:** 150 ms standard, 250 ms emphasised, ease-out; nothing bounces. Honour Remove
  animations.

## 6. Texture and imagery

- **Her fabrics (Cochilo 0.9.0):** photos of her slings wrap the Today card, picked by the person;
  never behind text. [textures.md](textures.md).

- **Trama (weave):** the diagonal hatching from the logo letters, 45°, 1 px lines at 4 px pitch, in
  `teal` at 25% over `page`. Use on the insert card, guide covers, Play feature graphic and the
  empty states in Cochilo. Never behind text.
- **Photos:** real families, home light, faces visible. Every photo with a sling must show correct
  positioning (face visible, chin off chest, back supported, "pernas de sapinho"). Every sleeping-baby
  photo must show safe sleep (on the back, flat firm surface, nothing soft around). No exceptions in
  ads.
- **Illustration:** single-weight line (1.5 px at 24 px), rounded caps, `ink` with one `teal` accent.
  Same style as her position guides.
- **Icons:** keep the design system's line set; add `sling` (for the carry log) and `heart-hands`
  (tips by Tatá).

## 7. Logo and the Cochilo mark

Brand architecture: **Tatá Slings** is the master brand; **Cochilo** is a product endorsed by it,
like Tatá Fácil Sling and Tatá Swaddle.

- **App name on the phone:** "Cochilo".
- **Play listing title:** "Cochilo da Tatá: sono do bebê" (search words in the title, the brand
  attached).
- **Endorsement line:** "da Tatá Slings" under the Cochilo wordmark on the splash, About, Play
  graphics and the insert card.
- **App icon:** a crescent moon cradled in a sling curve (a "U" of fabric), `navy` on a `teal`
  ground, with the woven hatching inside the curve. Readable at 48 px; no letters.
- **Wordmark:** "Cochilo" in Libre Franklin 700 with the "o"s as closed eyes (a small downward arc).
  Optional; the plain wordmark is fine for launch.
- Cochilo's `CO_` monogram from the Gabriel system stays for the developer-facing parts only.

## 8. Components (what changes in `ui/kit`)

The component set doesn't change; only tokens do. Notes per component:

| Component | Change |
|---|---|
| Primary button | `teal` fill, `onTeal` label, radius round, 52 dp |
| Secondary button | `ink` outline 1.5 dp, radius round |
| Chips | radius round; selected = `tealSoft` + `tealText`; event chips keep data dot + word |
| Rows | unchanged; `line` hairline |
| Sleep panel | `navy` ground in both themes, `page` text, timer in tabular figures |
| Prediction card | `surface`, radius L; the "why" line in body small `muted`; optional "Dica da Tatá" footer on `tealSoft` |
| Tip card (new) | `tealSoft`, radius L, Tatá's line + a link; at most one on a screen; dismissable |
| Store row (new) | A normal Settings row, "Loja Tatá Slings", external-link icon. Not a banner. |
| Empty states | Line illustration + trama, one sentence, one action |

## 9. Accessibility (unchanged rules, new values checked)

- Body text ≥ 4.5:1, large text and icons ≥ 3:1. All pairs in §3 were computed; the lowest text
  pair is `cerejaText` on `page` at 5.0:1.
- `teal` is never text and never the only signal of state.
- 48 dp targets, TalkBack labels, a spoken summary for every chart.
- Data colours always beside a word.

## 10. Where else this applies

The same tokens can theme the Nuvemshop store (CSS variables in a custom theme), the insert card,
Instagram templates (Canva brand kit) and e-mails, so the app, the box and the site look like one
brand. Suggested order: Cochilo first (smallest surface, we own the code), then the insert card,
then the Canva kit, then the store theme.
