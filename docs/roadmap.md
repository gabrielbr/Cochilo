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
| **Prediction, phase 3 (learned)** | research §7.2 | A small model on time of day, last nap, daylight, routine and feeds, once there are months of log |

## 3. UX improvements

### 3.1 Onboarding: shipped in 0.13.0 ([onboarding.md](onboarding.md))

### 3.2 Logging, faster still
- **The watch app on Play**, with the play build, so it installs with the phone's.

### 3.2b Ask Cochilo, next

- Set the `ASSISTANT_MODEL_URL` secret so the Play pack carries the model (a Gemma file the store
  may redistribute under Gemma's terms).
- Run the on-device test plan ([testing-on-device.md](testing-on-device.md)): LiteRT-LM on a 4 GB
  phone, Gemma 3n on an 8 GB one.

### 3.2c Tatá Slings inside the app

The non-intrusive plan in [tata-slings/in-app-marketing.md](tata-slings/in-app-marketing.md):
notes by age and log moments in the From Tatá tab: all twelve age notes since 0.44.0, all four
moments since 0.51.0. Next: the swaddle and a bigger baby's carrier, when the shop has one, and
only with Tatá.

### 3.3 Two parents
- **Partner sync in the play build** would need the owner to allow INTERNET there.
- **Background sync after a restart** without opening Cochilo would need `RECEIVE_BOOT_COMPLETED`.

### 3.4 Reading the data

### Routine music, more places (0.60.0 suggestions, ux-user-flows)
- A Play music button on the home-screen widget.
- Play music on the watch tile (needs the phone to open the app from a watch message).
- An opt-in "Play music when sleep starts" (Start sleep, the tile, the widget, the reminder's Start
  routine).

## 4. Betterments (quality)

| Item | Why |
|---|---|
| **On-device checks with TalkBack** | Paparazzi checks pixels, not the reading order |

## Shipped from this plan
- **0.63.0**: share a playlist from Spotify or YouTube Music straight to Routine music.
- **0.63.0** (with 0.62.0): the plan starts with the live guess, so the next nap shows on the ring and the list.
- **0.61.0**: every projected sleep on the hour-by-hour list; dotted expected lines; stronger breathing.
- **0.60.0**: the routine music one tap away: Today, the live notification, the reminder.
- **0.59.0**: the next sleep in breathing teal; every projected sleep's time on the ring (the owner's ask).
- **0.58.0**: the day ring floats (bigger marks, times along the circle); routine music (the owner's asks).
- **0.57.0**: the model download carries on after an interruption (100 MB pieces, the owner's ask).
- **0.56.0**: one AI runtime (MediaPipe removed); the watch tile's Last mark; every audit green.
- **0.55.0**: Kotlin 2.2; Ask Cochilo on LiteRT-LM; Gemma 3n for 8 GB phones.
- **0.54.0**: the prediction's clock (process C) and the sleep-pressure line.
- **0.53.0**: drag a sleep's start; the Wear OS tile.
- **0.52.0**: who logged it; partner sync with the app closed.
- **0.51.0**: Tatá's other three moments; one legend per screen; Settings' second lines; a quieter
  notification at night.

- **0.50.0**: the next expected sleep on the Day timeline ([prediction.md](prediction.md)).
- **0.49.0**: the rest of the day projected, on Today and the Day ring ([prediction.md](prediction.md)).
- **0.48.0**: the Day timeline on two tracks ([flows.md](flows.md)).
- **0.47.0**: partner sync on the home Wi-Fi, with deletions ([sync.md](sync.md)).
- **0.46.0**: the Day timeline's bars follow how long each entry lasted ([flows.md](flows.md)).
- **0.45.0**: Today without Day by day; the kind filter on the Day tab; the Ask Cochilo glimpse; a wider fabric band ([flows.md](flows.md)).
- **0.44.0**: From Tatá, a tab of its own, with all twelve age notes ([in-app-marketing.md](tata-slings/in-app-marketing.md) §13).
- **0.43.0**: less is more: Today, Day and Patterns decluttered, the night first in Patterns ([flows.md](flows.md)).
- **0.42.0**: the night as one thing: nap or night per sleep, night wakings, up for the day ([nights.md](nights.md)).
- **0.41.1**: the design system released 0.28.0 with the generic parts of Cochilo's patches; Cochilo re-synced to it and deleted the patches ([design/core/CORE.md](../design/core/CORE.md)).
- **0.41.0**: Share to Cochilo: Napper's export (or a backup) from any app's share sheet imports at once ([backup.md](backup.md)).
- **0.40.1**: the design in three layers in `design/` (core vendored, Tatá brand, Cochilo layer); the design system stays generic ([design/README.md](../design/README.md)).
- **0.40.0**: History moves to the end of Today (three tabs); Ask keeps chats, one per topic ([flows.md](flows.md), [assistant.md](assistant.md)).
- **0.39.0**: Today without the log rows; the Log button does it all (the owner's ask; [flows.md](flows.md)).
- **0.38.0**: Ask Cochilo in the header of every tab, a nap-length answer, and the whole log for the on-device AI ([assistant.md](assistant.md)).
- **0.37.0**: the on-device AI downloads itself in one tap on the family build, from this repository's releases ([assistant.md](assistant.md)).
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

| Release | What |
|---|---|

Each of these lands like the rest: design system first, docs with the change, screenshots looked
at, a PR, merged and released with a note in English and Portuguese.
