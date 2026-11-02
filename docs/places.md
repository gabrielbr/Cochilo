# Where the baby slept, and safe babywearing (0.17.0)

Contact naps (in arms, in the sling) are normal in the first months, and babywearing is the
brand's subject. Cochilo records where each sleep happened and shows the places side by side,
never judged.

## Places

Crib, in arms, sling, pram, car seat (`Kinds.CRIB/ARMS/SLING/PRAM/CAR`, the sleep's kind).

- **Remembered:** every new sleep (Today, the Log button, the notification, the widget, the tile)
  starts in the place used last (`Entries.startSleep`, `Places.last`).
- **One tap to change:** while the baby sleeps, the sleep panel shows "Where" with the five places
  as chips (outlines on the night panel, the current one a blush).
- **In the sheet:** "Where" chips, opened on the last place.
- **In lists:** "Nap · Sling".
- **Patterns:** "Where naps happened, last 7 days", naps per place, most first, with "Naps in arms
  and in the sling are normal in the first months." (`Places.napsByPlace`, `PlacesTest`).

## Safe babywearing: T.I.C.K.S.

The first time "sling" is picked (panel or sheet), a sheet shows the UK Sling Consortium's five
checks once, in plain words: **T**ight, **I**n view at all times, **C**lose enough to kiss,
**K**eep the chin off the chest, **S**upported back (legs in a frog position). It ends with "Cochilo
doesn't watch the baby: you do" (never "the app checks your baby", AAP 2022), **Got it**, and
**Tatá's guides** (her site). Shown once (`Prefs.ticksShown`).
