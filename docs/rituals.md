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

## Routine music (0.58.0)

The owner asked for a place to keep the songs or playlist the family uses to put the baby to
sleep, with a player for them.

- **Save once:** Settings › Baby › **Routine music**. Paste a link from Spotify, YouTube Music or
  YouTube (a song, a playlist, an album), and a name if you like. Up to 5 links. A pasted share
  text works ("Listen on Spotify: https://…"); tracking parameters (`si=`, `utm_…`) are dropped.
  Anything else gets "That isn't a Spotify, YouTube Music or YouTube link." and keeps the text.
- **Play:** a new routine's sheet lists the music under its steps, one button each ("Play
  Lullabies"). The tap opens the link in its own app (Spotify, YouTube Music or YouTube), or in
  whatever opens links when that app isn't installed. Each row in Settings › Routine music plays
  too.
- **Why it opens the music app instead of a player inside Cochilo** (researched 2026-11-06):
  - [Spotify's embedded player plays only 30-second previews inside an Android app](https://community.spotify.com/t5/Spotify-for-Developers/Only-30s-preview-on-Android-WebView-embeds/td-p/5386262),
    even for logged-in Premium users.
  - YouTube Music has no official embed.
  - A player inside the app stops when the screen goes off, which is exactly when a bedtime
    routine needs it.
  - The play build has no INTERNET permission at all (AGENTS.md). Opening another app needs none.
    The music app plays the whole playlist in the background, and Cochilo sends nothing.
  - [Spotify's own Android guidance](https://developer.spotify.com/documentation/android/tutorials/content-linking)
    is the same: an `ACTION_VIEW` intent on the link, to the Spotify app when it's there.

**Share it from the music app (0.63.0).** In Spotify or YouTube Music, Share › **Add to Cochilo**:
- Cochilo opens on what it was showing.
- It adds the link at the end of Routine music and says "Added to routine music", with Undo.
- It names the link from the share's subject when that's a short title.
- The same link shared again ("Already in routine music"), even with a new tracking tag, isn't
  added twice.
- With five saved, it says the list is full and where to remove one.
- Shared text that isn't a music link goes to the import as before. The share sheet's other entry,
  "Import into Cochilo", now takes only files. (`Music.add`, `Music.nameFromSubject`, tested;
  `MainViewModel.addSharedMusic`.)

**One tap away (0.60.0).** The first saved link is also played from:
- **Today's card:** "Play sleep music", a secondary pill under Start sleep, and under the night
  waking's two buttons (to settle back). Not while asleep: the notification has it then.
- **The live notification:** "Play music" while the baby sleeps, in a night waking, and in the 30
  minutes before the next sleep window opens.
- **The nap and bedtime reminder:** "Play music" beside Start routine.

A notification button opens the music app directly (`PendingIntent.getActivity`). That's
allowed from a notification even with Cochilo closed, where a background start isn't. The
manifest lists the three music apps in `<queries>`, so Cochilo can see whether each is
installed. That isn't a permission.

Code: `domain/Music.kt` (`MusicTest`), `music/MusicOpener.kt`, `ui/screens/RoutineMusic.kt` (the
sheet, `RoutineMusicButtons`). Stored in `Prefs`, like the steps (a setting, so not in the backup).
