# What changed, and day / night milk (0.18.0)

## What changed (research §2.5)

The app doesn't hard-code "sleep regressions". It notices a real change in this baby's own data
and says it plainly. Patterns opens with **What changed**: the last 7 whole days against the 14
before them, per day, for night sleep, nap sleep, the longest stretch, night wakings and naps.

- A change is shown only when it is both large (at least 15%) and big enough to matter (30 min of
  sleep, or about one waking or nap a day), and each window has at least four days logged.
- Each is a short head and its two numbers: "Night sleep is longer · 10h 30m · 9h 00m before".
- Neutral words only (longer / shorter, more / fewer), never better or worse, never a
  prediction; the line under it says "Changes, not problems."
- Today is left out (it is still happening). Rules in `domain/Changes.kt` (`ChangesTest`).

## Day milk and night milk (research §2.4)

Milk expressed at night carries more of the night's hormones. Pumped milk is tagged by when it
was expressed (`expressedAt`, or the pump's start), night being 19:00–07:00 as everywhere in the
app: lists say "Pump · 90 ml · Night milk", and Patterns shows "Pumped, last 7 days: day 420 ml ·
night 180 ml", with "Night milk for night bottles". Rules in `domain/Milk.kt` (`MilkTest`).
