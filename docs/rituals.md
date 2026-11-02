# The ritual builder (0.19.0)

Preventive parent education has the strongest evidence for good sleep habits (research §4), and
the same steps in the same order before sleep are what parents get from paid consultations
(Tatá Slings monetization §6.4). Cochilo lets the parent write the nap and bedtime steps once and
tick them in the routine.

- **Write once:** Settings › Baby › **Routine steps**: bedtime or nap, then the steps (up to 10,
  40 characters each). Add with the field, remove with the row's Remove; Save.
- **Defaults** in the app's language until they're edited: bedtime "Bath, Diaper and pyjamas, Dim
  the lights, Feed, A song in arms"; nap "Diaper, Room dark and quiet, A song in arms".
- **Tick as you go:** opening a new routine (tap the Routine row or the Log menu) shows the steps
  as a checklist; each tick is one tap; Save logs the routine with the ticked steps as its note
  ("Bath · Feed · A song in arms"). Sliding the Routine row still logs at once, without the list.
- **Celebrations:** a bedtime routine before the night's sleep is one of them
  ([celebrations.md](celebrations.md)).

Code: `domain/Ritual.kt` (`RitualTest`), `ui/screens/Rituals.kt`, `ui/kit/Kit.kt` `Checklist`,
the routine part of `EventSheet`. Stored in `Prefs` (not in the backup: it's a setting).
