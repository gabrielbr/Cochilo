# Tatá Slings design system (for Cochilo and beyond)

> Draft 0.1, 2026-11-02. Built from what tataslings.com.br ships today (its Nuvemshop theme CSS and
> logo), Tatá's product language and the [voice guide](brand-voice.md). It's a proposal for Tatá to
> approve. The token values live in
> [`design-system/tata-slings/tokens.json`](../../design-system/tata-slings/tokens.json); a visual
> preview is in [`preview.html`](preview.html).
>
> **How it reaches the code.** AGENTS.md says Cochilo takes every value from `CTokens.kt`, generated
> by the Gabriel Design System, and that nobody edits it by hand. So this becomes a **`tata` layer**
> in the design system (same route as `design-system/0001-…patch`), which generates a Tatá-themed
> `CTokens.kt`. Nothing in `app/` changes until that layer is merged there.

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

### Brand palette

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
