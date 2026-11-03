# Ask Cochilo: a local assistant for the baby's sleep

The owner's request (2026-11-03): a local AI that analyses the sleep pattern and answers the
mother's questions, smart enough to suggest, answer and guide; never medical advice, never
judging; running on any Android and, later, on iPhone.

## The shape: three layers, and only the last one is a language model

| Layer | What it does | Where it runs | Status |
|---|---|---|---|
| **1. Facts** | Computes everything with numbers from her log: naps, short-nap share, night wakings, longest stretch, bedtime and its spread, early mornings, daylight, contact naps, routines, the next sleep | Pure Kotlin on the phone (`domain/assistant/Assistant.kt`), reusing `Stats`, `Predictor`, `Reference` | **Shipped, 0.29.0** |
| **2. Guide** | Understands the question, routes health, medicine and the mother's wellbeing to a person, and answers from a reviewed library tied to [research.md](research.md), filled with layer 1's numbers | Pure Kotlin + reviewed strings in English and Portuguese | **Shipped, 0.29.0** |
| **3. Conversation** | An optional small on-device model for questions outside the library. It is only allowed to phrase layer 1's facts and layer 2's reviewed content, never to invent numbers or advice | MediaPipe LLM Inference with a small Gemma, on Android (and iOS later) | **Shipped, 0.30.0** |

Why this order: layers 1 and 2 are what make it **safe and honest**. The numbers are computed,
not generated, so they can't be hallucinated. Every answer can be reviewed by Tatá before it
ships. Safety questions get fixed replies before any matching. And it runs on every phone,
however old, with no download. The model in layer 3 only makes it more fluent; it is never
the source of truth.

## Phase 1 (0.29.0): what ships

**Where:** Patterns › "Questions about sleep" (one tap) opens Ask Cochilo, a pushed screen like
Settings. It isn't on Today: Today is for logging with a baby in arms.

**The screen:**
1. One line: "Answers from your log and from sleep research, made on this phone. Not medical
   advice."
2. **This week**: four plain lines from her log (sleep a day against the age range, naps a day
   and their median, the longest stretch, daylight).
3. **The conversation**: her question in a blush bubble. The answer card has up to four parts:
   - *From your log*: her numbers;
   - what research says;
   - *What can help*: gentle, optional suggestions;
   - the source (a research.md section, AAP 2022, the UK Sling Consortium).
4. **You could ask**: up to four suggested questions as chips (one tap each). The rules pick
   first what stands out in her log: short naps, early mornings, little daylight, the 4-month
   change, mostly contact naps.
5. A field and **Ask** for anything else.

Nothing is stored: the conversation lives only while the screen is open, and it never leaves the
phone (no permission added; the play build still has no INTERNET).

**What it understands (14 topics):** the next sleep, short naps, how long naps last (0.38.0), night wakings, early mornings,
bedtime, wake windows, "is it enough sleep", contact naps, daylight, routines, the 4-month change,
falling asleep feeding, and safe sleep. Questions are matched in English and Portuguese,
accent- and case-blind. A key word must start a word, and short keys must match a whole word, so
"colour" is never "colo".

**Safety first, before any topic:**

| The question is about | The reply (fixed, reviewed) |
|---|---|
| The baby's health: fever, breathing, blue lips, seizures, vomiting, not feeding, a fall, weight, rash, reflux, SIDS… | "That's a question for the paediatrician, who can see the baby. If the baby seems unwell, has trouble breathing, or you're worried right now, call SAMU 192 or go to the emergency room." |
| Medicines, supplements, melatonin, doses | "Cochilo doesn't advise on medicines, supplements or doses. The paediatrician or a pharmacist can." |
| The mother: sad, anxious, overwhelmed, "não aguento", hurting herself | Kind words, then CVV 188 (free, any hour) and SAMU 192 |

**Never judge, by test:** `AssistantTest.the_answers_never_judge` scans every `ask_` string in
both languages for words that scold, blame or call a phase a step back ("should", "wrong",
"bad habit", "regression", "deveria", "errado", "ruim", "regressão", "culpa", "mania", "vício",
"mimado"…). The copy is also neutral about the baby's sex ("the baby", "o bebê").

**When it doesn't know**, it says so and lists what it can help with, with the suggested
questions under it. It never guesses outside its library.

## Phase 2: the on-device model (0.30.0)

Decided 2026-11-03 by the owner: **Play delivery for the play build, file import for the family
build**. Optional and off until the parent gets the model; phase 1 is always there.

**Runtime:** MediaPipe LLM Inference (`com.google.mediapipe:tasks-genai`), the same `.task` model
files on Android and iOS, with no permissions of its own. It sits behind `ai/LocalModel.kt`, so it
can be swapped for [LiteRT-LM](https://ai.google.dev/edge/litert-lm/overview) (Google's newer
runtime) in one class. LiteRT-LM's Android library is built with Kotlin 2.2+, which this project's
Kotlin 2.0 can't read yet; that swap follows a Kotlin upgrade (roadmap).

**Model:** a small Gemma as a `.task` file (Gemma 3 1B, 4-bit, about 550 MB). The model is never in
git and never inside the APK.

| | **play** | **github** (family) |
|---|---|---|
| How the model arrives | An on-demand **Play asset pack** (`:assistant_model`): the Play Store downloads it; the app only asks for it | Since 0.37.0 the **app downloads it** with one tap (Settings › On-device AI › Download, or from Ask under a reply it couldn't give), through Android's download manager: Wi-Fi only unless she taps "Download now on mobile data", in the background, with the system's progress notification, resuming after a dropped connection. "Choose a file instead" still copies a file already on the phone |
| Where it comes from | The Release workflow takes it from the `assistant-model-v1` pre-release (checked against its `.sha256`), or from the `ASSISTANT_MODEL_URL` secret; without either the pack ships empty and the app says "Not in this build" | This repository's `assistant-model-v1` **pre-release** (`gemma3-1b-it-int4.task` and its `.sha256`), put there once by the **Publish assistant model** workflow from Hugging Face (`litert-community/Gemma3-1B-IT`, which is gated: the workflow needs an `HF_TOKEN` from an account that accepted Gemma's terms). A pre-release, so self-update never offers it. Until it's published, Download says "The model isn't ready to download yet" |
| Checks | The pack's file must exist | A download must not be text (an error page), be at least 50 MB, and match the published SHA-256 (`ai/ModelDownload.kt`, `ModelDownloadTest`); a chosen file must not be text, be at least 50 MB and fit in free space (`ai/ModelFile.kt`). Google's current Gemma `.task` isn't a zip, so the container isn't checked (0.37.1); the runtime refuses what it can't load |
| Remove | Remove the model (the pack is removed) | Remove the model, or Stop the download (the file is deleted) |
| Licence | Gemma Terms of Use, named in the sheet | The same; `GEMMA-NOTICE.txt` sits beside the file on the pre-release |

**What the model may answer:** only questions the library doesn't know ("not sure"). Health,
medicine and the mother still get the fixed replies, and the model never sees them. The 14 topics
keep their reviewed answers.

**How it stays grounded** (`domain/assistant/Grounding.kt`, tested in `GroundingTest`):
- The prompt is the rules, this week's facts (the "This week" lines) and the reviewed answers of
  the topics that stand out (plus safe sleep), then the question. Rules: answer in at most 4 short
  sentences, only from those, no numbers or studies of its own, no medical advice, medicines or
  diagnoses, never judge, and say "I can't answer that from your log" when they don't answer it.
- Every reply is checked before it's shown. It's thrown away if it's empty, longer than 700
  characters, judges ("should", "wrong", "regression"…), mentions medicine or diagnosis, or
  contains **any number that isn't in the prompt**. A thrown-away reply shows the library's
  "not sure" answer instead.
- A model answer is always labelled: "Written on this phone by a small AI model, from your log.
  It can make mistakes."

**Permissions** (enforced by `check<Variant>Permissions`): the play build gains Play Asset
Delivery's foreground extraction service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`)
and WorkManager's `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` and `ACCESS_NETWORK_STATE` (read-only:
whether a network is up). **Still no `INTERNET`**: the Play Store downloads the pack, and the app
can't reach the network. The github build gains nothing: its download (0.37.0) uses the `INTERNET`
it already had and the system's download manager, and the request carries only the app's name
and version.

**Size:** the runtime's native libraries add about 46 MB to the family APK (ARM 64- and 32-bit;
x86 left out). Play splits by ABI, so a Play install adds about half that.

## 0.38.0: it knows the log, and it's one tap away

The owner asked "o quão devem demorar as sonecas do Dante?" and got "I can help with…", which
was useless. Three causes, three fixes:

1. **The library didn't know the topic.** New topic `NAP_LENGTH`: words for "how long / last /
   duram / demorar / quanto tempo", counted only next to a nap word ("soneca", "nap"), so "how
   long awake" stays a wake-window question. Its answer gives her numbers: the median, shortest
   and longest nap this week, and how many naps were logged. Then what's usual for the age ("no
   right length; 30–45 min single cycles before 5–6 months, 30 min to 2 h all common"; from 6
   months, 1–2 h). It never names a target. Tested in `AssistantTest`.
2. **The model was told almost nothing.** It got four summary lines. Now `domain/assistant/Digest.kt`
   (tested in `DigestTest`) gives it the whole picture as plain lines:
   - the baby's name and age;
   - each of the last 7 days (sleep, naps, night, longest stretch, wakings, feeds, diapers,
     daylight);
   - today's sleeps with times, lengths and places;
   - this week's naps (median, shortest, longest, share under 45 min, contact naps), wake
     windows, bedtime, early mornings and routines;
   - the next-sleep guess and the age references (total sleep, wake windows, naps a day, nap
     length).

   Durations are written in minutes and as hours, so "65 min" and "1h 05m" both count.
3. **Good answers were thrown away in silence.** The number check now reads a number however
   it's written ("05" is "5", "9,7" is "9.7"). Most usual answers already passed, because their
   numbers are now in the facts. When a reply still fails, Ask says so ("The on-device AI wrote
   an answer I couldn't check against your log, so here is what I know instead") instead of
   pretending it had nothing. While the model is still downloading, a "not sure" reply says to
   ask again once it's ready.

   A known limit: a small number (3, 5, 7) is often somewhere in the facts, so the check can't
   catch every invented small number. The rules still forbid it, and replies are labelled.

**The entry point.** Ask is now in the header of every tab: the chat icon before Settings, one
tap from anywhere (design system 0.27.0). The row in Patterns stays.

## 0.39.1: asking no longer crashes the app

The owner: "when I make questions the app crashes." The 0.38.0 digest made the prompt much longer
than the model's window (1280 tokens, prompt and answer together). A prompt that overflows it
aborts inside MediaPipe's native code, which Kotlin can't catch, so the whole app closed. Fixes:

- **The prompt always fits.** The model counts the prompt's tokens (`sizeInTokens`).
  `Grounding.fit` keeps the fullest prompt within 1280 − 320 (room for the answer), dropping the
  digest's last lines first; tested in `GroundingTest`. The digest now runs most important first:
  - the name, age and now;
  - this week;
  - the age references;
  - today's sleeps;
  - then the 7 days one by one.
- **One answer at a time.** A lock around the engine: two questions asked quickly no longer run
  it twice at once, and Remove never closes it mid-answer.
- **CPU.** The engine runs on the CPU; the GPU path crashes natively on some phones.
- **No crash loop.** A flag is written (synchronously) before the model writes and cleared after.
  If it's still set at the next start, the model took the app down, so it's paused. Answers come
  from the library, and a "not sure" reply says so with "Try the on-device AI again".

## iPhone

The engine (`domain/assistant/`) has no Android imports on purpose. For an iPhone release it
moves, with `Predictor`, `Stats` and the other domain rules, into a Kotlin Multiplatform shared
module. The answer strings move to Compose Multiplatform resources (or a shared catalog). LiteRT-LM's
iOS runtime runs the same model files for phase 2.

## Files

- `domain/assistant/Assistant.kt`: facts, question matching, safety routing, suggestions.
- `ui/screens/Ask.kt`: the screen, the answer builder (`answer`, `insights`, testable).
- Strings `ask_*` in `values/strings.xml` and `values-pt-rBR/strings.xml`: the reviewed library.
- `domain/assistant/Grounding.kt`: the model's prompt and the reply check.
- `domain/assistant/Digest.kt`: what the model is told about this baby.
- `ai/LocalModel.kt` (the runtime), `ai/ModelState.kt` (states and the `ModelSource` interface),
  `ai/ModelFile.kt` (file checks and copy); `src/play/.../ai/ModelSources.kt` (Play asset pack) and
  `src/github/.../ai/ModelSources.kt` (the download, and file import); `ai/ModelDownload.kt` (where
  from, and the checksum); `ui/screens/ModelSheet.kt`; `.github/workflows/assistant-model.yml`
  (publishes the model once).
- `assistant_model/` (the asset pack module); the Release workflow's "Assistant model" step.
- Tests: `AssistantTest` (safety, both languages, word-boundary matching, facts, suggestions,
  never-judge, nap length), `GroundingTest`, `DigestTest`, `ModelFileTest`, `ModelDownloadTest`. Screenshots: `ask_light`, `ask_care_ptBR_dark`,
  `ask_not_sure_light`, `ask_generated_light`, `model_sheet_play_light`,
  `model_sheet_app_download_light`, `model_sheet_app_downloading_light`,
  `model_sheet_waiting_wifi_ptBR_light`, `model_sheet_ready_ptBR_dark`.
