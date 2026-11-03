# Where the baby slept, and safe babywearing (0.17.0)

Contact naps (in arms, in the sling) are normal in the first months, and babywearing is the
brand's subject. Cochilo records where each sleep happened and shows the places side by side,
never judged.

## Places

Crib, in arms, sling, pram, car seat (`Kinds.CRIB/ARMS/SLING/PRAM/CAR`, the sleep's kind).

- **Asked when the baby wakes (0.28.0, details after):** a sleep starts with one tap and no
  place. On Woke up, a small sheet asks "Where did Bebê sleep?" with the five places, the one used
  last outlined with a dot; one tap answers, "Not now" leaves it empty. A sleep ended from the
  widget, the notification or the tile is asked about the next time the app opens (within two
  hours). Before 0.28.0 the place was guessed at the start and changed on the sleep panel.
- **In the sheet:** "Where" chips, opened on the last place.
- **In lists:** "Nap · Sling".
- **Patterns:** "Where naps happened, last 7 days", naps per place, most first, with "Naps in arms
  and in the sling are normal in the first months." (`Places.napsByPlace`, `PlacesTest`).

## Safe babywearing: T.I.C.K.S.

The first time "sling" is picked (panel or sheet), a sheet shows the UK Sling Consortium's five
checks once, in plain words: **T**ight, **I**n view at all times, **C**lose enough to kiss,
**K**eep the chin off the chest, **S**upported back (legs in a frog position). It ends with "Cochilo
doesn't watch the baby: you do" (never "the app checks your baby", AAP 2022), **Got it**, and
**Tatá's guides** (her guide page "como usar, posições e cuidados", not the shop; since 0.31.0). Shown once (`Prefs.ticksShown`).
