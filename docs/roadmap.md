# Cochilo: plan for what comes next

Written 2026-11-02, after 0.12.0. A living plan: features to build, betterments, UX improvements,
and the owner's own suggestions, each with why, what, and the rules it must keep. Nothing here is
built yet unless it says so. Priorities are at the end.

Every item keeps the standing rules (AGENTS.md): **fewest taps**, **remember actions**, **sleep
leads**, **teal is the one action**, **motion from `Motion.kt`**, **bilingual copy**, **the baby's
data never leaves the phone**, **never a promise about how the baby will sleep**, **never imply the
app monitors the baby** (AAP 2022), and **document everything**.

---

## 1. The owner's suggestions

### 1.1 Celebrations: a small "parabéns" when what she was trying for happens

**The idea (owner):** when a parent's intent is met, for example the baby slept as long as
expected, Cochilo answers with one short congratulations animation: a few warm words and an
animated baby.

**Why it's right.** Tracking a baby's sleep is mostly logging what went wrong at 3 a.m. Peak-end
(impeccable's emotional-journey lens): people remember the best moment and the last one. A small,
earned celebration turns the end of a good nap into a peak. It also rewards the *logging*, which
is what makes the predictions good (research §1.7: apps need about a week of data).

**What counts as "intent met" (each from data the app already has):**

| Moment | Trigger | Example words (EN / PT) |
|---|---|---|
| Nap as long as expected | Woke up at or after the predicted wake (`expectedWake`), nap ≥ 30 min | "A full nap. Well done, both of you." / "Uma soneca inteira. Muito bem, vocês dois." |
| Nap started in the window | Start sleep inside the predicted window | "Right on time." / "Na hora certinha." |
| Longest night stretch this week | A night sleep segment longer than any in the last 7 nights | "His longest stretch this week." / "O maior sono da semana." |
| Daylight done | ≥ 30 min daylight logged between 12:00 and 16:00 (research §2.3) | "Daylight in the bag." / "Luz do dia: feito." |
| Routine before bed | A bedtime routine logged before the night sleep | "Routine done. Good night." / "Rotina feita. Boa noite." |
| A week of logging | Entries on 7 days in a row | "A whole week logged. Predictions get sharper now." / "Uma semana inteira registrada. As previsões ficam mais certeiras." |

**How it looks and moves.**
- One moment, not confetti everywhere: the baby avatar (1.2) rises from the bottom of the card
  that changed (the sleep panel, the row just logged), does one small happy motion (a stretch, a
  smile, arms up), with one line of text beside it, then settles away. About 1.2–1.6 s,
  `motion-emphasised` easing, from an already-visible default (craft floor).
- Colours from her fabrics (the event colour of what was celebrated), never teal (teal is the
  action).
- Tap anywhere to dismiss; it never blocks a tap or waits for one (fewest taps).
- **At night it is quieter:** between bedtime and morning, no bright flash and no movement on the
  dark screen; the words only, dim, for 1 s. Light at night is the one thing research says to
  avoid (§2.3).
- **Remove animations on:** a still picture of the baby and the words, then gone.
- TalkBack: the words are announced once (polite live region).

**Rules so it stays kind.**
- **Celebrate, never scold.** No "missed", no streaks that break, no red. A day without a
  celebration is just a day.
- **Never a promise.** "A full nap" describes what happened; it never says "she will sleep through
  tonight" (brand voice).
- **Rare enough to mean something:** at most one celebration per moment kind per day, and at most
  three per day overall.
- **A switch in Settings** ("Little celebrations" / "Pequenas comemorações"), on by default.
- Copy reviewed by Tatá (brand voice: warm, short, from one mother to another).

**Build notes.** A pure `Celebrations` rule set in `domain/` (input: log, prediction, now; output:
the moment or none), unit-tested like `Predictor`. One `Celebration` component in `ui/kit`
(design system first: component + motion spec). Animation drawn in Compose (vector parts on a
Canvas, transforms from `Motion.kt`), no new animation library. Screenshot tests of the resting
frame in light, dark and night.

### 1.2 The baby's avatar: chosen at onboarding, used by the celebrations

**The idea (owner):** in the baby's profile, when setting up, the parent picks how the baby looks,
so the animation looks like *her* baby.

**What to ask, in words a parent would use (and the brand voice):**
- **Skin tone** ("Tom de pele"): six swatches, light to deep (the same range as emoji skin tones).
  The app asks for how the baby *looks*, not for race or ethnicity: that is what the drawing needs,
  it is more accurate (families are mixed), and it is kinder to ask.
- **Hair** ("Cabelo"): none yet / a little / curly / straight / wavy, and a colour (black, brown,
  blond, red).
- **Boy, girl, or "rather not say"** ("Menino, menina ou prefiro não dizer"): changes small details
  only (an optional bow or none); "rather not say" draws a neutral baby. Never changes colours to
  pink/blue by gender; the app's palette is her fabrics.
- A live preview of the baby while choosing; every choice one tap; all optional, with a default.

**Where the avatar appears.** The celebrations; the onboarding welcome; the empty Today ("Ready
when Bebê is"); optionally the header in place of the moon mark (a Settings choice). Never on
data screens where it would compete with the numbers.

**Style.** The brand's illustration style (`docs/tata-slings/design-system.md` §6): single-weight
line with rounded caps, flat fills from the fabric palette, the same stroke as the icon set. Drawn
as layered vectors (head, face, hair, body, bow) so every combination is one drawing, and the
animation moves parts (blink, smile, arms) rather than swapping frames.

**Data.** Stored with the baby (Room migration), included in the backup (bump
`BackupFile.VERSION`, still read older files), never sent anywhere. Editable later in Settings ›
Baby › Appearance.

**Needs first:** a real onboarding (3.1) and the design-system illustration set.

---

## 2. Features from the research

Each is grounded in `docs/research.md` or the Tatá Slings plans (`docs/tata-slings/`).

| Feature | Why (source) | What it is |
|---|---|---|
| **Where the baby slept** (crib, arms, sling, pram, car) | Contact naps are normal early on; the brand is babywearing (monetization §6.1) | A kind on Sleep; Patterns shows "Naps in the sling: 3 this week", guilt-free; the `eventCarry` teal data colour is reserved for it |
| **Night light theme** | Evening and night light suppress melatonin even when dim; red/amber is least disruptive (research §2.3) | An automatic theme from bedtime to morning: very dim, warm amber/red on near-black, no teal glow; one tap to leave. Turns Tatá's "yellow or red light?" question into a feature |
| **Wind-down reminder** | Same (§2.3, §7.3) | ~60 min before the predicted bedtime: "Dim the lights; warm light only." Off by default; never a promise |
| **Daylight nudge** | Daylight 12:00–16:00 linked to better nights at 6–12 weeks (§2.3) | If no daylight is logged by 14:00, a quiet line on Today (not a notification) |
| **Change detection instead of "regressions"** | Don't hard-code regressions; detect real change (§2.5) | "Night wakings: more this week than the two before (3 vs 1.5)", neutral, with its numbers, like the prediction card |
| **Day milk / night milk** | Chrononutrition (§2.4) | Pumped milk tagged by the time it was pumped; bottles say which milk |
| **Ritual builder** | Preventive parent education has the strongest evidence (§4); what consultations sell (monetization §6.4) | The parent writes the steps once (feed, play, nappy, room ready, song) and ticks them off; logged as the routine |
| **Safe babywearing check (T.I.C.K.S.)** | AAP 2022; the brand's expertise (monetization §6.3) | Shown once, the first time "sling" is logged, by Tatá, with her video link. Never "the app checks your baby" |
| **Notes from Tatá by age** | The brand voice; trust (monetization §6.5) | One short note per age band, signed, reviewed against AAP 2022 |
| **Printable report** | Paediatrician visits (monetization §3) | A PDF made on the phone (sleep per day, feeds, the actogram), shared by the parent |
| **Smarter prediction (phase 2)** | Two-process model (§2.1, §7.2) | Weight recent days more, separate first nap / later naps / bedtime, still showing its numbers |

## 3. UX improvements

### 3.1 Onboarding (needed for the avatar)
Today the app asks for the baby's name and birth date in a sheet. A first-run flow of three short
screens, each skippable: the baby (name, birth date, due date if early) → how the baby looks
(1.2) → "Log the first sleep" with the widget offered. Remember actions applies: nothing asked
twice. Brand voice: warm, one line per screen.

### 3.2 Logging, faster still
- **"Fell asleep a few minutes ago"**: long-press Start sleep (or the widget) for 5 / 10 / 15 min
  ago (flows.md next candidates).
- **Quick-settings tile** for Start sleep / Woke up.
- **Drag to adjust** a sleep's start or end on the Day tab's timeline, instead of opening the sheet.
- **Wear OS tile**: Start sleep / Woke up and the breasts from the wrist, for when both hands hold
  the baby.

### 3.3 Two parents
- **Second phone**: today through export/import. Next: a local Wi-Fi sync between the two phones
  (no server, no account; the data still never leaves the home).
- **Who logged it**: an optional initial on entries when two phones sync.

### 3.4 Reading the data
- **Week view** on Patterns with a plain sentence first ("Night sleep is getting longer: 8h10 vs
  7h30 last week"), then the chart.
- **Search and filter** History by kind.
- **The Day tab's ring** with the sleep windows the prediction expected, faint, so a parent sees
  "on time" at a glance.

## 4. Betterments (quality)

| Item | Why |
|---|---|
| **Dates in the phone's language**: the Portuguese screenshots show "Thu 9 Jul" (Settings' birth date) | `Format.day` already formats in the configuration's locale, so this may be the screenshot tool's locale only; check on a Portuguese phone and fix if real |
| **200% font scale pass** | Audit v0.9.0 P3: the Log menu's labels ellipsize at large font sizes |
| **Tablet and foldable layout** | Android guidance: a navigation rail and two panes on wide screens |
| **Upstream the design-system patches** | Six patches wait in `design-system/`; merging them makes the system the source of truth again |
| **Seamless fabric tiles** | Prints mirror at their edges (Tropical); retouch each photo into a true repeating tile |
| **Encrypted backup** (optional passphrase) | The backup is the only way data leaves the phone |
| **On-device checks with TalkBack** | Paparazzi checks pixels, not the reading order |
| **Predictor tests on a year of synthetic data** | Every rule tested, as the product rule asks |

## 5. Priorities

| When | What | Why first |
|---|---|---|
| **Next (0.13)** | Onboarding (3.1) with the baby's avatar (1.2); check the dates in Portuguese (4) | The owner's request; the avatar needs onboarding |
| **Then (0.14)** | Celebrations (1.1) on the avatar | Builds on 0.13; the first "peak" moment |
| **Soon** | Night light theme; "fell asleep a few minutes ago"; where the baby slept | Research-backed, small, and on the brand's subject |
| **Later** | Ritual builder, T.I.C.K.S., notes from Tatá, printable report (the Cochilo+ set) | Needs Tatá's writing and the monetization plan's order of work |
| **Exploring** | Wi-Fi sync between two phones; Wear OS; smarter prediction | Bigger builds; worth a design spike first |

Each of these lands like the rest: design system first, docs with the change, screenshots looked
at, a PR, merged and released with a note in English and Portuguese.
