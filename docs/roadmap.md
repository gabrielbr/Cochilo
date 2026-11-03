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
| **Notes from Tatá by age** | The brand voice; trust (monetization §6.5) | Built in 0.34.0 with twelve drafts, off; waiting for Tatá's words ([in-app-marketing.md](tata-slings/in-app-marketing.md) §11) |
| **Prediction, the clock part** | Two-process model (§2.1, §7.2) | Process C: a fitted circadian curve (bedtime and the first nap pulled towards the baby's own clock as it forms), and a live sleep-pressure line on the Day tab |

## 3. UX improvements

### 3.1 Onboarding: shipped in 0.13.0 ([onboarding.md](onboarding.md))

### 3.2 Logging, faster still
- **Drag to adjust the start** too (0.27.0 moves the end; a late start is covered by the Earlier chips).
- **Wear OS tile**: Start sleep / Woke up and the breasts from the wrist, for when both hands hold
  the baby.

### 3.2b Ask Cochilo, next

- Set the `ASSISTANT_MODEL_URL` secret so the Play pack carries the model (a Gemma file the store
  may redistribute under Gemma's terms).
- Upgrade Kotlin, then swap MediaPipe for LiteRT-LM in `ai/LocalModel.kt`; measure speed and
  battery on a 4 GB phone; consider Gemma 3n for phones that can take it.

### 3.2c Tatá Slings inside the app

The non-intrusive plan in [tata-slings/in-app-marketing.md](tata-slings/in-app-marketing.md):
notes by age and log moments in Patterns (step 1, the fabric line, shipped in 0.31.0; step 4's
first moment, naps in arms, live in 0.36.0), never on Today, at night, or on safety screens. The
other moments and the age notes need Tatá's sign-off.

### 3.3 Two parents
- **Second phone**: today through export/import. Next: a local Wi-Fi sync between the two phones
  (no server, no account; the data still never leaves the home).
- **Who logged it**: an optional initial on entries when two phones sync.

### 3.4 Reading the data

## 4. Betterments (quality)

| Item | Why |
|---|---|
| **Upstream the design-system patches** | Six patches wait in `design-system/`; merging them makes the system the source of truth again |
| **On-device checks with TalkBack** | Paparazzi checks pixels, not the reading order |

## Shipped from this plan

- **0.36.0**: Tatá marketing step 4 starts: the naps-in-arms suggestion is live, signed off by Tatá ([in-app-marketing.md](tata-slings/in-app-marketing.md) §12).
- **0.35.0**: asleep and awake in the timelines, and the Log menu's cascade (the owner's ask; [flows.md](flows.md)).
- **0.34.0**: Tatá marketing step 3: notes by age, twelve drafts, all off until Tatá approves ([in-app-marketing.md](tata-slings/in-app-marketing.md)).
- **0.33.0**: window reminders, 30 and 15 minutes before the next sleep window ([notifications.md](notifications.md)).
- **0.32.0**: Tatá marketing step 2: the suggestion rules, the catalog and the Tatá note, all off until Tatá approves ([in-app-marketing.md](tata-slings/in-app-marketing.md)).
- **0.31.0**: Tatá marketing step 1: the fabric's shop line; the safety sheet links to her guide ([in-app-marketing.md](tata-slings/in-app-marketing.md)).
- **0.30.0**: Ask Cochilo, phase 2: the optional on-device model ([assistant.md](assistant.md)).
- **0.29.0**: Ask Cochilo, phase 1: a local assistant from the log and reviewed answers ([assistant.md](assistant.md)).
- **0.28.0**: details after the main action; Day's timeline newest first ([flows.md](flows.md)).
- **0.27.0**: drag to adjust a sleep's end on the Day tab ([flows.md](flows.md)).
- **0.26.0**: seamless fabric tiles ([textures.md](tata-slings/textures.md)).
- **0.25.0**: tablets and foldables, a rail and two panes ([tablet.md](tablet.md)).
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
| **Later** | Notes from Tatá (waiting for her writing) | Needs Tatá's writing and the monetization plan's order of work |
| **Exploring** | Wi-Fi sync between two phones; Wear OS; the prediction's clock part | Bigger builds; worth a design spike first |

Each of these lands like the rest: design system first, docs with the change, screenshots looked
at, a PR, merged and released with a note in English and Portuguese.
