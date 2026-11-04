# The prediction

What the Today card's "Next nap / Bedtime" does, and how it's tested. Science:
[research.md §2.1 and §7.2](research.md).

## Phase 1: the baby's own wake windows (0.1.0)

For each window of the day (the first after the morning wake, the second, … and the last before
bed), the recency-weighted median of the baby's windows over 14 days (half-life 3 days), blended
with the age prior: `(n·personal + 5·prior) / (n + 5)`. Bedtime is the weighted median of recent
bedtimes; when the next nap would land within 45 minutes of it, the card shows bedtime instead,
within 30 minutes of the usual bedtime.

## Phase 2: the last nap counts (0.24.0)

The two-process model's process S: sleep pressure builds while awake and drains while asleep, so
a short nap leaves more pressure and the next window is shorter; a long nap, longer.

- **Learned, not assumed.** From the last 14 days, every nap-to-nap pair (the nap's length, the
  window after it) is compared *within its slot of the day*, and a recency-weighted least-squares
  slope says how many minutes of window one more minute of nap buys for this baby.
- **Bedtime windows are left out of the fit**: the last window ends when the clock says (process C),
  so a long last nap shortens it, which isn't the pressure effect. The shift still applies to a
  bedtime prediction.
- **Careful by construction.** At least 8 pairs (about two days); naps must vary (at least 10
  minutes); the slope is held between 0 and 1; it's shrunk by `n / (n + 5)` like the windows;
  the shift is capped at 20% of the usual window; under 5 minutes it isn't applied or shown.
- **It explains itself.** Behind "How it's guessed" (one tap, since 0.43.0), the card adds "Last nap 35m, shorter than the usual 1h 18m, so 18m
  sooner" (or "longer…, so 10m later"), so the numbers on the card still add up.
- **A late nap still leaves time awake.** Bedtime is never sooner than half the usual last window
  after waking (found by the synthetic year, below).

`Predictor(napEffect = false)` turns phase 2 off; it exists only so the tests can compare.

## The rest of the day (0.49.0)

The card used to say only the next sleep. Now Cochilo projects every sleep left today
(`domain/DayPlan.kt`, tested in `DayPlanTest`), the way the day would go:
1. Predict the next sleep (the card's own guess, unchanged).
2. Suppose a nap of the baby's usual length for that place in the day happens then. The usual
   length is the median of the last 14 days' naps in the same slot, else of all naps, else 45
   minutes before about 6 months and 75 after.
3. Predict again from its end. The slot moves on, so the windows lengthen as the day does.
4. Stop at bedtime: when the predictor says so, or once the age's usual number of naps is done
   (`Predictor.predict(maxNaps)`), or at midnight.

**Ranges widen.** Each step adds half of its own range to the next, up to 45 minutes each side,
so the afternoon's guess says it's less sure than the next nap's. Every logged sleep or wake
replaces the projection with what happened and recomputes the rest: a short nap pulls the rest
of the day earlier.

**Where it shows:**
- **Today's card:** "Then: 13:50 · 16:05 · bed 18:45" under the window. "How it's guessed" adds
  how the later ones come about, and says when they come from the age alone.
- **The Day ring, today:** each projected sleep as a dashed outline on the sleep ring, never
  filled, so it never reads as data. Bedtime is in the night colour until midnight. Under the
  counts: "Expected today: …", which TalkBack reads with the ring.

- **The Day timeline, today** (0.50.0): the next expected sleep as one block on the main track,
  above what's happening now. It's a dashed outline in the nap colour (the night's for bedtime),
  sized by its expected length; bedtime gets a short bar. "~11:50" marks the time. It says
  "Expected nap" (or "Expected bedtime") in muted, regular weight, then the window and "about
  45m". It has no badge and no pencil, and it can't be tapped: it isn't data. Once its window
  passes with nothing logged, it says "Past its window", never in red. While a nap runs, it
  shows the sleep after that nap. It shows only with the "All" filter. Only the next sleep is
  drawn: the rest are a line away, on Today's card and under the ring.

**Not shown** at night: asleep for the night, or awake in the night.

## Tested on a synthetic year

`PredictorYearTest` makes a baby from 8 weeks to about a year: windows that grow with age (74
minutes at 8 weeks, about 3½ hours at a year), naps from 30 to 110 minutes, noise on every window,
bedtime from 19:00, and predicts every nap end after the first two weeks (about 1,060
predictions):

| Baby | Median error | 90th percentile | Mean |
|---|---|---|---|
| Windows follow the last nap (0.5 min per min), phase 2 on | 10 min | 27 min | 14 min |
| The same baby, phase 2 off | 14 min | 34 min | 17 min |
| Windows ignore the last nap, phase 2 on | 8 min | | 12.0 min |
| The same baby, phase 2 off | 8 min | | 12.0 min |

The tests assert that it is within minutes (median ≤ 15, 90th ≤ 40), that phase 2 helps a baby
who has the effect, that it does no harm to one who hasn't, and that every prediction lands
between 20 minutes and 6 hours after waking. `PredictorTest` keeps the hand-made cases, and five
more cover the nap effect's own rules.

## Next: the clock (process C)

A circadian curve fitted to the baby's own bedtimes and morning wakes as the rhythm forms
(months 2 to 4), so the first nap and bedtime lean towards it, and a live sleep-pressure line on
the Day tab. In the roadmap.
