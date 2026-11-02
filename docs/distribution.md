# Two builds: github and play

Cochilo ships as two builds from the same code (`productFlavors` in `app/build.gradle.kts`):

| | **github** (the family build) | **play** (Google Play, later) |
|---|---|---|
| Where it comes from | GitHub releases: `cochilo-<version>.apk` | Google Play: `cochilo-play-<version>.aab` |
| How it updates | Itself, the design system's [self-update](https://github.com/gabrielbr/Gabriel-Deisgn-System/blob/main/docs/self-update.md) | Google Play (Play forbids apps on it from updating themselves) |
| Permissions | `INTERNET`, `REQUEST_INSTALL_PACKAGES`, nothing else | None |
| What goes over the internet | One unauthenticated request to GitHub's public releases API, and the new APK | Nothing |
| Code | `app/src/github` | `app/src/play` (no-ops) |

In both, **the baby's data never leaves the phone**: no account, no analytics, no cloud backup.
The permission lists are enforced on every build by `check<Variant>Permissions` (run by
`assemble*` and `test*`); a library that adds a permission fails the build.

## Self-update in the github build

As the design system specifies (`docs/self-update.md` there):

- **When:** on every return to the foreground, at most every 15 minutes, with `If-None-Match`.
  Settings › About › Check for updates checks at once.
- **What counts:** the latest full release (not a draft or pre-release) whose `v<x.y.z>` is newer
  than the installed version and which has `cochilo-<x.y.z>.apk` and `SHA256SUMS.txt`.
- **Before installing, all required:** HTTPS all the way; the whole file arrived; its SHA-256 matches
  `SHA256SUMS.txt`; it is `br.gabriel.cochilo` signed with the installed app's key. Then Android
  asks "Do you want to update this app?".
- **Later** hides that version until the app is next started from cold. While downloading, Later
  becomes Cancel, and the sheet can't be swiped away.
- Debug builds (`.debug`, another key) can't update themselves and say so in About.
- The sheet shows the first paragraph of the release text.

Rules are pure Kotlin in `update/UpdateRules.kt` (tested in both builds); the network, checks and
install are `src/github/.../update/Updater.kt`; the sheet and the About row are `ui/kit/Update.kt`.

## Releasing

Run the **Release** workflow with a version (e.g. `0.2.0`) and, optionally, a sentence of what's new.
It tests both builds and publishes a full release with `cochilo-<v>.apk`, `cochilo-play-<v>.aab`
and `SHA256SUMS.txt`. Installed family builds offer it the next time they're opened.

Phones on 0.1.x (before self-update) need one manual install of 0.2.0; after that, updates come
by themselves.

## Before Google Play

- A real upload key in the `RELEASE_*` secrets (today both builds use the test key in the repo).
- Decide the Play package name: keeping `br.gabriel.cochilo` means a phone with the family build
  can't install the Play one over it without uninstalling (different signing keys).
- Play's data safety form: "No data collected".
