# Cochilo

A personal Android app for tracking a baby's sleep: one tap to start and stop a sleep, logs for
feeds, pumping, diapers, daylight, routines and notes, and a prediction of the next nap or bedtime
learned from the baby's own history. Everything stays on the phone; backups are files you export
and import yourself.

**Cochilo, da Tatá Slings**: a [Tatá Slings](https://tataslings.com.br) product. Built on the
[Gabriel Design System](https://github.com/gabrielbr/Gabriel-Deisgn-System) (the core, vendored in
[design/core/](design/README.md)), dressed in the Tatá Slings brand ([design/brand/](design/brand/tata-slings/brand.md)) (teal, plum-grey ink, Pérola, Libre Franklin, the moon-in-a-sling mark, and each kind of entry in one of her fabric colours;
[docs/tata-slings/](docs/tata-slings/README.md)). The research behind it is in
[docs/research.md](docs/research.md). What changed in each release: [CHANGELOG.md](CHANGELOG.md).

| Today | Asleep (dark) | Patterns | Hoje (pt-BR) |
|---|---|---|---|
| ![Today](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_today_light.png) | ![Sleeping](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_today_sleeping_dark.png) | ![Patterns](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_patterns_light.png) | ![Hoje](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_today_ptBR_light.png) |

Every screen and the main flows are on the [design canvas](docs/design-canvas.md), for comments.

## What it does

- **Today**: awake/asleep timer with one action (Start sleep / Woke up), the next nap or bedtime
  with its window and the reason for it, running timers (a feed, a walk outside), quick log
  buttons (since 0.39.0 the Log button in the corner, not a list of rows), and at the end **Ask
  Cochilo** (since 0.45.0): the one question that stands out in her log, with her numbers and
  what to try, one tap to the full answer. The card is wrapped in her fabric, with a wider band
  since 0.45.0. Past days are on the Day tab (its arrows), with the kind filter. Tap any entry to
  edit or delete it.
- **The rest of the day** (since 0.49.0): besides the next sleep, Cochilo projects every nap
  left today and bedtime. Today's card says "Then: 13:50 · 16:05 · bed 18:45", and the Day ring
  shows them as dashed outlines. Since 0.50.0 the Day timeline also shows the next one,
  dashed, above what's happening now. The later ones have wider ranges, and each log entry
  recomputes them ([docs/prediction.md](docs/prediction.md)).
- **Partner sync** (since 0.47.0, family build): pair the two parents' phones once (Settings ›
  Sync, compare six digits). While Cochilo is open on both on the home Wi-Fi, every change, a
  deletion included, goes across in seconds, directly between the phones, sealed. There's no
  server and no account. Since 0.52.0 it can keep going with Cochilo closed (one switch, on the
  notification Cochilo already shows), and each parent can set an initial, so the other phone
  shows "by G" on what they logged ([docs/sync.md](docs/sync.md)).
- **Nights** (since 0.42.0): each sleep is a nap or the night. Start sleep guesses which, and
  one tap changes it. Waking at night shows "Awake in the night" with **Back to sleep** and **Up
  for the day**, so a night with wakings is one night: bedtime, wakings and the morning are counted
  right. [docs/nights.md](docs/nights.md).
- **Day**: one day two ways. A 24-hour ring (midnight at the top; sleep on the outer ring, feeds,
  pumping and daylight inside, dots for diapers) with the total sleep in the middle, then every
  entry hour by hour on two tracks, like the ring: the main track holds the day's two states, each
  sleep (a filled bar) and each awake stretch (an outlined bar), newest first. The secondary track,
  beside each bar, holds every feed, diaper or walk that happened during it: an outline in its
  colour, a plain icon and smaller words (since 0.48.0). Since 0.46.0 every bar's height follows how long it lasted
  (not 1:1: two hours of sleep is clearly longer than ten minutes of feeding, and a night still
  fits the screen). Arrows move between days; tap an entry
  to edit it.
- **Four tabs** since 0.44.0: Today, Day, Patterns and From Tatá (her notes by age, this week's
  tip, her shop). History lives at the end of Today: every day
  with its summary, timeline and entries, grouped the same way (asleep and awake, with what
  happened during each set in); filtered to one kind, a plain list.
- **Icons**: every kind of entry has its line icon (a drop, a bottle, a spoon, a pump, a diaper,
  the moon, the sun, a bath, a capsule, a pencil), always beside its name (`ui/kit/Icons.kt`).
- **Medicine**: its own entry type and log row; the name and dose go in the note.
- **Log button**: the main action, in the bottom-right corner on every tab. Press, drag up to a
  kind and sideways to a choice (Feed › Right breast), let go: logged. Let go on the kind for its
  sheet. A tap opens it for taps.
- **Filters and the expected window**: the days on Today by kind in one tap; today's ring shows when the
  next sleep is expected. [docs/flows.md](docs/flows.md).
- **Report for the doctor**: a one-page PDF of the last two weeks, made on the phone.
  [docs/report.md](docs/report.md).
- **Routine steps**: write the nap and bedtime steps once, tick them each time.
  [docs/rituals.md](docs/rituals.md).
- **What changed**: real changes in the last week, in neutral words; day / night milk.
  [docs/changes.md](docs/changes.md).
- **Ask Cochilo** (the chat icon at the top of every tab): chats, one per topic, saved on the phone (since 0.40.0); questions answered from the baby's own log
  and the app's research, on the phone, in English and Portuguese; health, medicines and the
  mother's wellbeing go to a person. Not medical advice. Optional since 0.30.0: a small AI model on
  the phone for questions beyond the list, downloaded in one tap when you want it (by the Play
  Store, or since 0.37.0 by the family build itself, on Wi-Fi, in the background), given the whole log (each day, today's sleeps, this
  week, the age references) and checked before it's shown. [docs/assistant.md](docs/assistant.md).
- **Details after**: start a sleep (or a feed, pumping, daylight) in one tap; where she slept, which
  side or how much is asked once it ends, with the last answer marked. [docs/flows.md](docs/flows.md).
- **Where the baby slept**: crib, arms, sling, pram, car, asked on waking; naps by place in Patterns;
  T.I.C.K.S. the first time the sling is picked. [docs/places.md](docs/places.md).
- **Earlier and the tile**: "Fell asleep 5 · 10 · 15 min ago" in one tap; Start sleep / Woke up from
  the quick-settings tile. [docs/flows.md](docs/flows.md).
- **Night light**: warm amber on black from 19:00 to 07:00; a daylight nudge in the afternoon;
  "dim the lights" before bedtime. [docs/night-light.md](docs/night-light.md).
- **Little celebrations**: a few warm words and the baby lifting its arms when a nap goes well.
  [docs/celebrations.md](docs/celebrations.md).
- **Meet the baby**: a two-step first run, and the baby's avatar (skin tone, hair, girl / boy /
  rather not say) in a Rosa Chá sling. [docs/onboarding.md](docs/onboarding.md).
- **What's next**: the plan, with the owner's suggestions (celebrations, the baby's avatar),
  research-based features and UX work: [docs/roadmap.md](docs/roadmap.md).
- **Soft and warm**: plain titles, teal only on the one action, a blush for what's picked,
  sentence-case captions and pill buttons ([docs/design-review.md](docs/design-review.md)).
- **Home-screen widget**: Start sleep / Woke up and Left / Right, one tap each, with a running
  clock. [docs/widget.md](docs/widget.md).
- **On the watch** (since 0.53.0): a Wear OS tile with Start sleep / Woke up and the two breasts,
  one tap each, for when both hands hold the baby. It talks to the phone only when connected
  directly, never through the cloud. [docs/wear.md](docs/wear.md).
- **Sleep first, and motion**: sleep is a size up in every list; every action answers with a small
  movement (off with Remove animations); what was used last has a coloured outline and dot.
  [docs/flows.md](docs/flows.md).
- **Her fabrics**: Settings › Appearance › Fabric wraps the Today card in one of Tatá's slings
  (eleven, photographed from her shop); under the picked one, a quiet link to that sling in the shop
  ([in-app-marketing.md](docs/tata-slings/in-app-marketing.md)). [docs/tata-slings/textures.md](docs/tata-slings/textures.md).
- **From Tatá** (a tab since 0.44.0): her note for the baby's age, the next one, the earlier
  ones one tap away, and her shop. **Tatá's suggestions** appear there too ("This week"), rarely,
  when the baby's own log makes one of her products useful. Since 0.51.0 all four are on: lots of naps in arms in the first three months
  → her sling; a steady bedtime routine → the Naninha; little daylight → a walk in the sling;
  around six months → her TatáT.O. toys. Never at night, while the baby sleeps, in the first week, or after "Don't show
  again"; Settings › Notifications turns them all off
  ([in-app-marketing.md](docs/tata-slings/in-app-marketing.md)).
- **The Log button** (the round + in the corner of every tab): press, drag to a kind and its
  option (Feed › Left or Right, Diaper › Wet · Both · Dirty…), let go, and it's logged; tap a kind
  for its full sheet. What was used last is marked. Today has no list of log rows since 0.39.0:
  the Log button does it all. Slide a running breastfeed to switch side. How many taps each entry
  costs, and why: [docs/flows.md](docs/flows.md).
- **Notifications**: a quiet live notification, asleep or awake, with a running clock, the expected
  wake or next nap, a timeline bar and Woke up / Start sleep; running timers get Finish (and Switch
  side). Window reminders 30 and 15 minutes before the next nap or bedtime, with Start routine.
  [docs/notifications.md](docs/notifications.md).
- **"i" buttons**: each kind of entry's sheet has
  an "i" that explains what it's for, in English and Portuguese.
- **Patterns**: a 14-day actogram (the day/night rhythm forming) and 7-day averages next to the
  reference range for the baby's age.
- **Drag to adjust**: on the Day tab, hold a sleep and drag to change when it ended, in 5-minute
  steps, with Undo. [docs/flows.md](docs/flows.md).
- **Tablets and foldables**: a rail instead of the tab bar from 600dp, and Today beside a second
  tab from 840dp (an unfolded foldable). [docs/tablet.md](docs/tablet.md).
- **Large text**: every screen works up to the phone's 200% font size; chips wrap, nothing is cut
  off. [docs/accessibility.md](docs/accessibility.md).
- **Settings**: baby's name, birth date and due date (for corrected age), theme, language
  (English or Português, from the phone's per-app language setting), export and import.

### How the prediction works

Phase 1 of [docs/research.md §7.2](docs/research.md): for each wake window of the day (first after
the morning wake, second, … and the one before bed) it takes the recency-weighted median of the
baby's own windows over the last 14 days and blends it with an age-based prior,
`(n·personal + 5·prior) / (n + 5)`. The prior fades after about a week of logging. Bedtime is the
weighted median of recent bedtimes; when the next nap would land within 45 minutes of it, the app
shows bedtime instead. Since 0.24.0 the last nap counts too (the two-process model's sleep
pressure): after a short nap the next sleep comes sooner, after a long one later, by as much as this
baby's own log shows, and the card says so ("Last nap 35m, shorter than the usual 1h 18m, so 18m
sooner"). Since 0.54.0 the baby's clock counts as well (process C): as the day/night rhythm
forms (6 to 16 weeks), the first nap and bedtime lean towards the times this baby keeps, and the
Day tab draws today's sleep pressure as a line, a model said to be one. The card always says which numbers it used, one tap away ("How it's guessed", since 0.43.0). Tested on a synthetic year
([docs/prediction.md](docs/prediction.md)). Code:
[`domain/Predictor.kt`](app/src/main/java/br/gabriel/cochilo/domain/Predictor.kt).

### Your data

- No account, no cloud backup (Android's auto backup is off), no analytics.
- Two builds ([docs/distribution.md](docs/distribution.md)): the **family build** from GitHub
  releases updates itself, so it may reach GitHub to ask for a newer version and download it, and
  nothing else; the **Play build** has no internet permission at all.
- **Export backup** writes everything (baby + every entry) as JSON. **Import backup** merges a
  file: new entries are added, an entry you edited more recently replaces the older copy, and
  nothing is ever deleted, so importing the same file twice changes nothing.
- **Protect backups** (Settings › Data) sets a passphrase: every backup is then locked
  (AES-256-GCM) and opens only with it, on this phone or another. The spreadsheet and the report
  stay plain. [docs/backup.md](docs/backup.md).
- **Export spreadsheet** writes a CSV for Sheets or Excel (export only).
- **Import from Napper** reads Napper's logs export (the `…-logs-….csv` file): naps, nights (each
  bedtime to the next wake-up, split around night wakings), feeds, bottles, diapers and medicine
  notes. Like a backup import it never deletes, and importing the same file twice adds nothing.
  Napper's measurements file isn't read (Cochilo doesn't track growth).
- **Share to Cochilo** (since 0.41.0): in Napper (or any app, a file manager, an e-mail), Export or
  Share, then pick **Cochilo** ("Import into Cochilo") in Android's share sheet. The file is
  imported straight away, the same as Settings › Import, and a message says how many entries were
  added. "Open with Cochilo" on a CSV or JSON works too. [docs/backup.md](docs/backup.md).

## Build

Needs JDK 17+ and the Android SDK (platform 35).

```sh
./gradlew :app:testGithubDebugUnitTest :app:testPlayDebugUnitTest   # unit tests + screenshot checks
./gradlew :app:recordPaparazziGithubDebug                            # re-record screenshots after a UI change
./gradlew :app:assembleGithubDebug    # app/build/outputs/apk/github/debug/app-github-debug.apk
./gradlew :app:assemblePlayDebug      # the Play build, no internet permission
```

The debug build installs as `br.gabriel.cochilo.debug`, beside a release build.

## Layout

| Path | What |
|---|---|
| `app/src/main/java/br/gabriel/cochilo/data` | Room database: one `events` table, the `baby` row, small prefs |
| `.../domain` | Pure Kotlin: `Predictor`, `Stats`, age `Reference`, sleep maths |
| `.../backup` | JSON backup format, merge, CSV |
| `.../ui/theme` | `CTokens.kt` (generated by `design/build.py`, never edited) and `CochiloTheme` |
| `.../ui/kit` | The design system's components in Compose: frame, header, tab bar, rows, buttons, chips, fields, timeline, actogram, pickers |
| `.../ui/screens` | Today (with the days, `HistoryDays`), Day, Patterns, Ask and its chats, Settings, sheets |
| `app/src/test` | Predictor, stats and backup tests; Paparazzi screenshots of every screen |
| `design/` | How it looks, in three layers: the Gabriel Design System core (vendored), the Tatá Slings brand, the Cochilo layer; `build.py` makes `CTokens.kt` ([design/README.md](design/README.md)) |

Rules for working here: [AGENTS.md](AGENTS.md). Plans for Cochilo as a Tatá Slings product
(monetization, voice, design system, marketing): [docs/tata-slings](docs/tata-slings/README.md).
