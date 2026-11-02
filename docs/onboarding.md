# Onboarding and the baby's avatar (0.13.0)

## The first run

Two short steps, then Today. Everything can change later in Settings.

1. **Meet the baby.** "Welcome to Cochilo", the avatar asleep in the sling, one line ("Everything
   stays on this phone"), then name, birth date and (only if born early) the due date. **Next**.
2. **How the baby looks** (skippable). A live preview of the avatar, then one tap each: skin tone,
   hair, hair colour (hidden for "none yet"), and girl / boy / rather not say. **Done** saves;
   **Skip for now** saves the baby with the default look.

Taps: name typed, then **Next**, then **Done** (or **Skip**): the same as the old "Add baby" sheet
plus one, and the second step can be skipped.

## The avatar

`ui/kit/BabyAvatar.kt`: the parent's own baby, wrapped in a Rosa Chá sling, in the brand's
illustration style (single-weight line, rounded caps, flat fills). Drawn on a 100 × 100 grid and
scaled, so every combination is one drawing; moods **awake**, **asleep** and **happy** (with the
arms up), and parameters to blink and lift for the celebrations (roadmap 1.1).

- **Skin tone**: six tones, lightest to deepest, the emoji range. The app asks how the baby
  *looks*, never race or ethnicity: that's what a drawing needs, it is accurate for mixed families,
  and it is kinder to ask. Each eye has a catchlight so the face reads on every tone (audit).
- **Hair**: none yet, a little (one curl), curly, straight, wavy; black, brown, blond, red.
- **Girl / boy / rather not say**: small details only (a bow for "girl"). Colours never change by
  gender; the palette is her fabrics.
- **Colours**: the design system's illustration palette (`illustration-*`), the same in light and
  dark: a baby doesn't change colour at night. Never UI state, never text.

**Where it shows:** onboarding, the look picker, Today's card before the first sleep ("Ready when
the baby is", the baby asleep), and Settings › Baby › "How Bebê looks". Next, the celebrations.

## Data

`Baby.look`: one short text (`skin=3;hair=CURLY;color=BROWN;gender=UNSAID`, `domain/BabyLook.kt`,
`BabyLookTest`), so new choices need no migration. Room 2 → 3 adds the column; backups are
version 3 and still read versions 1 and 2 (no look = the default). It never leaves the phone.

## Also in 0.13.0

Dates follow the language the app is showing (`locale_tag` in each strings file): a Portuguese
screen says "qui 9 jul", never "Thu 9 Jul".
