# Night light, daylight and the wind-down (0.15.0)

Light is the baby's main time cue (research §2.3): daylight between 12:00 and 16:00 was linked to
better nights at 6–12 weeks, and light in the evening and at night suppresses melatonin even when
dim; red and amber are the least disruptive. Three small features follow it. None promises how
the baby will sleep.

## Night light (on by default)

From **19:00 to 07:00** the app switches to the design system's **night** theme: warm amber and
red on near-black, no blue and no teal. The one action is an amber fill; data colours are warm
variants told apart by lightness, always beside their word. Settings › Appearance › **Night
light** turns it off. It is checked each minute, so it starts and ends on its own. It answers
Tatá's own question, "yellow or red light at night?", inside the app.

| Role | Night |
|---|---|
| page / surface | `#0D0806` / `#170F0B` |
| ink / muted | `#E9A25F` (9.3:1) / `#B67A47` (5.6:1) |
| the one action (`mint`) | `#E0A060`, text `#0D0806` |
| Log button (`fab`) | `#B8642F`, plus `#0D0806` |
| selected / frame | `#3A2416` / `#241610` |

Every contrast pair the light and dark themes pass, the night theme passes too
(`check_tokens.py`). The illustration and the fabric photos keep their colours; the brand mark
stays teal (small, and the brand's).

## Daylight nudge

From 14:00 to 16:00, if no daylight has been logged in the 12:00–16:00 window and none is
running, Today shows one quiet row: "No daylight yet · 12:00–16:00 helps the body clock", with
**Go outside**, which starts a daylight walk timer in one tap. Not a notification. Rules in
`domain/Daylight.kt` (`DaylightTest`).

## Wind-down

In the hour before the predicted bedtime, Today already says "Wind down: dim the lights, warm or
red light only"; now the live notification says it too ("bedtime 19:30 · dim the lights").
