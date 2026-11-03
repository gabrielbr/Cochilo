# Cochilo — Research: Infant Sleep, Circadian Rhythms & Sleep Apps

> Research compiled 2026-11-01 as groundwork for **Cochilo**, a personal-use baby sleep app (in the spirit of Napper/Huckleberry) for tracking and predicting our son's sleep.
> Context: baby is ~3 months old (≈13 weeks) at the time of writing, which is right in the window where the circadian rhythm is consolidating.
>
> ⚠️ This is not medical advice. Anything worrying (breathing, feeding, weight, extreme sleepiness/irritability) goes to the pediatrician.

---

## 1. TL;DR — what matters for the app

1. **Two systems drive sleep**: *Process S* (homeostatic sleep pressure that builds while awake and drains during sleep) and *Process C* (the circadian clock). Every serious prediction approach is, at heart, a version of this **two-process model**.
2. **Newborns have almost no circadian rhythm.** Day/night hormone rhythms appear in sequence between **~6 and 18 weeks**: cortisol rhythm first (~8 wk), melatonin ~1 week later together with consolidated night sleep, then body-temperature rhythm (~10 wk); more consolidated sleep/wake blocks by ~15 weeks.
3. **Light is the main zeitgeber (time cue).** Babies with more daylight, especially **12:00–16:00**, slept better at night at 6–12 weeks. Evening light suppresses melatonin strongly in young children, even at dim levels (20–40 lux).
4. **"Wake windows" are useful heuristics, not science.** No study validates specific minute ranges. The biology (sleep pressure) is real; the tables come from sleep consultants. **So the app should learn from our own baby's data, and use age-based defaults only as a starting point.**
5. **Normal variation is huge.** Infant (0–12 mo) total sleep averages **12.8 h/24h (range 9.7–15.9 h)**. AASM recommends **12–16 h incl. naps for 4–12 months**; no recommendation exists for <4 months because variation is too wide.
6. **"Sleeping through the night" happens gradually.** In a longitudinal study, self-regulated sleep length grew most between 1 and 4 months; by 12 months ~86% slept 8 h straight, but only ~73% slept 22:00–06:00.
7. **Commercial apps (Napper, Huckleberry) need ~1 week of logging** before predictions get good. Their algorithms are proprietary; they combine age-based wake windows with the individual's history. We can do the same, and do it transparently.
8. **Safety rules are not optional**: back to sleep for every sleep until 1 year, firm flat surface, no soft bedding, and no device that claims to prevent SIDS (AAP 2022). The app should never imply it monitors safety.

---

## 2. The biology of infant sleep

### 2.1 Two-process model (Borbély; applied to children by Jenni & LeBourgeois)
- **Process S (homeostatic)** works like an hourglass: pressure rises while awake and falls during sleep. Slow-wave (delta) EEG activity is its marker.
- **Process C (circadian)** works like a clock: a ~24 h rhythm of alertness/sleepiness set by the suprachiasmatic nucleus and entrained mainly by light.
- In infants, **sleep pressure builds very fast**. That's why they can only stay awake for short stretches and need many naps. As they mature, pressure builds more slowly, so wake bouts get longer and naps drop (3→2 naps around the end of year 1; 2→1 in year 2). A 2022 PNAS paper argues that **nap transitions are driven mainly by homeostatic maturation**, not by caregiver schedules or light.
- Mathematical work (Skeldon & Dijk, Univ. of Surrey, *npj Biological Timing and Sleep*, 2025) shows the two-process model plus a light-driven clock can reproduce sleep patterns across species and **early childhood**, including why naps come and go, and that bright evening light pushes sleep later.

**App implication:** model `pressure(t)` rising exponentially during wake and decaying during sleep, plus an age-dependent circadian curve. This is a principled and explainable predictor (see §6).

### 2.2 How the circadian rhythm develops (0–6 months)

| Age (term baby) | What happens | Source |
|---|---|---|
| Birth–~6 wk | Sleep spread fairly evenly across day and night; ultradian cycles of ~3–4 h driven by feeding | J Physiol Anthropol review 2022 |
| ~3–5 wk | Rest-activity rhythm detectable on actigraphy; a "free-running" ~25 h rhythm can appear before locking to 24 h | Nishihara; review |
| **~8 wk** | **Cortisol day/night rhythm appears first** | Joseph et al., *Arch Dis Child Fetal Neonatal* 2015 (Leicester, n=35) |
| **~9 wk** | **Melatonin (6-sulfatoxymelatonin) rhythm appears**, at the same time as night sleep becomes established | Same |
| ~10 wk | Core body temperature drops at night | Same |
| ~8–12 wk | Mother's rhythm acts as an important entrainment signal (Kono 2003: circadian peak in all infants by week 8) | Hiroshima actigraphy studies |
| ~15 wk | More consolidated wake and sleep episodes | Review |
| 6–9 mo | Most infants able to sleep through the night | Review |

The sequence (cortisol → melatonin + night sleep → temperature) is consistent; exact ages come from small cohorts and vary a lot between babies. **At ~3 months our son is right in this transition**, which makes this a great time to support the clock with light, darkness and routine.

### 2.3 Light: the main time cue
- **Daytime light helps.** Harrison (2004, *J Sleep Res*, n=56): at 6, 9 and 12 weeks, good night sleepers had **~2× more light exposure between 12:00 and 16:00** than poor sleepers. There was no difference for morning or evening light. (Observational; association, not proof.)
- **Evening light hurts.** Akacem, Wright & LeBourgeois (2018): in preschoolers, 1 h of bright light (1000 lux) before bed suppressed melatonin by **~88%**, and it stayed suppressed for 50+ min after the light went off. Hartstein et al. (2022, *J Pineal Res*): **even 20–40 lux suppressed melatonin ~70%** in young children. Children are more sensitive than adults (larger pupils, clearer lenses). No equivalent data exists in infants yet, but the direction is clear.
- **Color.** Melanopsin (the clock's photoreceptor) is most sensitive around **480 nm (blue)**. Long wavelengths, **red (620–700 nm) and to a lesser extent amber (~590 nm)**, are much less disruptive. No infant trial compares red vs amber directly. Practical consensus: **dim, warm/red light for night feeds and changes; intensity matters as much as color.** (This answers Tatá's question about yellow vs red light: red is slightly better in theory, and dimmer beats either.)
- A 2024 scoping review in *Eur J Pediatr* confirms light is a key zeitgeber in infancy and calls for more research on critical periods.

### 2.4 Feeding and chrononutrition
- Breast milk is time-stamped: **melatonin peaks around midnight and is essentially absent in daytime milk; cortisol peaks early morning** (*Frontiers in Nutrition* 2025; a smaller study found infant salivary cortisol mirrors milk levels).
- Hypothesis: giving expressed milk at a different time of day than it was pumped may give mixed signals. Researchers suggest **labeling pumped milk with the time it was expressed** and feeding it at a similar time. This is untested in trials but costs nothing.
- **App implication:** if we log pumping, store the pump time and show a "day milk / night milk" tag.

### 2.5 Sleep architecture and the "4-month regression"
- Popular claim: around 3–5 months, sleep moves from newborn 2-stage (active/quiet) to adult-like cycles (N1–N3 + REM), causing more night wakings for 2–6 weeks.
- Evidence: the architecture maturation itself is well known in sleep physiology, but **"the 4-month regression" as a defined event is not well documented** in peer-reviewed infant studies. It's mostly parenting-blog lore.
- **App implication:** don't hard-code "regressions". Instead, **detect actual changes** in our data (e.g., night wakings ↑ over 7 days vs the previous 14) and show them neutrally.

---

## 3. What's "normal": reference numbers

| Metric | Value | Source |
|---|---|---|
| Total sleep, infants 0–12 mo | **12.8 h** (range 9.7–15.9) per 24 h | Galland et al. 2012, *Sleep Med Rev* (systematic review, 34 studies) |
| Total sleep, 4–12 mo (recommended) | **12–16 h incl. naps** | AASM consensus, Paruthi et al. 2016, *J Clin Sleep Med* (AAP-endorsed) |
| <4 months | No recommendation (variation too wide) | AASM 2016 |
| Night wakings | Decrease through age 2 | Galland 2012 |
| Daytime naps | Decrease with age | Galland 2012 |
| Sleeping 5 h straight at 12 mo | ~87% | Henderson et al. 2010, *Pediatrics* (n=75) |
| Sleeping 8 h straight at 12 mo | ~86% | Henderson 2010 |
| Sleeping 22:00–06:00 at 12 mo | ~73% | Henderson 2010 |
| Biggest gain in self-regulated sleep | Between 1 and 4 months | Henderson 2010 |

**Typical heuristic tables (NOT validated, use only as priors):**

| Age | Wake window (consultant heuristic) | Naps/day |
|---|---|---|
| 0–6 wk | 35–60 min | 4–8 (unstructured) |
| 6–12 wk | 60–90 min | 4–5 |
| 3–4 mo | 75–120 min | 4 |
| 5–7 mo | 2–3 h | 3 |
| 7–10 mo | 2.5–3.5 h | 2–3 |
| 10–14 mo | 3–4 h | 2 |
| 14–24 mo | 4–6 h | 1 |

Sources for these tables (Napper, Hatch, Kyte, etc.) themselves say there's no evidence-based basis for exact numbers. Typically, the **last wake window before bedtime is the longest**, and the first one of the morning is the shortest.

---

## 4. Behavioral evidence (routines and settling)

- **Mindell et al. 2006, *SLEEP*** (AASM task force, 52 studies): **94% of studies** found behavioral interventions produced clinically significant improvements in bedtime problems/night wakings; ~82% of children benefited, with effects holding 3–6 months. Strongest evidence: extinction approaches and **preventive parent education** (teaching about sleep before problems start).
- Moore, Meltzer & Mindell 2008: empirically supported approaches are extinction, graduated extinction, **positive bedtime routines**, and parent education.
- A **consistent bedtime routine** (same short sequence, same order: e.g., bath → massage → feed → song → bed) is the lowest-effort, most broadly recommended practice. Tatá started a nap + night routine on 31 Oct, so the app can track adherence and look for correlations.
- Formal sleep training (extinction-type methods) is generally not recommended before ~6 months.

## 5. Safe sleep (AAP 2022, Moon et al., *Pediatrics*)
- **Back to sleep for every sleep** until 1 year; side sleeping is not safe.
- **Firm, flat, non-inclined surface** (inclines >10° are unsafe); crib/bassinet with **no pillows, blankets, bumpers or toys**.
- Room-sharing without bed-sharing; bed-sharing risks are detailed in the statement.
- Human-milk feeding is protective (dose-dependent).
- **Do not use commercial devices or home cardiorespiratory monitors to reduce SIDS risk.**
- Swaddling isn't a SIDS-prevention strategy; if swaddled, always on the back, and stop when baby shows signs of rolling.
- **App implication:** a gentle optional safe-sleep checklist and **no claims of monitoring safety**.

---

## 6. Existing apps: market scan

| App | What it does well | Prediction approach | Price / model | Notes |
|---|---|---|---|---|
| **Napper** | Beautiful, simple; daily timeline; sleep sounds; multi-caregiver; trends | "Data, science and algorithmic magic": age-based wake windows personalized from logged history. Needs **~1 week** of data; reviewers report mixed accuracy over time | Freemium; ~US$50/yr, "Complete" up to ~US$130/yr | Closest model for Cochilo |
| **Huckleberry** | Most popular; **SweetSpot®** predicts next nap/bedtime (2+ months); feeding/diaper/growth logs; paid sleep-coach plans | SweetSpot = wake windows + baby's history (proprietary) | Free basic; SweetSpot behind Plus/Premium | Common complaint: predictions paywalled |
| **Baby Buddy** (open source) | Self-hosted web app + API; sleep, feeds, diapers, tummy time, growth; Home Assistant integration | No predictions | Free | Good reference for data model/API |
| Others (Hatch, Owlet, Nanit, BabyTracker, Glow Baby) | Hardware-tied or general trackers | Mostly logging and charts | Varies | Owlet/Nanit = monitoring hardware; AAP says not for SIDS prevention |

**Evidence on apps:** small studies suggest infant sleep apps can **reduce parental stress/fatigue** (SleepWellBaby, *JMIR Pediatrics & Parenting* 2026; Warwick psychoeducational app RCT, lower parenting stress), but effects on infant sleep itself are inconsistent. That's a useful design target: **reduce cognitive load and anxiety for us**, not chase perfection.

### Gaps we can fill for personal use
- **Transparent predictions**: show *why* the next nap is suggested ("last wake window 1h40; his median at this time of day over the last 7 days is 1h35").
- **No paywall, no account, local-first data**, shared between two phones.
- **Circadian-aware features**: daylight exposure logging, evening light reminders, day/night milk tagging.
- Detect real changes instead of generic "regression" labels.
- One-tap logging (widget / lock-screen / quick-settings tile) during night feeds, using a dark red UI.

---

## 7. Proposed Cochilo design (first draft)

### 7.1 Core features (MVP)
1. **One-tap sleep timer** (start/stop), with editable start/end and "fell asleep at / woke up at" corrections. Home-screen widget and a Quick Settings tile.
2. **Event log**: sleep (nap/night), feed (breast L/R, bottle, pump with timestamp), diaper, bedtime routine done, outdoor/daylight time, notes.
3. **Daily timeline** (24 h ring or horizontal bar, Napper-style), plus a **7/14-day actogram** (stacked days) that makes the circadian rhythm emerging visible.
4. **Next-nap / bedtime prediction** with a confidence window and an explanation.
5. **Stats**: total sleep/24 h, day vs night split, longest night stretch, number of night wakings, nap count, average wake windows by slot (1st, 2nd… last), all compared with the reference ranges in §3.
6. **Night mode**: dark red UI, minimal brightness, big buttons.
7. **Two caregivers sync** (me and Tatá).

### 7.2 Prediction algorithm (phased)
**Phase 1: personalized wake windows (like Napper/Huckleberry)**
- Priors: age-based wake-window table (§3), indexed by slot (1st WW of the day … last WW before bed).
- Personalize: for each slot, take the **weighted median of the last 7–14 days** of our baby's actual wake windows (exponential decay weighting, half-life ~3 days), and only include naps that "worked" (lasted >30 min, or fell asleep fast if we log that).
- Blend with the prior: `ww = (n·personal + k·prior) / (n + k)`, with k≈5 so the prior fades after about a week of data, matching the "1 week to get good" behavior of commercial apps.
- Bedtime: weighted median of recent bedtimes, constrained by last-WW length and target night duration.

**Phase 2: two-process model (more principled)**
- *Shipped in 0.24.0 (process S only)*: the last nap's length moves the next window, learned per baby; see [prediction.md](prediction.md). Process C (the fitted clock) is still to come.
- `S(t)`: during wake `S = 1 − (1 − S₀)·e^(−t/τw)`; during sleep `S = S₀·e^(−t/τs)`.
- `C(t)`: sinusoid (or skewed curve) with amplitude increasing with age (≈0 before 6 weeks, growing through 4–6 months), phase fitted to our data.
- Sleep is likely when `S(t) > H⁺(t) = H₀ + a·C(t)`; waking when `S < H⁻(t)`.
- Fit `τw, τs, H₀, a, phase` per baby with simple least squares over the last 14 days of logs; refit nightly.
- Nice side effect: we can **plot sleep pressure live** and estimate circadian amplitude/phase as he grows.

**Phase 3 (optional):** small gradient-boosted model on features (time of day, previous nap length, daylight minutes, routine done, feed timing). Only worth it once we have months of data.

### 7.3 Circadian-support features
- **Daylight tracker**: log outdoor/daylight minutes (manual, or later the phone's ambient-light sensor while a "daylight walk" timer runs). Remind about **light around 12:00–16:00** (Harrison 2004).
- **Evening wind-down reminder**: ~60 min before predicted bedtime: "dim the lights, warm/red light only".
- **Pumped-milk time tags**: day vs night milk.
- **Actogram** to see the day/night pattern consolidating week over week.

### 7.4 Tech suggestions (personal use)
- **Android-first** (Kotlin + Jetpack Compose) for Pixel phones, matching existing personal apps and the in-house UI kit (`br.gabriel.ui`). Alternative: a PWA if Tatá's phone is iOS.
- **Local-first storage** (Room/SQLite) with simple sync between two phones: options include a tiny self-hosted backend on the home server, Firebase/Supabase, or a Baby Buddy-compatible API.
- Widgets (Glance), Quick Settings tile, optional Wear OS tile for one-tap logging.
- Export to CSV/JSON; optional Health Connect (sleep sessions) is not designed for infants, so keep data in-app.

### 7.5 Data model sketch
```
Baby(id, name, birthDate, dueDate?)          // dueDate → corrected age if premature
SleepSession(id, babyId, start, end?, type[nap|night], location?, fellAsleepMethod?, quality?, notes)
Feed(id, babyId, start, end?, kind[breastL|breastR|bottle|solid], amountMl?, milkExpressedAt?)
Pump(id, start, end?, amountMl, expressedAt)
Diaper(id, babyId, time, kind[wet|dirty|both])
Routine(id, babyId, time, kind[nap|bedtime], steps[], completed)
Daylight(id, babyId, start, end, outdoor:boolean, luxAvg?)
Note(id, babyId, time, text, tags[])
```
Use **corrected age** (from due date) for priors if the baby was premature; circadian milestones follow post-conceptional age.

---

## 8. Decisions (2026-11-01)

1. **Android only** (Kotlin + Jetpack Compose).
2. **Local only**: no network, no account; JSON backup export/import (merge, never deletes) and CSV export.
3. **Track everything**: sleep, feeds, pumping, diapers, daylight, routines, notes.
4. **English and Brazilian Portuguese**, following the phone's per-app language.
5. **UI on the Gabriel Design System** (new `cochilo` layer). Prediction ships as phase 1
   (personalized wake windows); the two-process model (phase 2) comes later.

Still open: a dim night mode (the design system's divergence D-11), home-screen widget and
Quick Settings tile, daylight and wind-down reminders.

## 9. References

### Scientific
- Galland BC, Taylor BJ, Elder DE, Herbison P. *Normal sleep patterns in infants and children: a systematic review of observational studies.* Sleep Med Rev. 2012;16(3):213–222. https://pubmed.ncbi.nlm.nih.gov/21784676/
- Paruthi S, et al. *Consensus statement of the AASM on the recommended amount of sleep for healthy children.* J Clin Sleep Med. 2016;12(11):1549–1561. https://aasm.org/resources/pdf/pediatricsleepdurationmethods.pdf · https://depts.washington.edu/dbpeds/pediatricsleepdurationconsensus.pdf
- Joseph D, et al. *Getting rhythm: how do babies do it?* Arch Dis Child Fetal Neonatal Ed. 2015;100(1):F50–F54. https://pubmed.ncbi.nlm.nih.gov/25245173/ · https://fn.bmj.com/content/100/1/F50
- *Development of circadian rhythms in infants* (review). J Physiol Anthropol. 2022. https://pmc.ncbi.nlm.nih.gov/articles/PMC9109407/ · https://link.springer.com/doi/10.1186/s40101-022-00294-0
- Light exposure in infancy, scoping review. Eur J Pediatr. 2024. https://link.springer.com/10.1007/s00431-024-05951-3
- Harrison Y. *The relationship between daytime exposure to light and night-time sleep in 6–12-week-old infants.* J Sleep Res. 2004;13(4):345–352. https://healthdesign.org/knowledge-repository/relationship-between-daytime-exposure-light-and-night-time-sleep-6-12-week-old
- Akacem LD, Wright KP, LeBourgeois MK. *Sensitivity of the circadian system to evening bright light in preschool-age children.* Physiol Rep. 2018. https://pmc.ncbi.nlm.nih.gov/articles/PMC5835497
- Hartstein LE, et al. *High sensitivity of melatonin suppression response to evening light in preschool-aged children.* J Pineal Res. 2022. https://sciencedaily.com/releases/2022/01/220127104208.htm · https://www.ajmc.com/view/any-light-exposure-before-bedtime-may-lead-to-sleep-troubles-in-young-children
- Jenni OG, LeBourgeois MK. *Understanding sleep–wake behavior and sleep disorders in children: the value of a model.* Curr Opin Psychiatry. 2006. https://pmc.ncbi.nlm.nih.gov/articles/PMC2980811
- *Nap transitions and homeostatic sleep pressure* (PNAS 2022). https://pmc.ncbi.nlm.nih.gov/articles/PMC9636905/
- Skeldon AC, Dijk DJ. *The complexity and commonness of the two-process model of sleep regulation from a mathematical perspective.* npj Biol Timing Sleep. 2025. https://www.surrey.ac.uk/news/maths-behind-early-morning-awakenings-effects-evening-light-and-naps-babies
- Henderson JMT, France KG, Owens JL, Blampied NM. *Sleeping through the night: the consolidation of self-regulated sleep across the first year of life.* Pediatrics. 2010;126(5):e1081. https://contemporarypediatrics.com/view/more-half-infants-sleep-through-night-age-5-months
- Mindell JA, et al. *Behavioral treatment of bedtime problems and night wakings in infants and young children.* SLEEP. 2006;29(10):1263–1276. https://pubmed.ncbi.nlm.nih.gov/17068979/
- Moon RY, Carlin RF, Hand I. *Sleep-related infant deaths: updated 2022 recommendations.* Pediatrics. 2022. https://www.aap.org/en/news-room/news-releases/aap/2022/american-academy-of-pediatrics-updates-safe-sleep-recommendations-back-is-best · https://healthychildren.org/English/news/Pages/safe-sleep-back-is-best-avoid-soft-bedding-inclined-surfaces-and-bedsharing.aspx
- Kono et al. / Nishihara et al., infant–mother actigraphy (Hiroshima). https://ir.lib.hiroshima-u.ac.jp/00000162?locale=en · https://pmc.ncbi.nlm.nih.gov/articles/PMC4955618
- Breast milk circadian variation (Frontiers in Nutrition, 2025). https://www.frontiersin.org/news/2025/09/05/storing-breast-milk-support-baby-circadian-rhythm · https://neurosciencenews.com/breast-milk-circadian-rhythm-29667/
- SleepWellBaby app evaluation. JMIR Pediatr Parent. 2026. https://pediatrics.jmir.org/2026/1/e88948/PDF
- Psychoeducational app for parents of infants (Warwick). https://wrap.warwick.ac.uk/174396/
- Mindell et al., parental concerns via a mobile app (Penn State). https://pure.psu.edu/en/publications/parental-concerns-about-infant-and-toddler-sleep-assessed-by-a-mo/

### Apps and industry
- Napper: https://apps.apple.com/app/id1491340863 · https://play.google.com/store/apps/details?id=com.napper · review: https://www.techradar.com/computing/websites-apps/napper · wake windows article: https://doulores.napper.app/articles/wake-windows
- Huckleberry: https://huckleberrycare.com/blog/what-color-light-helps-baby-sleep · https://huckleberrycare.com/blog/huckleberry-sleep-study-hcdc
- Baby Buddy (open source): https://docs.baby-buddy.net · https://selfhostyourself.com/services/baby-buddy
- Wake windows heuristics (commercial): https://www.hatch.co/blog/how-to-measure-awake-windows · https://kytebaby.com/blogs/news/optimal-sleep-and-bedtime-windows-by-age

### Evidence-quality notes
- Circadian milestone ages come mostly from **small cohorts** (n≈35–75). Treat them as approximate.
- Light/melatonin studies in children are in **preschoolers**, not infants.
- Wake-window tables and "sleep regressions" are **practitioner heuristics** with no peer-reviewed validation.
- Several figures above come from abstracts or secondary coverage. Check the original papers before relying on exact numbers.
