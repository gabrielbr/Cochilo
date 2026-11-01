# Working in this repo (for agents)

Cochilo is a personal baby-sleep app for the owner's family. Read [README.md](README.md) first,
then [docs/research.md](docs/research.md) for the science the features rest on.

## The design system decides how it looks

The UI follows the [Gabriel Design System](https://github.com/gabrielbr/Gabriel-Deisgn-System),
layer `cochilo`. Its AGENTS.md and DESIGN.md are the rules; the short version:

- **Tokens only.** Every colour, size, radius, duration and type role comes from
  `ui/theme/CTokens.kt` (through `CochiloTheme.colors`, `CTokens.Spacing/Radius/Size`, `Type`).
  No literal dp, sp, ms or colour in a screen. If something is missing, add it to the design
  system first, then copy the regenerated `CTokens.kt` here.
- **Never edit `CTokens.kt` by hand.** It is `generated/cochilo/CTokens.kt` from the design system,
  copied as is; `python3 scripts/drift.py --cochilo ../Cochilo` there must report 0 differences.
- **One mint action per screen** (Start sleep / Woke up on Today; Save in a sheet). Everything else
  is a secondary outline, a row or a chip. Text on mint is `onMint`.
- **Event colours are data colours.** Use them only on data, always next to a word.
- **Components live in `ui/kit`.** Screens compose them; they don't draw their own buttons or rows.
- **Copy in English and Brazilian Portuguese.** Every string in `values/strings.xml` and
  `values-pt-rBR/strings.xml`; short, plain, sentence case, verb first on buttons; no plurals that
  break with 1 (write "Naps: 1").
- 48dp targets, TalkBack labels, a spoken summary for every chart, Remove animations honoured.

## Product rules

- **Nothing leaves the phone.** No INTERNET permission, no analytics, no cloud backup. Data moves
  only through Export/Import.
- **Import never deletes** and keeps the newest edit of an entry (`Backup.merge`). Changing the
  backup format means bumping `BackupFile.VERSION` and still reading older files.
- **Not a medical or safety device.** Never imply the app monitors the baby; safe-sleep text
  follows AAP 2022.
- **Predictions explain themselves.** The card shows the numbers it used. Keep `Predictor` pure
  Kotlin with unit tests for every rule.

## Before you push

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

After a UI change, re-record the screenshots (`./gradlew :app:recordPaparazziDebug`), look at
every image, and commit them with the change.
