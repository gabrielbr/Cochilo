# Notifications

In the spirit of Napper's: one quiet notification says where the day is without opening the app,
with a timeline bar and the one button that comes next. Built 2026-11-02 (Cochilo 0.4.0).

## What the person sees

**The live notification** (always there once a baby is set up, while the setting is on):

| State | Title | Line | Bar | Button |
|---|---|---|---|---|
| Asleep | Nap / Night sleep | Since 4:10 PM · expected wake 4:47 PM | The sleep, in the nap or night colour, from when it began to the expected wake | Woke up |
| Awake | Awake | Since 2:30 PM · next nap 4:01 PM (or bedtime) | Awake time (muted), then the nap window (teal), with dots for what was logged meanwhile (a feed, a diaper) | Start sleep |
| Awake in the night (0.42.0) | Awake in the night | Since 3:10 AM | the same bar | **Back to sleep** and **Up for the day** ([nights.md](nights.md)) |

- **At night (19:00–07:00) it's quieter** (0.51.0): the title, the running clock and the buttons
  only. No line and no bar, so there's less to read in the dark.
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

### Play music (0.60.0)

When a routine music link is saved ([rituals.md](rituals.md)), the live notification adds
**Play music** while the baby sleeps, in a night waking (beside Back to sleep and Up for the day),
and while awake in the 30 minutes before the next window opens. The window reminder adds it beside
Start routine. The button opens the first link in its music app. The notification follows the
saved links: adding or removing one refreshes it.

### Kept up to date (0.57.1)

The owner's wife's phone, paired and keeping sync on with Cochilo closed, showed an old
"Asleep since…" that never went away, while Today on the same phone was right. Two causes, both
fixed:
- **One loop updates the notification, the widget, the tile and the watch** (`CochiloApp`). It
  lives as long as the process, which is days when sync runs with Cochilo closed. A failure in any
  step used to end it for good, so the notification froze. Each step now runs on its own (a
  failure is logged and the next update still runs), and the watch push gives up after 15 s.
- **Refreshes now take turns** (`LiveNotifications.refresh`, a mutex). A sync, the 5-minute tick,
  the tile and the buttons can all ask at once; one that read the log just before a sync landed
  could post last and put back a sleep that had ended. Each now reads the log fresh, in turn.
- **Opening Cochilo refreshes it** (`MainActivity.onStart`), so it is never wrong for longer than
  it takes to open the app.

## Window reminders (0.33.0)

The owner's request: a heads-up **30 and 15 minutes before each sleep window**, to bring
attention to the window in time to start the preparation (the routine).

**What the person sees:** "Nap window in 30 min" (or "Bedtime window in 15 min"), "Expected about
3:51–4:11 PM. A good moment to start the routine.", and one button, **Start routine**, which logs a
nap or bedtime routine at once (one tap) and takes the reminder away. A tap on the body opens the
app. The reminder goes away by itself when the window closes, or as soon as a sleep starts.

**Settings › Notifications › Window reminders:** on by default; under it, one tap picks when: **30
and 15 min** (default), **30 min** or **15 min**. Turning it on asks for the notification
permission if it wasn't given.

**The rules** (`domain/Reminders.kt`, tested in `RemindersTest`):
- Only while the baby is awake and the app has a guess (the prediction's window, `windowStart`).
- Each lead fires once per window; a window that moves by less than 20 minutes (a newer guess)
  is the same window and isn't announced again.
- **Late is skipped, not sent:** a reminder more than 10 minutes late (the phone was asleep, or the
  window moved earlier) is dropped. An alarm firing up to 2 minutes early still counts.
- **Quiet night:** none from 21:00 to 06:00. A bedtime at 20:30 still gets its 20:00 and 20:15.
- When the alarm fires, the prediction is read again; the reminder is sent only if it's still due.

**How it's built:** `notify/SleepReminders.kt`. Its own channel, **"Window reminders"**, at default
importance: it can make the phone's notification sound, and the person can silence it or turn it off
in Android's settings without touching the quiet live notification. The alarm is
`setAndAllowWhileIdle` (wakes the phone even in Doze, give or take a few minutes): **no new
permission** (no exact-alarm permission). It's set again after every change to the log, the
baby or the setting, and after each alarm. Start routine is `Entries.logRoutine`, through the same
receiver as the other buttons.

**Known limit:** after the phone restarts, reminders resume once the app, the widget or the tile
next updates (a boot receiver would need a permission the family build doesn't have).

## Not built yet

- Live updates / status-bar chip (Android 16): see above.
