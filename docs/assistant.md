# Ask Cochilo: a local assistant for the baby's sleep

The owner's request (2026-11-03): a local AI that analyses the sleep pattern and answers the
mother's questions, smart enough to suggest, answer and guide; never medical advice, never
judging; running on any Android and, later, on iPhone.

## The shape: three layers, and only the last one is a language model

| Layer | What it does | Where it runs | Status |
|---|---|---|---|
| **1. Facts** | Computes everything with numbers from her log: naps, short-nap share, night wakings, longest stretch, bedtime and its spread, early mornings, daylight, contact naps, routines, the next sleep | Pure Kotlin on the phone (`domain/assistant/Assistant.kt`), reusing `Stats`, `Predictor`, `Reference` | **Shipped, 0.29.0** |
| **2. Guide** | Understands the question, routes health, medicine and the mother's wellbeing to a person, and answers from a reviewed library tied to [research.md](research.md), filled with layer 1's numbers | Pure Kotlin + reviewed strings in English and Portuguese | **Shipped, 0.29.0** |
| **3. Conversation** | An optional small on-device model for questions outside the library and a more natural voice. It is only allowed to phrase layer 1's facts and layer 2's reviewed content, never to invent numbers or advice | LiteRT-LM (Google AI Edge) with a Gemma model, on Android and iOS | **Planned, needs a decision (§5)** |

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

**What it understands (13 topics):** the next sleep, short naps, night wakings, early mornings,
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

## Phase 2: the on-device model (to decide)

**Engine:** [LiteRT-LM](https://ai.google.dev/edge/litert-lm/overview), Google's open-source
on-device LLM runtime, on Android (CPU/GPU/NPU) and iOS (CPU/GPU, a Swift SDK in early preview).
It's the one runtime that covers both platforms with the same model files.

**Model:** the smallest chat-ready Gemma that answers well in Portuguese. Gemma 3 1B (4-bit,
roughly 0.5–0.6 GB) is the realistic floor for "any Android with 4 GB of RAM". Gemma 3n E2B
(about 3 GB) is better but too large for older phones. The 270M models are for embeddings and
function calling, not chat. To check on real phones before choosing: speed on a 4 GB mid-range
Android, battery use, and quality in pt-BR.

**How it stays safe:**
- Layer 2 runs first; health, medicine and the mother still get the fixed replies, and the
  model never sees those questions.
- The prompt holds only layer 1's facts and the matching reviewed passages, and the model is told
  to answer only from them.
- Every reply is checked against the never-judge words and against numbers that aren't in the
  facts; one that fails falls back to the library answer.
- Always labelled "Not medical advice"; it never says it monitors the baby.

**The decision it needs (the owner's):** the model file is hundreds of MB and has to reach the
phone.

| Option | Play build (no INTERNET) | Family build (github) | Trade-off |
|---|---|---|---|
| **A. Play Asset Delivery** (an on-demand asset pack, downloaded by the Play Store) | Works, if the app itself needs no INTERNET for it (to verify) | n/a | Cleanest for Play; ties the model to Play |
| **B. Download from a GitHub release** | Not possible | Needs INTERNET for one more thing than the self-update | Changes a product rule |
| **C. Import the model file** (like a backup: the parent picks the file) | Works | Works | No rule changes; one more step for the parent |
| **D. Ship it inside the app** | Works | Works | The app grows from MBs to over 0.5 GB |

Recommendation: **A for Play, C for the family build**, with the model off by default and Phase 1
always there. Until that's decided, Phase 1 is the assistant.

## iPhone

The engine (`domain/assistant/`) has no Android imports on purpose. For an iPhone release it
moves, with `Predictor`, `Stats` and the other domain rules, into a Kotlin Multiplatform shared
module. The answer strings move to Compose Multiplatform resources (or a shared catalog). LiteRT-LM's
iOS runtime runs the same model files for phase 2.

## Files

- `domain/assistant/Assistant.kt`: facts, question matching, safety routing, suggestions.
- `ui/screens/Ask.kt`: the screen, the answer builder (`answer`, `insights`, testable).
- Strings `ask_*` in `values/strings.xml` and `values-pt-rBR/strings.xml`: the reviewed library.
- Tests: `AssistantTest` (safety, both languages, word-boundary matching, facts, suggestions,
  never-judge). Screenshots: `ask_light`, `ask_care_ptBR_dark`, `ask_not_sure_light`.
