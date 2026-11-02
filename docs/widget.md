# Home-screen widget

Cochilo 0.11.0 adds a 4 × 2 widget, "Sleep and feed" ("Sono e mamada"): the two entries logged
most often, one tap each from the home screen, without opening the app.

```
[mark] Awake since 14:20                 40:00
[ ☾  Start sleep      ] [ ● Left ] [ Right ]
```

- **Status line:** asleep or awake, since when, and a clock that ticks by itself (a Chronometer;
  the widget is never redrawn on a timer). Tap it to open the app.
- **Start sleep / Woke up:** the one mint action, wider than the rest.
- **Left / Right:** start a breastfeed on that side (a running timer, as on Today).
  - The breast used last has the **Last mark**: an outline and a dot in the feed colour
    (remember actions). TalkBack says "used last".
  - While a breastfeed runs, its side is lit (soft feed colour, outlined) and reads **Finish**: one
    tap ends it. The other side **switches** to it (that side ends, this one starts): one tap.
- **No baby yet:** "Open Cochilo to add the baby first", and a tap opens the app.

## How it works

- `widget/CochiloWidget.kt` (an `AppWidgetProvider`) fills `res/layout/widget_cochilo.xml` from
  `domain/WidgetState.kt` (pure, `WidgetStateTest`). `bind` is written against `WidgetTarget`, so
  the screenshot test fills the very layout the home screen shows (`WidgetSnapshotTest`).
- The buttons are broadcasts to `notify/NotificationActions` (not exported), the same receiver as
  the notification's buttons, which calls `data/Entries` (`startSleep`, `wakeUp`,
  `startBreastfeed`, `switchSide`, `finish`): exactly what the app's buttons do.
- It updates whenever the log changes (`CochiloApp` observes the database, like the live
  notification), and after each of its own taps.
- Colours: a RemoteViews layout can't read `CTokens`, so `widget_*` colours are copied into
  `values/colors.xml` and `values-night/colors.xml`; `WidgetSnapshotTest.colorsMatchTheTokens`
  fails if they drift. Sizes are the design system's (dimens.xml); the family is Libre Franklin.
- Privacy: it reads only the phone's own log. No permission is added to either build.

Design system: the Home widget component (0.10.0, patch `design-system/0005-…`).
