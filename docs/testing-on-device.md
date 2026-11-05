# Testing on real devices (0.56.0)

Paparazzi checks every screen's pixels, and unit tests check every rule. What they can't check
is how a thing behaves on a phone, a watch or the home Wi-Fi. This is the plan for that: what to
try, how, and what counts as a pass. ux-user-flows' check 15 asks for exactly this: the riskiest
assumptions named, with a test planned for each.

Mark each one ✅ with the date and the phones when it passes, or open an issue when it doesn't.

## The assumptions, riskiest first

| # | Assumption | Why it's risky | Built in |
|---|---|---|---|
| 1 | Sync with the app closed keeps both phones in step overnight without draining the battery | Android may stop a foreground service; NSD can stop answering | 0.52.0 |
| 2 | The watch tile's tap reaches the phone and comes back in under 2 seconds | Bluetooth links sleep; the Message API needs the two apps signed alike | 0.53.0 |
| 3 | LiteRT-LM answers on a 4 GB phone in about the same time MediaPipe did (10 s or less), without crashing | A new runtime; the token count is an estimate | 0.55.0, 0.56.0 |
| 4 | Gemma 3n runs on an 8 GB phone: downloads, joins, passes its check, answers | 3.6 GB; two parts; memory | 0.55.0 |
| 5 | The old `.task` model is offered for update and replaced cleanly | A migration with no undo | 0.56.0 |
| 6 | Dragging a sleep's start (and end) feels right one-handed | Paparazzi can't feel a drag | 0.53.0, 0.55.1 |
| 7 | TalkBack reads every new screen in a sensible order | Paparazzi checks pixels, not order (roadmap §4) | all |
| 8 | An interrupted model download carries on from the last finished piece, and the result passes its check | The download manager's behaviour differs by phone maker; a restart mid-piece | 0.57.0 |

## How, and what passes

1. **Sync overnight.**
   - Setup: two phones, paired, "Keep in sync when closed" on, both apps closed, on Wi-Fi from
     21:00 to 07:00.
   - Each logs a sleep and a feed, minutes apart.
   - **Pass:** each entry is on the other phone within a minute. In the morning, each phone's
     battery shows Cochilo under 3% overnight.
2. **The watch tile.**
   - Setup: a Wear OS watch paired to the phone. Install `cochilo-wear-<v>.apk`; add the tile.
   - Tap Start sleep, Woke up, Left, Finish.
   - **Pass:** each shows on the tile at once, and on the phone's Today within 2 s.
   - With the phone's Bluetooth off, the tile says "Open Cochilo on the phone, nearby." and logs
     nothing.
3. **LiteRT-LM on 4 GB.**
   - Setup: a 4 GB phone with the 1B `.litertlm`.
   - Ask 10 questions the list can't answer.
   - **Pass:** every answer in 10 s or less, and no crash.
   - If the app does close, the next start says the model is paused (0.39.1) and doesn't crash again.
4. **Gemma 3n on 8 GB.**
   - Setup: an 8 GB phone. The sheet says "about 3.5 GB"; Download.
   - **Pass:** one progress bar runs to the end, the model is Ready, and it answers the same 10
     questions in 15 s or less.
5. **The old model.**
   - Setup: a phone with the 0.54.0 `.task` model; update to 0.56.0.
   - **Pass:** the Settings row says "A newer model is ready. Tap to get it." One tap downloads
     the 1B `.litertlm`, the old file is gone, and Ask answers.
6. **Dragging.** One-handed, in bed, at night: move five sleeps' starts and ends by 5 to 30 minutes.
   - **Pass:** each lands where intended on the first try, and Undo brings it back.
7. **TalkBack.** Walk Today, Day (with sleep pressure), Settings › Sync and the On-device AI sheet.
   - **Pass:** every control is announced with its name and state, in reading order, and every
     chart reads its summary.

8. **Carrying on.**
   - Setup: any phone; Download the model on Wi-Fi.
   - At about 30%, turn the phone off and on. At about 60%, open Android's Downloads and remove
     the Cochilo download.
   - **Pass:** each time the sheet says "stopped at N%" with N no more than one piece (100 MB)
     below where it was. Carry on downloading continues from there, the model is Ready, and Ask
     answers.

## Results

| # | Date | Devices | Result |
|---|---|---|---|
| 1–8 | — | — | ⬜ not yet run |
