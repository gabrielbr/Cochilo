---
name: less-is-more
description: Use when designing, reviewing or changing any Cochilo screen, flow, notification or widget, or when the owner asks to reduce clutter. A checklist for removing what doesn't earn its place, for a tired parent using the phone one-handed, often in the dark. Pairs with the impeccable and graphic-design-fundamentals audits.
---

# Less is more (Cochilo)

Cochilo is used with a baby in arms, at 3am, by someone who slept four hours. Every element on a
screen costs attention. This skill decides what earns its place, what moves one tap away, and what
goes. Run it on every new screen and flow, and on every UX audit. Write the result into the
release's audit (`docs/audits/<version>-<topic>.md`).

## Where the rules come from

- **Calm technology** (Weiser and Brown, Xerox PARC, 1995; Amber Case, *Calm Technology*, 2015):
  technology should stay in the background and speak only when it helps. Give people what they
  need to solve their problem, and nothing more. Measure a product by how well it served the
  person, not by how much attention it took.
  ([overview](https://en.wikipedia.org/wiki/Calm_technology), [Case's principles](https://caseorganic.com/post/principles-of-calm-technology/))
- **Progressive disclosure** (credited to Nielsen; IBM's Carroll and Rosson): show what most people
  need now, and keep the rest one step away. Don't hide what's required, what's legal, or what a
  destructive action will do.
  ([summary](https://thedecisionlab.com/fr-CA/reference-guide/design/progressive-disclosure))
- **Hick–Hyman law**: more equal choices take longer to decide. It's strongest for unfamiliar
  lists; people scan familiar ones. So cut options where they're unfamiliar or equal, and order the
  rest by frequency.
  ([IxDF on Hick–Hyman and its limits](https://ixdf.org/literature/article/the-hick-hyman-law-an-argument-against-complexity-in-user-interface-design))
- **Reduce load, don't strip features**: clean up the mental clutter (duplicates, chunking,
  defaults) rather than deleting what people use.
  ([Webdesigner Depot](https://webdesignerdepot.com/10-ways-to-reduce-cognitive-load-without-turning-your-ui-into-a-minimalist-ghost-town/))
- **Baby trackers in practice** (practitioner write-ups, not studies): big targets for one hand; the
  core actions on the first screen; a dark, calm night; and the least possible data entry (no
  1–10 scales). ([one developer's write-up](https://zerodad-issmcsp.pages.dev/articles/best-free-baby-tracker.html))
- **Cochilo's own rules** (AGENTS.md): fewest taps, remember actions, one mint action per screen,
  celebrate never scold, sleep leads.

## The test for every element

For each label, number, line, card, button, chip, row, "i" and hint on the screen, ask in order:

1. **Does it help the next thing she does, or a decision she makes today?** If yes, it stays visible.
2. **Is it shown somewhere else on the same screen, or one tap away already?** Then show it once.
   Keep the copy nearest the action, and remove the other.
3. **Does she need it every time, or only to understand once?** Explanations, sources, the
   predictor's numbers and how-to hints stay one tap away ("Why?", "How it's guessed"), or show
   until she's used the thing once.
4. **Does it matter for the app's goal: calm, long nights and a rhythm she can follow?** Numbers
   about the night lead. Others are grouped or folded.
5. **Is it an action she takes weekly or less?** Then it lives in Settings or a sheet, never on Today.

| Answer | Do |
|---|---|
| Helps now | Keep, in the order of how often it's used |
| Duplicate | Keep one, nearest its action |
| Understand once | One tap away, or shown until first use |
| Weak for the goal | Group it into one row, or move it to Patterns |
| Rare | Settings or a sheet |
| None of these | Remove |

## Screen rules

- **One subject per screen, and one mint action** (Today: the sleep; a sheet: Save).
- **A number only with its meaning.** "Night: 10h 05m" beats a bare "10h 05m". Never two numbers
  that say the same thing (like the header and the card both saying "Awake · 40m").
- **Text in the meta role is a cost.** One meta line per card at most; a second line has to earn it.
- **No permanent hints.** A gesture hint shows until it's been used, then goes.
- **Fewer "i" buttons.** One per screen at most, on the thing people misread; not on the main card.
- **Night is quieter than day.** At night show the state and the two actions, nothing to read.
- **Defaults over questions.** Guess (the night, the place, the side), show the guess, and allow
  one-tap correction. Ask afterwards, never before (details after).
- **No duplicate paths on one screen.** If the header opens Ask, Patterns doesn't need a row for it.
- **Celebrate rarely.** At most three a day, words only at night.

## Flow rules

- **Count taps** (docs/flows.md). Removing clutter must never add a tap to a common entry.
- **A flow ends where it started.** No dead ends, and no "Done" screens.
- **Undo instead of confirm.** Confirm only when it's destructive and can't be undone.

## Output

In the audit, list each removed, moved or merged element with its answer (1–5) and where it went.
Then score:

| Dimension | 0–4 |
|---|---|
| Elements per screen, compared with before | |
| Duplicates left | |
| Text to read at night | |
| Taps on the common entries (must not rise) | |
