# Working in this repo (for agents)

Cochilo is a personal baby-sleep app for the owner's family. Read [README.md](README.md) first,
then [docs/research.md](docs/research.md) for the science the features rest on.

## Standing rules from the owner

- **Document everything in this repo's docs.** Every change updates what it touches: `README.md`
  (what the app does), `CHANGELOG.md` (every release, in plain words), the matching file in
  `docs/` (or a new one), and this file when a rule changes. A PR that changes behaviour without
  its docs isn't done.
- **Always merge and release.** Once a PR's CI is green and it has no open review threads, merge it
  (squash) and run the Release workflow with the next version and a one-line note in English and
  Portuguese. Never leave a finished PR waiting.
- **Fewest taps.** Every action costs the fewest taps it can: the common entries are one move
  (the Log button's press-and-drag, the home-screen widget, the quick-settings tile, the Earlier chips, one-tap buttons, notification buttons); details stay one tap away, never in the way;
  undo instead of "are you sure?" where it's safe. Every change that adds or changes a flow updates
  the tap audit in [docs/flows.md](docs/flows.md) and must not make a common entry cost more.
- **Remember actions.** What was done last is shown and offered first: the last breast is marked
  (so she can decide whether to continue on that side), the sheet opens on the last choice (diaper
  kind, daylight, routine), the last bottle and pump amounts and the last medicine are filled in, and
  the sheet says "Last time: … · 14:20". New entry types and kinds follow it, and the audit in
  docs/flows.md lists what each type remembers.
- **Keep the plan.** Ideas, the owner's suggestions and research-backed features go into
  [docs/roadmap.md](docs/roadmap.md); when one ships, move it out of the plan and into the docs.
- **Never guess a repo or project.** Work in Cochilo. If a request seems to point at another repo
  (PulseLoop, the design system…), ask before opening it.

## The design decides how it looks: three layers ([design/README.md](design/README.md))

Cochilo is a **Tatá Slings** product built on the **Gabriel Design System**, the owner's own
boilerplate, which is in Gabriel's brand. Since 0.40.1 the design is in three layers:
- **core**: the Gabriel Design System, vendored at a pinned version in `design/core/`;
- **brand**: Tatá Slings, in `design/brand/tata-slings/`: the colour values (teal accent, plum-grey
  ink, Pérola page, Amora Log button), the one family (Libre Franklin), the mark and her textures;
- **product**: Cochilo's own tokens and components, in `design/product/cochilo/`.

Every standard not set by the brand or the product is the core's. Its AGENTS.md and DESIGN.md are
the rules; the short version:

- **Where a design change goes.**
  - Could any of the owner's apps use it? It's **core**: change it in the design system (ask
    before opening that repo), release it, re-sync `design/core/` (`design/core/CORE.md`).
  - Is it about Tatá's look? It's **brand**: `design/brand/tata-slings/brand.json`.
  - Only Cochilo's? It's **product**: `design/product/cochilo/layer.json` and `components.md`.
  - Never edit `design/core/` here. What still has to go upstream is in `design/UPSTREAM.md`.
- **Tokens only.** Every colour, size, radius, duration and type role comes from
  `ui/theme/CTokens.kt` (through `CochiloTheme.colors`, `CTokens.Spacing/Radius/Size`, `Type`).
  No literal dp, sp, ms or colour in a screen. If something is missing, add it to the right layer
  and run `python3 design/build.py`.
- **Never edit `CTokens.kt` by hand.** `python3 design/build.py` writes it from the three layers
  with the core's own generator; `python3 design/build.py --check` (CI runs it) fails if it
  differs, or if a check fails (scale, type floor, brand limits, contrast in all three themes).
- **Teal is the one action, nothing else.** No highlighter behind titles; the active tab and a
  picked chip use `selected` (a blush), the screen's edge uses `frame-line` (a soft hem). Captions
  are sentence case; buttons are pills. Why: [docs/design-review.md](docs/design-review.md).
- **One mint action per screen** (Start sleep / Woke up on Today; Save in a sheet). Under the brand
  the `mint` role is Tatá teal; it's a fill, never text. Everything else is a secondary outline, a
  row or a chip. Text on it is `onMint`.
- **Event colours are data colours.** Use them only on data, always next to a word.
- **No text on a texture.** Her fabrics (`ui/kit/Fabric.kt`) show only as a Fabric frame's band
  or a picker swatch ([docs/tata-slings/textures.md](docs/tata-slings/textures.md)).
- **Brand voice.** Words follow [docs/tata-slings/brand-voice.md](docs/tata-slings/brand-voice.md):
  quiet and short in the app, warm in marketing, never a promise about how the baby will sleep.
- **Every kind of entry has an icon** (`ui/kit/Icons.kt`, the design system's line set), shown
  beside its name, never instead of it. A new entry type gets its icon in the design system first.
- **Components live in `ui/kit`.** Screens compose them; they don't draw their own buttons or rows.
- **Copy in English and Brazilian Portuguese.** Every string in `values/strings.xml` and
  `values-pt-rBR/strings.xml`; short, plain, sentence case, verb first on buttons; no plurals that
  break with 1 (write "Naps: 1").
- **The baby avatar** (`ui/kit/BabyAvatar.kt`) uses only the `illustration-*` colours, the same in
  both themes; it asks how the baby looks, never race; gender changes details only, never colour.
- **Audit every release.** Each feature ends with an impeccable critique and the
  graphic-design-fundamentals checklist in `docs/audits/<version>-<topic>.md`; fix what fails first.
- **Three themes.** Light, dark and the night light (`CTokens.Night`, 19:00–07:00); every new
  colour role needs a night value or falls back to dark, and passes contrast in all three.
- **Celebrate, never scold.** Celebrations (`domain/Celebrations.kt`) describe what happened,
  never promise, never shame a miss; at most three a day; words only at night.
- **Sleep leads.** Sleep is the app's subject: wherever kinds are listed it is a size up
  (`kind-badge-lead`, label role); `EventType.leads` says which.
- **Last mark, not a word.** What was used last is an outline and a dot in the kind's colour
  (`LastDot`, `LastLine`); "last" is only spoken.
- **Motion on every action**, from `ui/kit/Motion.kt` only (two durations, system easing); every
  new interaction gets one, and all of it snaps with Remove animations.
- 48dp targets, TalkBack labels, a spoken summary for every chart, Remove animations honoured.

## Product rules

- **The baby's data never leaves the phone.** No analytics, no account, no cloud backup. Data moves
  only through Export/Import. Two builds ([docs/distribution.md](docs/distribution.md)): **play** has
  no INTERNET permission at all (since 0.30.0 it may run Play Asset Delivery's extraction for Ask
  Cochilo's model, whose permissions are listed in docs/distribution.md); **github** (the family
  build) has INTERNET only for the design system's self-update and, since 0.37.0, the one-tap
  download of Ask Cochilo's model from this repository's releases (no data in either request). Both may post notifications (`POST_NOTIFICATIONS`, the live notification,
  [docs/notifications.md](docs/notifications.md)). `check<Variant>Permissions` fails the build if
  either list grows.
- **Import never deletes** and keeps the newest edit of an entry (`Backup.merge`). Changing the
  backup format means bumping `BackupFile.VERSION` and still reading older files. A locked backup
  (`SealedFile`, [docs/backup.md](docs/backup.md)) wraps the same file and has its own `VERSION`;
  changing the lock means bumping that one.
- **Not a medical or safety device.** Never imply the app monitors the baby; safe-sleep text
  follows AAP 2022.
- **Predictions explain themselves.** The card shows the numbers it used. Keep `Predictor` pure
  Kotlin with unit tests for every rule.

## Before you push

```sh
python3 design/build.py --check && python3 -m unittest discover -s design -p "test_*.py"
./gradlew :app:testGithubDebugUnitTest :app:testPlayDebugUnitTest \
  :app:assembleGithubDebug :app:assemblePlayDebug :app:lintGithubDebug :app:lintPlayDebug
```

After a UI change, re-record the screenshots (`./gradlew :app:recordPaparazziGithubDebug`,
then `:app:verifyPaparazziPlayDebug` must pass too), look at
every image, and commit them with the change.
