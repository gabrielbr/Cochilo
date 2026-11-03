# Tablets and foldables

Since 0.25.0 Cochilo uses the room a wider screen gives, at the design system's window classes
(`width-medium` 600dp, `width-expanded` 840dp, Android's own). `ui/kit/Shell.kt`.

| Width | What you see |
|---|---|
| **Compact** (a phone, under 600dp) | As always: the header, the screen, the tab bar under the thumb |
| **Medium** (600–840dp: a small tablet, a foldable half open, a phone on its side) | The tabs stand on their side as a **rail** at the start; the screen's column stays at most 600dp (`content-max`), centred, so lines stay readable |
| **Expanded** (840dp and up: an unfolded foldable, a tablet on its side) | The rail (Today, Day, Patterns since 0.40.0), and **Today beside the picked tab**: Today always at the start, Day, History or Patterns next to it (Day when Today is picked) |

- The class follows the window's width (`LocalConfiguration.screenWidthDp`), not the space left
  inside the frame, the way Android's window classes are defined.
- The Log button stays at the bottom end of the screen's column, under the right thumb.
- Settings and onboarding are one centred column at any width.
- Screenshots: `today_tablet_light` and `patterns_tablet_light` (800dp, medium),
  `today_foldable_light`, `history_foldable_dark` and `settings_foldable_light` (841dp, expanded).
