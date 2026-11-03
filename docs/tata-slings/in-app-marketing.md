# Tatá Slings inside Cochilo: a plan that never gets in the way

> Draft, 2026-11-03, to discuss with Tatá. It builds on [monetization.md](monetization.md) §5
> ("sends people to the store without becoming an ad") and §10 ("what we will not do"), and on
> [brand-voice.md](brand-voice.md). Nothing here is built yet; the roadmap holds it.

## 1. The idea in one paragraph

Cochilo sells by being useful, not by showing products. The parent opens it a dozen times a day
for the baby, never for the store. So the store appears only **where the parent already is in a
calm, curious moment** (Patterns, Settings, the fabric picker, a note from Tatá), **only when her
own log or the baby's age makes a product genuinely relevant**, **at most once a week**, **with the
reason in plain words**, and **always with "Não mostrar mais"**. Logging, Today, the night, the
notification, the widget and every safety screen stay free of it, always.

## 2. Ground rules (the non-intrusive contract)

These are product rules, the same weight as "the data never leaves the phone". A suggestion that
breaks one doesn't ship.

| Rule | Why |
|---|---|
| **Never on Today, the Log button, a sheet, the notification, the widget or the tile** | Those are for logging with a baby in arms; one tap must stay one tap |
| **Never while the baby sleeps, never between 19:00 and 07:00** | The night light hours are for calm (night-light.md) |
| **Never in the first 7 days** of use | Trust first; the parent is learning the app |
| **At most one suggestion on screen, at most one new one a week** | A tracker, not a shop window |
| **Always the reason, from her own log or the baby's age** | "Porque" is the brand (*Explica o porquê*) |
| **"Não mostrar mais" on each, and one switch for all** (Settings › Sugestões da Tatá) | Dismissed means dismissed, for good |
| **Safety never sells** | T.I.C.K.S., safe sleep, swaddle-stopping notes carry no product, ever |
| **Celebrations never sell** | A "parabéns" with a price tag reads as a trick |
| **No urgency, no discount countdowns, no badges** | Brand voice: quiet and short in the app |
| **No tracking in the app** | The store counts visits with UTM tags on the link; the app counts nothing |
| **Works offline** | The play build has no INTERNET: suggestions ship inside each release, the link opens the browser |

## 3. Where Tatá Slings can appear

Ordered from most to least natural. Each is a place the parent chose to go.

### 3.1 "Da Tatá" notes by age, in Patterns (content first)

The roadmap's *Notes from Tatá*: one short note per age band, written and signed by her, reviewed
against AAP 2022. Most notes carry **no product at all**. A few end with one related link, only when
the product is what the note is about:

> **Aos 2 meses** · Sonecas curtas e no colo são comuns agora; o ritmo do dia ainda está se formando.
> Se o colo cansa os braços, o sling de malha deixa as mãos livres. *Ver o Tatá Fácil Sling*
> — Tatá, terapeuta ocupacional e mãe do Francisco e do Dante

This is the main channel: it builds trust every week and sells rarely.

### 3.2 Moments from her own log, in Patterns

A small card under the week's data, when the log shows a pattern a product genuinely helps with.
Each moment has a rule (pure Kotlin, unit-tested, like the predictor) and fires once:

| Moment (rule) | What the card says (pt-BR, short) | Product |
|---|---|---|
| 0–12 weeks, 5+ naps in arms in the last 7 days | "5 sonecas no colo esta semana. O sling deixa as mãos livres." | Tatá Fácil Sling |
| Sling naps logged, baby now 4+ months (heavier) | "Para um bebê maior, um carregador estruturado divide o peso." | Ring sling / structured carrier |
| Daylight under 20 min/day for a week | "Um passeio de sling entre 12h e 16h conta como luz do dia." | Sling (or no product: a tip only, see 3.6) |
| Bedtime routine logged 5+ times | "A rotina está firme. A Naninha pode virar parte dela." | Naninha / Bolsinha Calminha |
| 6 months | "Aos 6 meses, brincar também ajuda a dormir melhor à noite." | TatáTessori line |
| 0–8 weeks, frequent startle wake-ups noted | "O swaddle ajuda nas primeiras semanas." (stops at rolling signs, see 2) | Tatá Swaddle |

Never more than one card, never two weeks in a row for the same product, and the copy always
starts with what *she* logged, not with the product.

### 3.3 The fabric picker (Settings › Aparência › Tecido)

The parent already picked a Tatá fabric to wrap her Today card. Under the picked swatch, one quiet
line: "Este tecido existe em sling. *Ver na loja*". It's in Settings, behind a deliberate choice,
and it's about the thing she chose. This is the least intrusive placement in the app.

### 3.4 Settings › Loja Tatá Slings (always there, never pushed)

One row, as monetization.md §5 says, plus:
- "Cupom de quem usa o Cochilo": a thank-you code for app users (a store decision), shown only
  here.
- The guides (monetization §2) once they exist: "Guias da Tatá".

### 3.5 The report for the doctor

The PDF already says "From the parent's own log". Its footer can say "Feito com Cochilo, da Tatá
Slings" (brand, not an ad; no link, no product). Paediatricians and consultants see it.

### 3.6 Tips with no product (they earn the right to the rest)

The daylight nudge, the ritual builder, T.I.C.K.S. and the celebrations stay product-free. They're
why a parent trusts the 1-in-10 moment that does show a product.

### 3.7 Outside the app, about the app

Not in-app, but it closes the loop: the card in the box (monetization §4), Instagram posts built
from features ("Toda soneca conta. Até no colo."), and the order-confirmation e-mail with the
Play link. The app's job is to keep the parent; the store's job is to bring her in.

## 4. Where it never appears

Today (awake or asleep), the Log button and its menu, every entry sheet, details-after sheets,
the onboarding, the live notification, the widget, the quick-settings tile, celebrations,
T.I.C.K.S., safe-sleep text, the passphrase and import screens, error messages, and anything
between 19:00 and 07:00.

## 5. How it's built (when it's built)

- **Design system first**: a "Tatá note" component (a card in `surface`, her name and role as
  the byline, one secondary text link, "Não mostrar mais" as a quiet text button). No teal fill:
  teal stays the screen's one action. No fabric behind text (no text on a texture).
- **`domain/Suggestions.kt`**: pure rules over the log, the baby's age, the clock and what was
  dismissed → at most one `Suggestion(moment, productKey)`. Unit tests for every rule and every
  ground rule in §2 (never at night, never in the first week, once a week, never twice).
- **A catalog in the app** (`assets/tata/catalog.json`): product keys, names, URLs, age ranges,
  reviewed with Tatá each release. No remote config: the play build can't fetch it, and a
  catalog that changes without a release would break the "nothing leaves the phone" promise's
  spirit.
- **Links**: `https://tataslings.com.br/...?utm_source=cochilo&utm_medium=app&utm_campaign=<moment>`,
  opened with an intent in the browser (no permission). The store reads the UTM numbers; the app
  stores only which moments were shown or dismissed, on the phone.
- **Prefs**: `tataSuggestions` (the global switch, on by default with a clear line in
  Settings), `suggestionsDismissed`, `suggestionLastShown`. Not in the backup.
- **Copy**: pt-BR and English, by Tatá's voice, reviewed by her; never "compre", never "!".
- **Audit**: each suggestion card gets the same impeccable + §13 audit as any feature.

## 6. How we'll know it works (and when to stop)

Measured in the store, never in the app: Nuvemshop orders with `utm_source=cochilo`, by
`utm_campaign` (the moment). After 3 months:

| Signal | Keep going if | Change if |
|---|---|---|
| Orders from Cochilo links | ≥ 20 a month (monetization §7) | Under 20: fewer moments, better notes |
| Play rating and reviews | No review mentions ads or pushiness | One does: remove that moment |
| "Não mostrar mais" rate per moment | Under ~30% | Over: the moment isn't useful; drop it |
| Uninstalls after a card (Play console) | No change | A rise: pull the cards |

## 7. Order of work

| Step | What | Needs |
|---|---|---|
| 1 | Settings › Loja Tatá Slings and the fabric picker line (§3.3, §3.4) | **Shipped in 0.31.0** (§9) |
| 2 | "Tatá note" component in the design system; `Suggestions.kt` with tests | — |
| 3 | Notes by age (§3.1) | Tatá's writing (the scarce resource) |
| 4 | Log moments (§3.2), one at a time, each with its UTM campaign | Tatá's sign-off per moment |
| 5 | Review at 3 months by the numbers in §6 | Store reports |

## 9. Step 1, as shipped (0.31.0)

- **The fabric line (§3.3).** Under the picked fabric in Settings › Appearance › Fabric: "This
  fabric comes as a sling. See it in the shop ›" (pt: "Este tecido existe em sling. Ver na loja ›").
  Not shown for Plain. A **quiet link** (design system 0.24.0): ink text and a chevron in a 48dp
  row, never teal, never a button.
- **No guessed product pages.** The link is the shop's own search for "sling <fabric>"
  (`tataslings.com.br/search/?q=…`), checked on 2026-11-03 for all eleven fabrics: each lands on
  that fabric's Tatá Fácil Sling first. A restock or a renamed product still works. (Folhagem is
  sold today as "Malha NATURA", at the same product address, and the search finds it; see the
  questions below.)
- **The shop row (§3.4)** already existed: Settings › About › "Cochilo, da Tatá Slings" opens the
  shop's home (campaign `about`). It stays as it is; the coupon and the guides wait for Tatá.
- **Safety never sells, fixed.** The T.I.C.K.S. sheet's "Tatá's guides" used to open the shop's
  home. It now opens her guide page, "Tatá Fácil Sling: como usar, posições e cuidados"
  (`/tudo-sobre-o-tata-facil-sling/`): positions, the chin-off-the-chest check and her videos.
- **Campaigns** (UTM, counted by the shop only): `about`, `fabric_<key>` (e.g. `fabric_rosa_cha`),
  `ticks_guide`. All links live in `store/Store.kt` (tested in `StoreTest`) and open the browser
  through an intent: no permission, nothing recorded in the app.

## 8. Open questions for Tatá

1. Which products and URLs belong in the first catalog, and their age ranges?
2. Is there an app-user coupon, and what is it?
3. Will she write the notes by age (about 12 short notes for 0–24 months)?
4. Which of the §3.2 moments feel right to her, and which feel pushy?
5. Should the "Sugestões da Tatá" switch start on or off? (Recommendation: on, with the line
   "Às vezes, uma dica da Tatá com um produto da loja. Desligue quando quiser.")
6. Folhagem is now "Malha NATURA" in the shop: should the app's fabric be renamed Natura?
