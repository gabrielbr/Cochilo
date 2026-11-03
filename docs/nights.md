# Nights: the night as one thing (0.42.0)

The owner's question: Cochilo didn't separate the naps from the real night. Shouldn't there be
"asleep for good, until the next day", with interruptions during the night? It matters because
naps, bedtime and the night's numbers are what the app is for, and long, calm nights are what
parents most want.

## What was wrong

Until 0.41 a sleep was "night" when it **started between 19:00 and 07:00**, and the night's wakings
were the night sleeps in that window minus one. At 3 months, while the body clock is still forming
(research §2.2), that misread ordinary days:

| What happened | What Cochilo counted | What it is |
|---|---|---|
| A catnap at 19:20, bedtime at 21:30 | Bedtime 19:20, plus one night waking | A nap, then bedtime 21:30 |
| A sleep from 18:40 that became the night | A long nap; no bedtime that day | Bedtime 18:40 |
| A first nap at 06:50 | Night sleep | A nap |

Every night number depended on that label:
- the bedtime the predictor learns;
- the longest last window before bed;
- night sleep and night wakings;
- the longest stretch;
- naps a day;
- "What changed";
- the longest-night celebration.

## How it works now

- **Each sleep says nap or night** (`Event.night`). **Start sleep is still one tap**: Cochilo guesses
  (`Nights.guess`), and the sleep card says which it chose ("Night sleep for" / "Napping for"), with
  one quiet tap to change it ("It's a nap" / "It's the night").
- **The guess:**
  - during a night waking, it's the night (back to sleep);
  - up to 05:00, it's the night;
  - before 16:00, it's a nap;
  - in the evening, it's the night from 75 minutes before the baby's own learned bedtime, or from
    18:30 until a bedtime is learned.
- **A night waking doesn't end the night.** "Woke up" during the night ends that stretch, and Today
  shows **Awake in the night**, with how long and "Night since 21:30 · sleeps: 2". There's no
  next-nap guess (it would be wrong at 3am), and no "where did she sleep?" question in the dark.
  Two buttons:
  - **Back to sleep**, the one mint action until 05:00. It continues the night.
  - **Up for the day**, the one mint action from 05:00. It ends the night: the last stretch is marked
    (`Event.upForDay`), and its end is the morning wake. The place question comes then, once for
    the whole night.
- **A night with no sleep for 4 hours is over**, even without Up for the day, so a forgotten tap
  doesn't keep a night open.
- **The numbers follow the night** (`Nights.of`):
  - bedtime is the first stretch's start;
  - night wakings are the gaps between its stretches (`Nights.wakings` has their minutes);
  - it runs from the evening to up for the day, not by the clock.

  Night sleep, naps, the longest stretch, bedtime and the predictor's windows all read each sleep's
  own label (`isNightSleep`).
- **Older entries keep the clock rule** (a null `night`), so nothing in the log changes meaning
  overnight.
- **Napper imports** come with their real nights: the parts between Napper's bedtime and wake-up
  are night, and the last one is up for the day. A Napper nap is a nap.

## Everywhere the night shows

| Where | Asleep at night | Awake in the night |
|---|---|---|
| Today | "Night sleep for", Woke up, "It's a nap" | "Awake in the night", Back to sleep / Up for the day |
| Live notification | Night, Woke up | "Awake in the night", Back to sleep + Up for the day |
| Home-screen widget | Woke up | Back to sleep |
| Quick-settings tile | Woke up | Start sleep, which continues the night |

## Data

- `events.night` and `events.upForDay`: nullable, added by Room migration 3 → 4 (`CochiloDb.NIGHTS`).
- Backups are version 4. Version 1–3 files still read; their sleeps keep the clock rule.
- Code:
  - `domain/Nights.kt` (`guess`, `openNight`, `of`, `containing`, `wakings`), tested in `NightsTest`;
  - `Entries.startSleep` (it guesses), `upForDay` and `setNight`;
  - `Stats.nightWakings`.
