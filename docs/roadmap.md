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

### 1.1 Celebrations: shipped in 0.14.0 ([celebrations.md](celebrations.md))

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

- **0.14.0**: little celebrations (1.1) ([celebrations.md](celebrations.md)).
- **0.13.0**: onboarding and the baby's avatar (1.2, 3.1); dates in the app's language
  ([onboarding.md](onboarding.md)).

## 5. Priorities

| When | What | Why first |
|---|---|---|
| **Soon** | Night light theme; "fell asleep a few minutes ago"; where the baby slept | Research-backed, small, and on the brand's subject |
| **Later** | Ritual builder, T.I.C.K.S., notes from Tatá, printable report (the Cochilo+ set) | Needs Tatá's writing and the monetization plan's order of work |
| **Exploring** | Wi-Fi sync between two phones; Wear OS; smarter prediction | Bigger builds; worth a design spike first |

Each of these lands like the rest: design system first, docs with the change, screenshots looked
at, a PR, merged and released with a note in English and Portuguese.
