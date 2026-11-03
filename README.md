# Cochilo

A personal Android app for tracking a baby's sleep: one tap to start and stop a sleep, logs for
feeds, pumping, diapers, daylight, routines and notes, and a prediction of the next nap or bedtime
learned from the baby's own history. Everything stays on the phone; backups are files you export
and import yourself.

**Cochilo, da Tatá Slings**: a [Tatá Slings](https://tataslings.com.br) product. Built on the
[Gabriel Design System](https://github.com/gabrielbr/Gabriel-Deisgn-System) (`cochilo` layer),
dressed in the Tatá Slings brand (teal, plum-grey ink, Pérola, Libre Franklin, the moon-in-a-sling mark, and each kind of entry in one of her fabric colours;
[docs/tata-slings/](docs/tata-slings/README.md)). The research behind it is in
[docs/research.md](docs/research.md). What changed in each release: [CHANGELOG.md](CHANGELOG.md).

| Today | Asleep (dark) | Patterns | Hoje (pt-BR) |
|---|---|---|---|
| ![Today](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_today_light.png) | ![Sleeping](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_today_sleeping_dark.png) | ![Patterns](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_patterns_light.png) | ![Hoje](app/src/test/snapshots/images/br.gabriel.cochilo.ui_ScreensSnapshotTest_today_ptBR_light.png) |

## What it does

- **Today**: awake/asleep timer with one action (Start sleep / Woke up), the next nap or bedtime
  with its window and the reason for it, running timers (a feed, a walk outside), quick log
  buttons, a 24-hour timeline and the day's entries. Tap any entry to edit or delete it.
- **Day**: one day two ways. A 24-hour ring (midnight at the top; sleep on the outer ring, feeds,
  pumping and daylight inside, dots for diapers) with the total sleep in the middle, then every
  entry hour by hour on a vertical rail: sleeps as bars as long as they lasted, and the awake time
  between them. Arrows move between days; tap an entry to edit it.
- **History**: every day with its summary, timeline and entries.
- **Icons**: every kind of entry has its line icon (a drop, a bottle, a spoon, a pump, a diaper,
  the moon, the sun, a bath, a capsule, a pencil), always beside its name (`ui/kit/Icons.kt`).
- **Medicine**: its own entry type and log row; the name and dose go in the note.
- **Log button**: the main action, in the bottom-right corner on every tab. Press, drag up to a
  kind and sideways to a choice (Feed › Right breast), let go: logged. Let go on the kind for its
  sheet. A tap opens it for taps.
- **Filters and the expected window**: History by kind in one tap; today's ring shows when the
  next sleep is expected. [docs/flows.md](docs/flows.md).
- **Report for the doctor**: a one-page PDF of the last two weeks, made on the phone.
  [docs/report.md](docs/report.md).
- **Routine steps**: write the nap and bedtime steps once, tick them each time.
  [docs/rituals.md](docs/rituals.md).
- **What changed**: real changes in the last week, in neutral words; day / night milk.
  [docs/changes.md](docs/changes.md).
- **Ask Cochilo** (Patterns › Questions about sleep): questions answered from the baby's own log
  and the app's research, on the phone, in English and Portuguese; health, medicines and the
  mother's wellbeing go to a person. Not medical advice. Optional since 0.30.0: a small AI model on
  the phone (downloaded by the Play Store, or a file the family imports) for questions beyond the
  list, grounded in the log and checked before it's shown. [docs/assistant.md](docs/assistant.md).
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
- **Sleep first, and motion**: sleep is a size up in every list; every action answers with a small
  movement (off with Remove animations); what was used last has a coloured outline and dot.
  [docs/flows.md](docs/flows.md).
- **Her fabrics**: Settings › Appearance › Fabric wraps the Today card in one of Tatá's slings
  (eleven, photographed from her shop); under the picked one, a quiet link to that sling in the shop
  ([in-app-marketing.md](docs/tata-slings/in-app-marketing.md)). [docs/tata-slings/textures.md](docs/tata-slings/textures.md).
- **Slide to log**: on Today's log rows, slide instead of tapping. Feed: left = left breast, right =
  right breast. Diaper: Wet · Both · Dirty. Slide a running breastfeed to switch side. Tap still
  opens the full sheet. How many taps each entry costs, and why: [docs/flows.md](docs/flows.md).
- **Notifications**: a quiet live notification, asleep or awake, with a running clock, the expected
  wake or next nap, a timeline bar and Woke up / Start sleep; running timers get Finish (and Switch
  side). [docs/notifications.md](docs/notifications.md).
- **"i" buttons**: each kind of entry (on the log rows, the sleep panel and each entry's sheet) has
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
sooner"). The card always says which numbers it used. Tested on a synthetic year
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
| `.../ui/theme` | `CTokens.kt` (generated by the design system, copied as is) and `CochiloTheme` |
| `.../ui/kit` | The design system's components in Compose: frame, header, tab bar, rows, buttons, chips, fields, timeline, actogram, pickers |
| `.../ui/screens` | Today, History, Patterns, Settings, sheets |
| `app/src/test` | Predictor, stats and backup tests; Paparazzi screenshots of every screen |
| `design-system/` | The design-system change (the `cochilo` layer and the Tatá Slings brand), waiting to be merged there |

Rules for working here: [AGENTS.md](AGENTS.md). Plans for Cochilo as a Tatá Slings product
(monetization, voice, design system, marketing): [docs/tata-slings](docs/tata-slings/README.md).
