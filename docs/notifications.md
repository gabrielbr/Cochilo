# Notifications

In the spirit of Napper's: one quiet notification says where the day is without opening the app,
with a timeline bar and the one button that comes next. Built 2026-11-02 (Cochilo 0.4.0).

## What the person sees

**The live notification** (always there once a baby is set up, while the setting is on):

| State | Title | Line | Bar | Button |
|---|---|---|---|---|
| Asleep | Nap / Night sleep | Since 4:10 PM · expected wake 4:47 PM | The sleep, in the nap or night colour, from when it began to the expected wake | Woke up |
| Awake | Awake | Since 2:30 PM · next nap 4:01 PM (or bedtime) | Awake time (muted), then the nap window (teal), with dots for what was logged meanwhile (a feed, a diaper) | Start sleep |

- The header shows the app and a **running clock** (time asleep or awake) that ticks by itself.
- The bar is solid for the time already lived and faded for what's to come; a short tick marks now.
  It moves every 5 minutes, and at once whenever anything is logged.
- With too little history for a guess, the line just says "Since 4:10 PM": nothing it can't back up.

**One notification per running timer** (a feed, pumping, a walk in daylight): "Feed · Left breast",
"Since 2:45 PM" with the running clock, **Finish**, and for a breastfeed **Switch to Right**.

Screenshot (mock-up of the shade with the real bar): `app/src/test/snapshots/images/
br.gabriel.cochilo.ui_NotificationSnapshotTest_notification_light.png` and `…_ptBR_dark.png`.

## The rules

- **Quiet.** Its own channel, "Asleep or awake", low importance: no sound, no vibration, no
  heads-up, no badge. It never nags; it's there to glance at.
- **Asked once.** On Android 13+ the app asks for permission once, when there's a baby. Settings ›
  Notifications › Live notification turns it off (and asks again if it was refused).
- **The buttons do what Today's do.** Woke up, Start sleep, Finish and Switch side run the same code
  as the app (`data/Entries.kt`), then everything updates. Nothing opens unless you tap the body.
- **Expected wake is the baby's own** (`domain/Live.kt`, `expectedWake`): a nap lasts the median
  of the last 14 days' naps; a night ends at the median morning wake (the last night sleep ending
  04:00–11:00 each day). At least 3 examples, or no guess.
- **Nothing leaves the phone.** It reads the phone's own log and posts to the phone; no network.
  The only new permission is `POST_NOTIFICATIONS` (both builds; `check<Variant>Permissions` allows
  exactly it).
- **Not a monitor.** It shows what was logged and a guess; it never says it watches the baby.

## How it's built

| Piece | Where |
|---|---|
| The stretch, expected wake, the bar's pieces (pure, tested) | `domain/Live.kt`, `LiveTest` |
| Building, posting, clearing; the 5-minute tick (inexact alarm, no special permission) | `notify/LiveNotifications.kt` |
| The bar, drawn as a bitmap in the app's timeline colours | `notify/TimelineBar.kt` |
| Title, line and bar layout (Android adds header and buttons: `DecoratedCustomViewStyle`) | `res/layout/notif_live.xml` |
| The buttons and the tick (receiver, not exported; the home-screen widget's buttons use it too, [widget.md](widget.md)) | `notify/NotificationActions.kt` |
| Shared actions (app and notification) | `data/Entries.kt` |
| Refresh on every change to the log, the baby or the setting | `CochiloApp.onCreate` |

**Why a drawn bar and not Android 16's `ProgressStyle`:** the segmented progress style needs
`compileSdk 36`, and the screenshot tool (Paparazzi 1.3.5) can't render against Android 16; moving
to Paparazzi 2 means upgrading the Android Gradle plugin, Kotlin and Gradle together. The drawn bar
shows the same timeline on every Android version (not just 16) in the app's own colours. When the
toolchain moves to API 36, `ProgressStyle` can replace the bitmap and make the notification a
"Live update" (status-bar chip) too.

## Not built yet

- Reminders (e.g. "the nap window opens in 10 minutes"): useful, but it would make sound; to
  decide with Tatá how and when, and as its own setting.
- Live updates / status-bar chip (Android 16): see above.
