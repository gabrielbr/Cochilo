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

### 1.2 The baby's avatar: shipped in 0.13.0

Chosen at onboarding (skin tone, hair, girl / boy / rather not say), drawn in a Rosa Chá sling.
Now in [onboarding.md](onboarding.md). The celebrations (1.1) use it next.

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

### 3.1 Onboarding: shipped in 0.13.0 ([onboarding.md](onboarding.md))

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
| **200% font scale pass** | Audit v0.9.0 P3: the Log menu's labels ellipsize at large font sizes |
| **Tablet and foldable layout** | Android guidance: a navigation rail and two panes on wide screens |
| **Upstream the design-system patches** | Six patches wait in `design-system/`; merging them makes the system the source of truth again |
| **Seamless fabric tiles** | Prints mirror at their edges (Tropical); retouch each photo into a true repeating tile |
| **Encrypted backup** (optional passphrase) | The backup is the only way data leaves the phone |
| **On-device checks with TalkBack** | Paparazzi checks pixels, not the reading order |
| **Predictor tests on a year of synthetic data** | Every rule tested, as the product rule asks |

## Shipped from this plan

- **0.13.0**: onboarding and the baby's avatar (1.2, 3.1); dates in the app's language
  ([onboarding.md](onboarding.md)).

## 5. Priorities

| When | What | Why first |
|---|---|---|
| **Then (0.14)** | Celebrations (1.1) on the avatar | Builds on 0.13; the first "peak" moment |
| **Soon** | Night light theme; "fell asleep a few minutes ago"; where the baby slept | Research-backed, small, and on the brand's subject |
| **Later** | Ritual builder, T.I.C.K.S., notes from Tatá, printable report (the Cochilo+ set) | Needs Tatá's writing and the monetization plan's order of work |
| **Exploring** | Wi-Fi sync between two phones; Wear OS; smarter prediction | Bigger builds; worth a design spike first |

Each of these lands like the rest: design system first, docs with the change, screenshots looked
at, a PR, merged and released with a note in English and Portuguese.
