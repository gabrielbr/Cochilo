# Little celebrations (0.14.0)

When what the parent was trying for happens, Cochilo answers with one small moment: the baby
avatar rises into a soft blush card under the header, lifts its arms and smiles, beside one warm
line; then it settles away by itself (about 2.6 s), or at a tap. It never blocks a tap and never
waits for one. Settings › Notifications › **Little celebrations** turns it off (on by default).

## When

The rules are pure (`domain/Celebrations.kt`, `CelebrationsTest`), from data the app already has:

| Moment | When | Words (EN / PT) |
|---|---|---|
| A full nap | Woke up at or after the expected nap end (`expectedWake`, five minutes' grace), nap ≥ 30 min | "A full nap. Well done, both of you." / "Uma soneca inteira. Muito bem, vocês dois." |
| On time | Start sleep inside the predicted window (with history behind it) | "Right on time for Dante." / "Na hora certinha para Dante." |
| Longest night | A night sleep longer than any of the last seven nights (needs three nights to compare) | "Dante's longest night this week." / "A noite mais longa de Dante na semana." |
| Daylight | The walk that takes today's 12:00–16:00 daylight past 30 min (research §2.3) | "Daylight done for today." / "Luz do dia: feito por hoje." |
| Routine before bed | A night sleep started within 90 min after a bedtime routine | "Routine done. Good night." / "Rotina feita. Boa noite." |
| A week logged | Something logged on each of the last seven days; at most once a week | "A whole week logged. The suggestions know Dante better now." / "Uma semana inteira registrada. As sugestões conhecem Dante melhor agora." |

## So it stays kind

- **Celebrate, never scold.** No "missed", no streak that breaks, no red; a day without one is
  just a day.
- **Never a promise.** The words describe what happened, never what the baby will do.
- **Rare enough to mean something:** each moment at most once a day (the week once a week), and
  at most three a day in all (`Prefs.celebrationsShown`).
- **Quiet at night** (19:00–07:00): the words only, dim, no baby and no movement, gone sooner.
  Light at night is what the research says to avoid.
- **Remove animations:** a still picture and the words.
- **TalkBack** reads the words once (a polite live region).

## Code

`ui/kit/CelebrationCard.kt` (the card, its one motion from `Motion.kt`), the triggers in
`MainViewModel` (`startSleep`, `wakeUp`, `finish`, `quickLog`, `save`), shown by `CochiloRoot`.
The avatar is the one chosen at onboarding ([onboarding.md](onboarding.md)).
