# Components, as Cochilo uses them

Cochilo's Compose kit (`app/src/main/java/br/gabriel/cochilo/ui/kit`) follows this table. It was
the design system's `docs/components.md` at 0.27.0, moved here in Cochilo 0.40.1.

**Which rows are whose:**
- A row that is generic (any app could use it: Frame, Header, Tab bar, Rail, Quiet link,
  Checklist, Conversation, Filter chips…) belongs to the **core**. Those changes go upstream
  ([../../UPSTREAM.md](../../UPSTREAM.md)); this copy then follows the core.
- A row only Cochilo has (Log button, Slide row, Kind icon, Last mark, Fabric frame and picker,
  Baby avatar, Celebration, Home widget, Earlier chips, the quick-settings tile, the expected
  window, drag to adjust, the cascade, asleep and awake, the Tatá note) is **Cochilo's**, and is
  changed only here.

One name per component across apps. uikit = `br.gabriel.ui.Ui` (NotTheOmiAIApp); Compose =
PulseLoop's `ui/components`. Status: ✅ both, 🟡 one app, ⬜ to build.

Cochilo's Compose kit (`br.gabriel.cochilo.ui.kit`) follows the uikit spec column; where it adds a
component, the row says so.

| Component | Spec | uikit | Compose | Status |
|---|---|---|---|---|
| Frame | 4dp mint border; loading line (uikit). Cochilo: `frame-line`, a soft hem under the brand | `FrameDrawable`, `FrameSweep`, `Motion.loading` | `AppChrome` frame | ✅ |
| Header | 56dp; monogram 32 / app name over one status line / one trailing icon, at most two when the second opens a screen used from every tab (Cochilo: Ask, then Settings; `space1` apart, 48dp each) | `Ui.header`, `Ui.backHeader` | `AppHeader` (greeting + headline line, ring pill, gear) | ✅ |
| Tab bar | 64dp; active = mint pill behind the icon (Cochilo: `selected` blush); badge 12sp bold coral-text on coral-soft | `Ui.tabBar` | `NavigationBar`, `NavigationRail` from 600dp | ✅ |
| Navigation rail | `rail` 80dp wide, from `width-medium` (600dp): the same tabs stood on their side, the same `selected` pill and words; the screen's column stays at most `content-max` (600dp), centred. From `width-expanded` (840dp) Cochilo shows Today beside the picked tab (Day when Today is picked) | `Ui.rail` | `NavigationRail` | ✅ |
| Screen title | uikit: display 30/36, one word on the mint highlighter. PulseLoop tabs have no title. Cochilo (0.11.0): display 600 in ink, no highlighter | `Ui.title` | — | 🟡 |
| Section caption | 12/16 bold caps muted, 40 above, 12 below, no divider. Cochilo: 13/18 semibold, sentence case | `Ui.caption` | `PulseType.eyebrow` | ✅ |
| Row group | rows on surface, radius 8, inset 16, hairline between rows | `RowGroups` | `SettingsComponents` | ✅ |
| Row | 48dp min (uikit 56); label + ≤ 1 meta line; one thing on the right | `Ui.row` | settings rows | ✅ |
| Switch row | the whole row toggles, Role.Switch | `Ui.switchRow` | `Modifier.switchRow` (`A11y.kt`) | ✅ |
| Item row | title, chip, meta, 2-line excerpt | `Ui.itemRow` | — | 🟡 |
| Button | 52dp (Cochilo: a pill, `radius-round`); PRIMARY mint (one per screen), SECONDARY ink outline, DANGER coral-text outline + confirm, STOP solid coral with `on-coral` label | `Ui.button` | Material `Button`/`FilledTonalButton`; one filled accent per screen (Actions) | ✅ |
| Icon button | 48dp target, 22dp icon, content description | `Ui.iconButton` | `IconButton` | ✅ |
| Chip | read-only state; MINT / CORAL (text on tint) / MUTED | `Ui.chip` | chips with dither ground | ✅ |
| Filter chips / segmented | 32dp pills, selection semantics. Cochilo: picked = `selected` blush; a dark variant on the night panel (outlines in `on-panel-muted`, text `on-panel`) | `Ui.filterChips` | `PulseSegmented` | ✅ |
| Field | 52dp, radius 4, line border; search clears | `Ui.field` | `OutlinedTextField` | ✅ |
| Snackbar | ink ground, page text, `snackbar-action` action, 8 s | `Ui.snackbar` | `SnackbarHost` (⬜ replace remaining Toasts) | 🟡 |
| Sheet | title, one line, primary + secondary; confirms and permission reasons | `Ui.sheet` | `ModalBottomSheet` / `LogSheet` | ✅ |
| Recorder panel / player | panel colours, timer 56/64, −15 s, scrubber | `Ui.recorderPanel`, `Ui.player` | — | 🟡 |
| Empty state | title 22/28, one line, ≤ 1 primary | `Ui.emptyState` | — | 🟡 |
| Skeleton | grey bars instead of "Loading…" text | `Ui.skeletonRow` | — | 🟡 |
| Ring / gauge | swept fill 900ms, dithered track, spoken summary. Cochilo day ring: today's expected next-sleep window as the nap colour at 35% under the real sleeps, named in the legend and spoken | — | `VitalRingGauge`, `ActivityRings` | 🟡 |
| Charts | one message per chart, zone colours, spoken summary | — | `Charts`, `ZoneLineChart` | 🟡 |
| Update sheet | "Update available · {version}", release notes, Update · {size} (primary) / Later; "Downloading… n%" with the frame's line, Later becomes Cancel — [self-update.md](self-update.md) | `Ui.updateSheet` | `UpdateDialog` | 🟡 |
| Check for updates | About row: version, a secondary button, one result line; release builds only — [self-update.md](self-update.md) | `Ui.checkForUpdates` | `CheckForUpdatesButton` | 🟡 |
| Sleep panel | Cochilo: `panel` block, state word over the `timer` role, one PRIMARY (Start sleep / Woke up) | — | Cochilo `SleepPanel` | 🟡 |
| Timeline / actogram | Cochilo: 24h bar, `timeline` 24dp high, `event-*` blocks on `line`, hour labels muted, spoken summary | — | Cochilo `Timeline`, `Actogram` | 🟡 |
| Vertical timeline: asleep and awake | Cochilo: the day as its two main states, newest first. A sleep is a main row (its bar, `kind-badge-lead`, label role); an awake stretch is a main row with a hollow `space3` ring outlined in `muted` on the rail (awake isn't an entry: no data colour, no icon), body role, its range and length in meta, a TalkBack heading. Every other entry sits under the stretch it happened in, set in by `space6`, its time only in its meta, so the time column reads the stretches' starts in order. In a list: the same, with the ring in a `kind-badge-lead`-wide slot | — | Cochilo `TimelineAwake`, `TimelineRow(indent)`, `ListRow(indent)`, `AwakeMark` | ✅ |
| Vertical timeline: drag to adjust | Cochilo: press and hold a finished sleep, drag down (later) or up (sooner) in 5-minute steps, 12dp a step; the held row turns `selected`, its bar follows, its meta says the new end live in `on-selected`; a haptic tick per step; let go saves, with Undo; TalkBack gets two actions (5 minutes later / sooner) | — | Cochilo `TimelineRow(adjust)` | ✅ |
| Conversation (Ask) | Cochilo: the parent's question right-aligned in `selected` / `on-selected`, at most half the column plus 64dp; the answer a `surface` card with a hairline, meta labels ("From your log", "What can help", the source) over body text; suggested questions as filter chips; one teal Ask. Fixed replies for health, medicine and the parent's wellbeing use the same card, no colour alarm. An answer written by an on-device model uses the same card with a meta label under it that says so, always; while it writes, a meta "Thinking, on this phone…" card (a live region) | — | Cochilo `AskScreen` | ✅ |
| Quiet link | A line of context in `muted` meta, then the action in `ink` meta and a chevron, in a 48dp row; the whole row is one button for TalkBack with a full spoken sentence. For the rare link out of the app (Cochilo: the picked fabric's sling in the shop). Never `mint`, never a filled or outlined button, never more than one per screen, never on a safety or logging surface | — | Cochilo `QuietLink` | ✅ |
| Tatá note | Cochilo: a short signed note in a `surface` card with a hairline: her words in body `ink` (starting from what the parent logged), her byline in meta `muted`, at most one Quiet link, then "Don't show again" as a meta `muted` text button in a 48dp row. Never `mint`, never a texture behind the words, never an image or a price, never on Today, a sheet, a safety screen, a celebration or at night; at most one on screen | — | Cochilo `TataNote` | ✅ |
| Cascade (Log menu) | Cochilo: a menu opens item by item, nearest the button first, each one rising `space4` and fading in for `motion-emphasised`, `motion-stagger` apart; the column bows in a soft arc (up to `space3`, deepest in the middle), so it reads as a gesture, not a grid. Closing is one sink, all together. With Remove animations: all at once | — | Cochilo `LogButton` | ✅ |
| Slide row | Cochilo: a row that also slides; past the touch slop it becomes its options side by side, the one under the finger in `ink` with a haptic tick; letting go logs, back to start cancels; the last-used option says LAST; tap still opens the sheet; each option is a TalkBack action | — | Cochilo `SlideRow` | 🟡 |
| Kind icon | Cochilo: an entry kind's 22dp line icon in its `event-*` colour, on a `kind-badge` (36dp) round of `event-*-soft` (sleep: `kind-badge-lead`, 44dp, with its name in the label role, because sleep leads); always beside the kind's name; on a highlighted row the round is `on-fab` at 18% and the line `on-fab` | — | Cochilo `KindIcon` | 🟡 |
| Last mark | Cochilo: the option used last time: an `outline` (2dp) in its kind's `event-*` colour and a `space-2` dot of that colour before its name; on a lit option the dot is `on-fab`. Spoken as "last" by TalkBack, never written. Also before the "last time" line of a row or sheet | — | Cochilo `LastDot`, `SlideOption.last` | 🟡 |
| Baby avatar | Cochilo: the parent's own baby in a Rosa Chá sling, drawn on a 100-unit grid in the brand's illustration style (single line weight, round caps, flat fills); `illustration-*` colours only, the same in both themes; moods awake / asleep / happy, blink and lift for celebrations; a catchlight in open eyes so faces read on every skin tone; described for TalkBack. Sizes `avatar` (112) and `kind-badge-lead` (44) | — | Cochilo `BabyAvatar` | 🟡 |
| Look picker | Cochilo: a live avatar preview, then skin tone (six 48dp swatches with spoken names), hair (filter chips), hair colour (swatches, hidden for "none yet"), girl / boy / rather not say (chips). One tap each, defaults for all | — | Cochilo `LookPicker` | 🟡 |
| Celebration | Cochilo: one small moment when an intent is met: a `selected` blush card with a hairline under the header; the Baby avatar (happy) lifts its arms (`motion-emphasised`) beside one line (label role); gone by itself or at a tap, never blocks; at night words only, muted, no motion; polite live region. Celebrate, never scold | — | Cochilo `CelebrationCard` | 🟡 |
| Earlier chips | Cochilo: under the sleep panel's one action, one sentence "Fell asleep [5] [10] [15] min ago": hairline pills (32dp in 48dp targets), meta text either side; only valid choices shown; on the night panel in `on-panel` colours; each spoken in full | — | Cochilo `EarlierChips` | 🟡 |
| Quick-settings tile | Cochilo: "Cochilo sleep", the moon icon; inactive while awake (label Start sleep), active while asleep (label Woke up, subtitle "Asleep since 14:50"); unavailable before a baby is added | — | Cochilo `SleepTile` | 🟡 |
| Checklist | Cochilo: steps ticked one by one in a row group; a ticked step has `gg_ic_check` in a `selected` blush round (`kind-badge`), the words unchanged (no strike); each row a checkbox | — | Cochilo `Checklist` | 🟡 |
| Printable report | Cochilo: one A4 page (595 × 842 pt), 48pt margin, light theme: title (22 bold), subtitle with absolute dates, sections (11 semibold muted captions, 11pt rows with values on the right edge, hairlines), 14 day bars in data colours with a legend, footer "not a medical record" | — | Cochilo `Report` | 🟡 |
| Home widget | Cochilo: a 4 × 2 home-screen widget (RemoteViews): the mark, "Asleep / Awake since 14:20" and a running clock, then one row of 48dp buttons: the one mint action (Start sleep / Woke up, wider) and the two breasts (secondary; the one used last has the Last mark; the running side lit in `event-feed-soft` with an `outline` and reads Finish). `surface` card, `radius-m`, buttons `radius-s`, `space-3` padding, `space-2` gaps. Colours copied from the tokens into resources, checked by a test | — | Cochilo `CochiloWidget` | 🟡 |
| Fabric frame | Cochilo: a card wrapped in a brand texture, the way a sling wraps the baby: the texture is a band (`space8` on top, `space3` elsewhere) round a card of its own colour (`radius-s` inside `radius-m`); no word ever sits on the texture. Without a texture it is the plain card | — | Cochilo `FabricFrame` | 🟡 |
| Fabric picker | Cochilo: the brand's textures as 48dp round swatches in one scrolling row, the name under each (meta), the next one always peeking in; one tap picks and applies; the picked one has a 2dp `ink` ring; each is a radio button for TalkBack. First choice: plain | — | Cochilo `FabricPicker` | 🟡 |
| Log button | Cochilo: the main action in the thumb's corner (bottom-end, `space4` in, `button` size, `fab` circle (Amora under the brand), plus that turns to a cross). Press: the entry kinds rise above it, most frequent nearest; drag up to a kind, sideways onto a choice; let go logs (choice) or opens the sheet (kind); elsewhere cancels. A tap opens it for taps and TalkBack. Scrim `page` at 94%. Never mint: the screen's one mint action stays | — | Cochilo `LogButton` | 🟡 |
| Fairy transformation | once, over everything; glow, wand, sparkles, "Tatá, te amo!"; 4.2s (1.4s reduced) — [fairy-mode.md](fairy-mode.md) | ⬜ | `FairyTransformOverlay` | 🟡 |

**Motion (Cochilo, 0.9.0).** Every target scales to 97% while pressed (`motion-standard`); the
Log menu fades and rises from its button and its plus turns to a cross (`motion-emphasised`);
every state change of colour cross-fades (`motion-standard`): a lit option, a chip, a tab, the
sleep panel going to night (`motion-emphasised`); lists grow and shrink instead of jumping
(`motion-emphasised`); a new fabric cross-fades. Easing is the system's. With Remove animations
on, every one of them snaps.

Rules for every component: built from tokens only; 48dp targets; TalkBack reads label + value +
state; states (default, pressed, disabled, loading, empty, error) defined before it ships; a new
component, or a change to one, is added to this table in the same PR.
