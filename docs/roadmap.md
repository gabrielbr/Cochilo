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
| **Notes from Tatá by age** | The brand voice; trust (monetization §6.5) | One short note per age band, signed, reviewed against AAP 2022 |
| **Prediction, the clock part** | Two-process model (§2.1, §7.2) | Process C: a fitted circadian curve (bedtime and the first nap pulled towards the baby's own clock as it forms), and a live sleep-pressure line on the Day tab |

## 3. UX improvements

### 3.1 Onboarding: shipped in 0.13.0 ([onboarding.md](onboarding.md))

### 3.2 Logging, faster still
- **Drag to adjust** a sleep's start or end on the Day tab's timeline, instead of opening the sheet.
- **Wear OS tile**: Start sleep / Woke up and the breasts from the wrist, for when both hands hold
  the baby.

### 3.3 Two parents
- **Second phone**: today through export/import. Next: a local Wi-Fi sync between the two phones
  (no server, no account; the data still never leaves the home).
- **Who logged it**: an optional initial on entries when two phones sync.

### 3.4 Reading the data

## 4. Betterments (quality)

| Item | Why |
|---|---|
| **Tablet and foldable layout** | Android guidance: a navigation rail and two panes on wide screens |
| **Upstream the design-system patches** | Six patches wait in `design-system/`; merging them makes the system the source of truth again |
| **Seamless fabric tiles** | Prints mirror at their edges (Tropical); retouch each photo into a true repeating tile |
| **On-device checks with TalkBack** | Paparazzi checks pixels, not the reading order |

## Shipped from this plan

- **0.24.0**: the last nap's length in the prediction (phase 2, sleep pressure) and a synthetic year of tests ([prediction.md](prediction.md)).
- **0.23.0**: locked backups with an optional passphrase ([backup.md](backup.md)).
- **0.22.0**: large text, 200% font scale ([accessibility.md](accessibility.md)).
- **0.21.0**: History filters by kind; the expected window on today's ring ([flows.md](flows.md)).
- **0.20.0**: the report for the doctor (PDF) ([report.md](report.md)).
- **0.19.0**: the ritual builder ([rituals.md](rituals.md)).
- **0.18.0**: what changed (change detection, the week's plain sentences first) and day / night milk ([changes.md](changes.md)).
- **0.17.0**: where the baby slept, naps by place in Patterns, the T.I.C.K.S. check ([places.md](places.md)).
- **0.16.0**: "Fell asleep a few minutes ago" (Earlier chips) and the quick-settings tile ([flows.md](flows.md)).
- **0.15.0**: night light, daylight nudge, wind-down in the notification ([night-light.md](night-light.md)).
- **0.14.0**: little celebrations (1.1) ([celebrations.md](celebrations.md)).
- **0.13.0**: onboarding and the baby's avatar (1.2, 3.1); dates in the app's language
  ([onboarding.md](onboarding.md)).

## 5. Priorities

| When | What | Why first |
|---|---|---|
| **Soon** | Quality pass: seamless fabric tiles, tablet layout | Research-backed, small, and on the brand's subject |
| **Later** | Notes from Tatá (waiting for her writing) | Needs Tatá's writing and the monetization plan's order of work |
| **Exploring** | Wi-Fi sync between two phones; Wear OS; the prediction's clock part | Bigger builds; worth a design spike first |

Each of these lands like the rest: design system first, docs with the change, screenshots looked
at, a PR, merged and released with a note in English and Portuguese.
