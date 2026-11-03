# Changelog

Every release of Cochilo, newest first. Releases are on
[GitHub](https://github.com/gabrielbr/Cochilo/releases); the family build updates itself from them.

## 0.43.0 — 2026-11-03

**Less on the screen.** A pass through every screen asking what earns its place
(`.claude/skills/less-is-more/SKILL.md`). Today loses its "Today" title and the header's
"Awake · 40m" (the card already says it), and the sleep card's "i". How the next nap is guessed is
now one tap away ("How it's guessed") instead of always open. The day ring's caption is gone, and
the "hold and drag" hint shows only until you've used it once. During a night waking, Today shows
only the night and its two buttons: no daylight nudge. Patterns drops "Questions about sleep" (the
chat icon at the top does the same), puts the night first (night sleep, longest stretch, night
wakings and the usual bedtime, new), then the day, with feeds and diapers in one row. No tap was
added anywhere. For whoever works on the app: every screen and flow is now designed and
audited with three skills: impeccable, graphic-design-fundamentals and less-is-more (AGENTS.md).
`docs/flows.md`.

## 0.42.0 — 2026-11-03

**The night is one thing.** Each sleep is now a nap or the night. Start sleep is still one tap:
Cochilo guesses from the baby's own bedtime and the hour, and one tap fixes it ("It's a nap" / "It's
the night"). When the baby wakes in the night, Today says "Awake in the night", with **Back to
sleep** and **Up for the day**. The notification has both too, and the widget offers Back to sleep.
So an evening catnap is no longer counted as bedtime, an early first nap no longer counts as night,
and bedtime, night wakings and the next-sleep guess follow the real night. Earlier entries keep the
old clock rule, and Napper imports bring their real nights. `docs/nights.md`.

## 0.41.1 — 2026-11-03

**No change you can see.** The Gabriel Design System released 0.28.0. It took in the general
parts of Cochilo's old design patches and dropped everything that is Cochilo's or Tatá's. Cochilo
now builds from that real release, and the old patches are gone. Every value in the app is the
same. `design/core/CORE.md`.

## 0.41.0 — 2026-11-03

**Share to Cochilo.** Cochilo now appears in Android's share sheet. In Napper, tap Export and pick
Cochilo ("Import into Cochilo"): the logs are imported straight away, the same as Settings ›
Import, and nothing is ever deleted. It also works for a Cochilo backup shared from e-mail or
a chat, and for "Open with" on a CSV or JSON file. `docs/backup.md`.

## 0.40.1 — 2026-11-03

**No change you can see.** Behind the scenes, Cochilo's look now lives in this repo in three
layers:
- the Gabriel Design System's core, kept at a fixed version;
- the Tatá Slings brand;
- Cochilo's own pieces.

The design system stays the owner's generic boilerplate. Every value in the app is exactly the
same. `design/README.md`.

## 0.40.0 — 2026-11-03

**History is on Today, and Ask keeps chats.** The History tab is gone. Today now ends with Day by
day: the same filters, then every day, today first, with its summary, timeline and entries. Three
tabs are left: Today, Day and Patterns. In Ask you can now have several chats, one per topic.
Each new visit starts a fresh one, Chats lists the saved ones to go back to, and a chat is deleted
with Undo. Chats stay on this phone and aren't in backups. `docs/flows.md`, `docs/assistant.md`.

## 0.39.1 — 2026-11-03

**Asking questions no longer closes the app.** With the on-device AI on, a question could make the
app quit: since 0.38.0 the AI was given more of the log than it can read at once. Now what it's
given always fits, it answers one question at a time, and if it ever stops the app again it pauses
itself (with "Try the on-device AI again") instead of crashing every time. `docs/assistant.md`.

## 0.39.0 — 2026-11-03

**Today is simpler.** The list of log rows is gone from Today: the Log button (the round + in the
corner) already logs all of it from any tab, with what you used last marked. Today keeps the sleep
panel, the next nap, running timers, the timeline and today's entries, which now sit higher.
`docs/flows.md`.

## 0.38.0 — 2026-11-03

**Ask Cochilo knows your log, and it's one tap away.** Ask now sits at the top of every tab (the
chat icon, beside Settings). It answers "how long should naps last?" with the baby's own naps this
week (the typical length, the shortest and the longest) and what's usual for the age. The
on-device AI is now given the whole log: the baby's name and age, each of the last seven days,
today's sleeps, this week's naps, windows and bedtime. When it writes something that can't be
checked against the log, Ask says so instead of answering as if it didn't understand.
`docs/assistant.md`.

## 0.37.1 — 2026-11-03

**The on-device AI accepts Google's current model file.** The check on the model file expected
the older zip format, so it would have turned down the Gemma file Google publishes today, both
when downloading and when choosing a file. It now only rules out the wrong kind of file (like a
web page), and a download is still checked against its published checksum.

## 0.37.0 — 2026-11-03

**The on-device AI downloads itself.** When you want Ask Cochilo to answer questions beyond its
list, tap Download (about 550 MB), from Settings or right under an answer it couldn't give. The app
downloads it on Wi-Fi in the background, with its progress in the notification, and checks it
before using it; on mobile data it waits for Wi-Fi unless you tap "Download now on mobile data".
No more finding the file yourself. `docs/assistant.md`.

## 0.36.0 — 2026-11-03

**Tatá's first suggestion is on.** Tatá approved her note on naps in arms. In the first three
months, after a week with five or more naps in arms, Patterns can show it once: it says how many
there were, that contact naps are normal at this age, and that a sling keeps your hands free, with
a link to her sling. It never shows at night, while the baby sleeps, in your first week with the
app, or again after "Don't show again". Settings › Notifications › Tatá's suggestions turns all of
them off. `docs/tata-slings/in-app-marketing.md` §12.

## 0.35.0 — 2026-11-03

**Asleep and awake, and a softer Log menu.** The Day timeline and History now read as the day's
two states: each sleep and each awake stretch is a main row, newest first, and a feed, diaper or
walk sits set in under the stretch it happened in, so a night feed shows inside the night. The
Log button's menu opens item by item, rising from the button on a soft curve instead of all at
once; with Remove animations it simply appears. `docs/flows.md`.

## 0.34.0 — 2026-11-03

**Ready for Tatá's notes by age (all off for now).** Twelve short notes from birth to two years,
one for each stage, signed by Tatá, will appear in Patterns once she writes or approves them:
what's common at that age, with no promises. Nothing changes on your screen yet.
`docs/tata-slings/in-app-marketing.md`.

## 0.33.0 — 2026-11-03

**Window reminders.** 30 and 15 minutes before the next nap or bedtime window, a notification
says it's coming ("Nap window in 30 min · expected about 3:51–4:11 PM"), with one button, Start
routine. It never comes while the baby sleeps, late, twice for the same window, or between 21:00
and 06:00, and it goes away when the window closes. Settings › Notifications › Window reminders
turns it off or picks 30 or 15 minutes only. `docs/notifications.md`.

## 0.32.0 — 2026-11-03

**Ready for notes from Tatá (all off for now).** The groundwork for Tatá's suggestions: rare,
signed notes in Patterns that start from what you logged and point to one product in her shop.
They follow strict rules (never at night, never while the baby sleeps, never in the first week,
at most one a week, "Don't show again" means never) and **none is switched on until Tatá approves
it**, so nothing changes on your screen yet. `docs/tata-slings/in-app-marketing.md`.

## 0.31.0 — 2026-11-03

**Your fabric, in the shop.** In Settings › Appearance › Fabric, the fabric you picked now has one
quiet line under it: "This fabric comes as a sling. See it in the shop", opening Tatá Slings in the
browser. Nowhere else, and nothing about it is recorded in the app. The safety check's "Tatá's
guides" now opens her guide to using the sling (positions, safety, videos) instead of the shop.
`docs/tata-slings/in-app-marketing.md`.

## 0.30.0 — 2026-11-03

**On-device AI for Ask Cochilo (optional).** Settings › Ask Cochilo › On-device AI: get a small AI
model (about 550 MB) that runs on the phone and answers questions beyond Ask Cochilo's list, from
your log. On Google Play the Play Store downloads it; in the family build you choose the model file.
Its answers use only your week's numbers and the reviewed answers, are checked before they're
shown (no invented numbers, no medicine, no judging), and are always labelled as written by the
model. Nothing is sent anywhere, and health questions still go to a person. `docs/assistant.md`.

## 0.29.0 — 2026-11-03

**Ask Cochilo.** Patterns › Questions about sleep: this week in four plain lines, then questions
answered from your baby's own log and from the research the app rests on (short naps, nights,
early mornings, bedtime, wake windows, daylight, routines, contact naps, the 4-month change, safe
sleep), with suggested questions one tap away. Everything is worked out on the phone; nothing is
stored or sent. Questions about health, medicines or how you're feeling are pointed to a person
(the paediatrician, SAMU 192, CVV 188), never answered by the app. `docs/assistant.md`.

## 0.28.0 — 2026-11-03

**Start fast, details after.** Start sleep is one tap with nothing to choose; when the baby wakes,
one tap says where she slept (the place used last is marked). The same for a feed started
without a side, a bottle's or pumping's amount and daylight: asked once, when it ends, and "Not
now" is always there. **Newest first:** the Day tab's hour-by-hour timeline now starts with what
just happened. `docs/flows.md`.

## 0.27.0 — 2026-11-03

**Drag to adjust.** On the Day tab, hold a sleep and drag it down or up to change when it ended,
in 5-minute steps, with the new time shown as you drag and Undo after. It never runs into the next
sleep or into the future. TalkBack has two actions for it. `docs/flows.md`.

## 0.26.0 — 2026-11-03

**Her fabrics, like cloth.** The eleven Tatá fabrics behind the Today card now repeat like a bolt
of fabric instead of mirroring at the edges, which turned prints like Tropical and Balão into a
kaleidoscope. No seams, no lighter or darker blocks. `docs/tata-slings/textures.md`.

## 0.25.0 — 2026-11-03

**Tablets and foldables.** On a wider screen the tabs move to a rail at the side and the screen
keeps a readable width; unfolded, Today stays on the left with Day, History or Patterns beside
it, so the timer and Start sleep never leave the screen. Phones are unchanged. `docs/tablet.md`.

## 0.24.0 — 2026-11-03

**The last nap counts.** After a short nap the next one is expected sooner, and after a long one
later, by as much as your baby's own log shows (sleep pressure, from the two-process model). The
card says what it did: "Last nap 35m, shorter than the usual 1h 18m, so 18m sooner." The predictor
is now tested on a synthetic year, from 8 weeks to a year old, which also found and fixed a case
where a late nap ending at bedtime could give a bedtime with no time awake. `docs/prediction.md`.

## 0.23.0 — 2026-11-03

**Locked backups.** Settings › Data › Protect backups: set a passphrase, and every backup you
export is locked with it (AES-256-GCM). Import asks for the passphrase only when this phone doesn't
already have it, and says so plainly when it doesn't match. Exporting is still one tap. Lose the
passphrase and nobody can open those files, so write it down. `docs/backup.md`.

## 0.22.0 — 2026-11-03

**Large text.** At the phone's largest font size every screen still shows every word: the header's
status line grows instead of being cut off, the "Fell asleep … min ago" chips and the Log menu's
choices wrap (Dirty no longer disappears), the tab names fit, and the charts' hours and days stay
readable. `docs/accessibility.md`.

## 0.21.0 — 2026-11-02

**Reading the data.** History has one-tap filters by kind (Sleep, Feed, Diaper…), and today's
Day ring shows the expected window for the next sleep as a faint band, so "on time" can be seen at
a glance. `docs/flows.md`.

## 0.20.0 — 2026-11-02

**A report for the doctor.** Settings › Data › Report for the doctor (PDF): one page with the
week's averages, what changed, where naps happened and two weeks of sleep as bars, made on the
phone and saved where you choose. "From the parent's own log. Not a medical record."
`docs/report.md`.

## 0.19.0 — 2026-11-02

**Routine steps.** Write the bedtime and nap steps once (Settings › Baby › Routine steps; there are
gentle defaults to start from), then tick them as you go when you open a routine; the ticked steps
are saved with it. Sliding the Routine row still logs at once. `docs/rituals.md`.

## 0.18.0 — 2026-11-02

**What changed.** Patterns now opens with what really changed in the last week against the two
before it ("Night sleep is longer · 10h 30m · 9h 00m before"), only when the change is real, in
neutral words: changes, not problems, and never a "regression". **Day and night milk:** pumped
milk says whether it was expressed by day or at night, and Patterns shows the week's split, so
night bottles can be night milk. `docs/changes.md`.

## 0.17.0 — 2026-11-02

**Where the baby slept.** Crib, in arms, sling, pram or car seat: each sleep remembers the last
place, and while the baby sleeps the panel shows "Where" to change it in one tap. Patterns shows
where naps happened this week, side by side ("Naps in arms and in the sling are normal in the first
months"). The first time you pick the sling, Cochilo shows the five T.I.C.K.S. checks for safe
babywearing, once. `docs/places.md`.

## 0.16.0 — 2026-11-02

**"Fell asleep a few minutes ago."** Under Start sleep / Woke up: "Fell asleep 5 · 10 · 15 min ago",
one tap, for when the baby fell asleep in your arms (or woke) before you could reach the phone.
**A quick-settings tile**: add "Cochilo sleep" to the phone's quick settings for Start sleep /
Woke up from the notification shade, lit while the baby sleeps. `docs/flows.md`.

## 0.15.0 — 2026-11-02

**A night light.** From 19:00 to 07:00 the app turns warm amber on black, with no blue light (light
at night is what the research says to keep dim; red and amber disturb least). Settings ›
Appearance › Night light turns it off. **Daylight:** from 14:00, if there's been no daylight
between 12:00 and 16:00, Today offers "Go outside" to start a walk in one tap. **Wind-down:** the
notification says "dim the lights" in the hour before bedtime. `docs/night-light.md`.

## 0.14.0 — 2026-11-02

**Little celebrations.** When what you were trying for happens (a full nap, a sleep right on time,
the longest night of the week, daylight done, the routine before bed, a whole week logged), the
baby you drew lifts its arms in a soft pink card with a few warm words, then goes away by itself.
Never a scolding, never a promise; at most three a day; just the words, dim, at night. Settings ›
Notifications › Little celebrations turns it off. `docs/celebrations.md`.

## 0.13.0 — 2026-11-02

**Meet the baby.** A warm first run: welcome, the baby's name and birth date, then how the baby
looks (skin tone, hair, girl / boy / rather not say), skippable. The baby appears as a little
drawing wrapped in a Rosa Chá sling: in the first run, asleep on Today before the first sleep, and
in Settings › Baby, where the look can change any time. Dates now always read in the app's
language ("qui 9 jul"). Backups are version 3 (older backups still import). `docs/onboarding.md`.

## 0.12.0 — 2026-11-02

**Softer, warmer, made for her** (a design review with the owner's graphic-design-fundamentals and
impeccable skills, `docs/design-review.md`).
- No colour behind the screen titles: they stand on their own, plain and a touch lighter.
- Teal is only for the one main button. The active tab and a picked chip are a soft blush; the
  edge round the screen is a soft Rosa Chá hem instead of a teal line.
- Captions in quiet sentence case ("Awake for", "Running") instead of spaced capitals.
- Every button is a soft pill, like the Log menu and the widget.
- Icon rounds in dark are a little lighter, no longer muddy.
- A feed under a minute reads "<1min", not "0min".

## 0.11.0 — 2026-11-02

**A home-screen widget: Sleep and feed.** Start sleep / Woke up and the two breasts, one tap each
from the home screen, with "Awake since 14:20" and a running clock. The breast used last has the
coloured dot and outline; while a breastfeed runs, its side reads Finish and the other side
switches. Light and dark, English and Portuguese. `docs/widget.md`.

## 0.10.0 — 2026-11-02

**Motion, sleep first, and a quieter "last".**
- Every action answers with a small movement: presses dip, the Log menu rises from its button and
  its plus turns into a cross, lit options and chips cross-fade, the list opens for a new entry,
  the card fades to the night panel when sleep starts. Off with the phone's Remove animations.
- Sleep is a size up everywhere entries are listed (History, Today, the Day tab's hour by hour,
  the log rows, the Log menu): a bigger round and a bold name, so the sleep pattern stands out.
- "LAST" / "ÚLTIMO" is gone: the option used last has an outline in its kind's colour and a small
  dot before its name, and the "last" lines start with the same dot. TalkBack still says "used last".
- Design system 0.9.0 (`design-system/0004-…`). `docs/flows.md` (Motion, Sleep leads, the Last mark).

## 0.9.0 — 2026-11-02

**Her fabrics.** Pick one of Tatá's real slings in Settings › Appearance › Fabric and it wraps the
Today card, the way a sling wraps the baby: Pérola, Rosa Chá, Capuccino, Mel, Trevo, Cereja,
Crepúsculo, Chumbo, and the prints Balão, Folhagem and Tropical, photographed from her shop. One
tap, applied at once; Plain is the default. No text ever sits on a fabric. Design system 0.8.0;
`docs/tata-slings/textures.md`.

## 0.8.0 — 2026-11-02

**Tecidos: warmer, more colourful, made for her.** Cochilo looked cold and masculine (navy lines on
cream). Now each kind of entry wears one of Tatá's fabrics: its icon sits in a soft round of its
colour (Rosa Chá for a feed, Lavanda for night sleep, Imensidão for a nap, Trevo for a diaper,
Amora for pumping, Mel for daylight, Turquesa for a routine, Cereja for medicine, Capuccino for a
note), in every list, the Log menu, the Day tab and the timelines. Text is a warm plum-grey instead
of navy, the page has a blush, the sleep panel is a plum night, corners are rounder, and the Log
button is Amora. Teal stays the one action. Light and dark, contrast checked. Design system 0.7.0;
`docs/tata-slings/design-system.md` §3a.

## 0.7.0 — 2026-11-02

**The Log button.** Logging no longer sits below the fold: a navy button in the bottom-right
corner, where a right thumb rests, on every tab. Press and hold: the kinds of entry rise above it,
the commonest nearest the thumb. Without lifting, drag up to Feed and its choices appear beside it
(Left · Right, the last breast marked LAST, and the last bottle's amount); slide onto one and let
go: logged, with Undo. Let go on a kind to open its sheet; anywhere else, nothing happens. Diaper
(Wet · Both · Dirty), Sleep (start or wake up), Medicine (the last one again), Pump, Daylight,
Routine and Note are there too. A tap opens the same menu to use with taps (and TalkBack). After
Wunderlist's thumb-corner add button and marking menus. `docs/flows.md`.

## 0.6.0 — 2026-11-02

**Remember actions** (now a project rule, with "fewest taps"). What was done last is shown and
offered first: the Feed row says which breast was last and the slide marks it (LAST), so you can
decide whether to continue on that side; Diaper, Daylight and Routine say and mark their last kind;
Pump shows the last amount; Medicine shows the last medicine and when. Every sheet opens on the last
choice (breast side or bottle with its amount, diaper kind, pump amount, medicine name) and says
"Last time: Right breast · Today 14:20". Replaces the "next: other breast" suggestion.
Audit per entry type: `docs/flows.md`.

## 0.5.0 — 2026-11-02

**Icons for every kind of entry**, so entries are known at a glance: a drop for a breastfeed, a
bottle, a spoon for solids, a pump, a diaper, the moon for sleep, the sun for daylight, a bath for a
routine, a capsule for medicine, a pencil for a note. On the log rows, every entry list, the Day
tab's hour-by-hour list (in the entry's timeline colour) and the timers' notifications. Always next
to the name. Icons are in the design system's line set (patch in `design-system/`).

**Medicine is its own entry type**, with its own log row and "i". The Napper import brings
medicine in as Medicine; medicine already imported as notes ("Remédio · Luftal") becomes Medicine
("Luftal") on first launch (database version 2). Backups are format version 2: older backups still
import; a version-2 backup can't be imported into an app older than 0.5.0.

## 0.4.0 — 2026-11-02

**Notifications, like Napper's.** A quiet live notification: asleep or awake, a running clock,
"expected wake 4:47 PM" or "next nap 4:01 PM", a timeline bar for the stretch (with dots for what
was logged meanwhile and the nap window in teal), and Woke up or Start sleep. Each running timer
(feed, pump, walk) gets its own with Finish, and Switch side for a breastfeed. Asks permission once
(Android 13+); Settings › Notifications turns it off. `docs/notifications.md`.

**Fixes**
- Finishing a running timer from its sheet (switching "running now" off) saved it as 0 minutes,
  because the end defaulted to the start time. It now defaults to now, so a timer that crossed
  midnight keeps its real length.
- "Awake 0min" between two sleeps a minute apart: durations now follow the clock as shown
  (01:53 → 01:54 is 1 minute), not the seconds underneath.

## 0.3.0 — 2026-11-02

**Cochilo, da Tatá Slings.** The app wears the Tatá Slings brand, through the Gabriel Design
System's new *brands* concept (design system 0.6.0, patch in `design-system/`).

- Colours: Tatá teal for the one action, the frame and the active tab; navy text; a warm Pérola
  page; Cereja for alerts; a navy sleep panel, in light and dark. Contrast checked in both.
- Type: Libre Franklin (the website's typeface) replaces Ubuntu Mono; times use tabular figures.
- Mark: a crescent moon resting in a sling, navy on teal: the launcher icon and the header mark.
- Settings › About: "Cochilo, da Tatá Slings" opens tataslings.com.br.
- Unchanged: spacing, radii, sizes, motion, components, data colours and every Gabriel rule.
- Docs: `docs/tata-slings/design-system.md` (what's built), `design-system/README.md`, AGENTS.md
  (the brand, and the owner's standing rules: document everything, always merge and release).

## 0.2.2 — 2026-11-02

**Slide to log.** On Today's log rows, slide instead of tapping: Feed left = left breast, right =
right breast; Diaper Wet · Both · Dirty; Daylight, Routine, Pump. Slide a running breastfeed to
switch side. Every quick entry offers Undo. The Feed row and sheet suggest the other breast from
the last feed. Audit of taps per entry: `docs/flows.md`.

## 0.2.1 — 2026-11-02

**Fix:** Import from Napper always failed on the phone ("isn't a Napper logs export"). A label
template was formatted with no numbers and threw before the file was read.

## 0.2.0 — 2026-11-02

- **Self-update** (family build): checks GitHub when opened, shows "Update available", installs
  after checksum and signature checks. Two builds: `github` (family, updates itself) and `play`
  (no internet permission at all). `docs/distribution.md`.
- **Import from Napper:** Settings › Import from Napper reads Napper's logs CSV (naps, nights from
  bedtime to wake-up split by night wakings, feeds, bottles, diapers, medicine notes).
- **Day tab:** a 24-hour ring and an hour-by-hour timeline with awake windows.
- **"i" buttons** explaining every kind of entry, in English and Portuguese.
- **Fix:** Today's log listed diapers and notes from earlier days.

## 0.1.1 — 2026-11-02

Same app as 0.1.0; docs only (the Tatá Slings plans in `docs/tata-slings/`).

## 0.1.0 — 2026-11-01

First build: one-tap sleep timer, logs for feeds, pumping, diapers, daylight, routines and notes,
a next-nap prediction that shows its numbers, History, Patterns, export and import. Built on the
Gabriel Design System, research in `docs/research.md`.
