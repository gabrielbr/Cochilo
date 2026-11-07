# Partner sync (0.47.0; closed and who logged it, 0.52.0)

Two parents, two phones, one log. The phones talk to each other directly on the home Wi-Fi. There
is no server, no account, and nothing on the internet. Only in the **github** (family) build: the
play build has no INTERNET permission, and Android needs it for any socket, even on a home
network ([distribution.md](distribution.md)).

## For the parents

1. **Pair, once.** On both phones, open Settings › Sync › **Pair a phone**. Each phone lists the
   other; on one, tap the other's name.
2. **Compare.** Both phones show the same six digits ("482 913"). If they match, tap **Same
   number** on both. If they don't, tap **Not the same**: someone else is on the network, and
   nothing is paired.
3. **Done.** From then on, while Cochilo is open on both phones and they're on the same Wi-Fi,
   every change goes across a few seconds after the last tap. Opening Cochilo catches up with
   whatever the other phone did meanwhile. "From Pixel da Ana: 3 new or changed" says when
   something arrived.

Settings › Sync shows who it's paired with and when they last synced, or "Not found on this
Wi-Fi. Open Cochilo on …". It also has **Sync now**, **Your initial on entries**, **Keep in sync
when closed** and **Unpair**. After Unpair, both phones keep the log they have.

**Keep in sync when closed** (0.52.0, off until turned on): the phones stay in step with Cochilo
closed, as long as both are on the Wi-Fi. It keeps Cochilo's notification on: the one that
already shows asleep or awake, so there's no second one, or "In sync with Pixel da Ana" when the
live notification is off. It's a foreground service: Android keeps it running, and it costs a
little battery. After the phone restarts, opening Cochilo once starts it again.

**Who logged it** (0.52.0, optional): set one letter in **Your initial on entries**. On the other
phone, what you log says "by G" at the end of its line. What you log yourself never shows a
letter, and with no initial nothing shows. An entry keeps who first logged it, even after the
other parent edits it. Entries from before the initial was set stay unmarked.

| Flow | Taps |
|---|---|
| Pair (once) | Settings › Pair a phone, on both (2 + 2); tap the other phone (1); Same number, on both (1 + 1) |
| A change goes to the other phone | 0 |
| Catch up | 0 (open Cochilo) |
| Sync now | Settings › Sync now (2) |
| Keep in sync when closed (once) | Settings › the switch (2) |
| Your initial (once, optional) | Settings › Your initial on entries (2), a letter, Save (1) |

## What syncs

Everything in the log, both ways:
- **New entries and edits:** the newest edit of an entry wins, as in an import.
- **Deletions too.** Since 0.47.0 a phone remembers what was deleted and when (the `deletions`
  table, Room 4 → 5). An entry deleted on one phone is deleted on the other, unless it was edited
  after the deletion (Undo, or the other parent editing it). So Undo still works across phones.
- **Who logged each entry** (0.52.0), when the parents set their initials. The sync message
  carries the sender's initial; the format stays version 1, so a phone on 0.47 still syncs.
- **The baby's profile** goes only to a phone that has none. Each phone keeps its own name
  spelling, look and fabric.

Not synced:
- settings (theme, fabric, reminders, notifications);
- Ask Cochilo's chats;
- the model;
- Tatá's suggestions seen.

Those belong to each phone.

## How it's safe

- **On the home network only.** The phones find each other with Android's network service
  discovery (mDNS, the way a phone finds a printer, `_cochilo._tcp`) and connect directly.
  Nothing goes to the internet.
- **Pairing, by comparing numbers.** Each phone makes a fresh P-256 key pair and the two agree a
  secret (ECDH). The six digits come from that secret and from everything both phones said
  (HKDF). Someone in the middle would make the two phones show different numbers, so "Same
  number" on both is the proof. It's the same idea as Bluetooth's numeric comparison. A second
  step proves both parents said yes (a MAC with the new key).
- **The pair key** (32 bytes, derived, never sent) stays in the app's private storage, like the
  log itself. It is excluded from Android's backups (`allowBackup="false"`). Unpair forgets it.
- **Each sync:** the phones prove they hold the pair key (HMAC over fresh nonces from both
  sides), then derive a key for this session only (HKDF). Each log crosses sealed with
  AES-256-GCM. A phone that doesn't hold the key is refused before any data is sent.
- **Nothing hand-rolled but HKDF**, a few lines over the platform's HMAC-SHA256 (RFC 5869). The
  rest is `java.security`: EC, ECDH, AES-GCM, SHA-256.

## When both phones logged the same sleep (0.64.0)

If both parents tap Start sleep before the phones meet, the merged log holds two sleeps for one
night. One gets its Woke up, the other stays open, and Cochilo used to say the baby had slept 21
hours (the Day ring summed 25). After every sync, and once when the app starts, `SleepRepair`
cleans this up:
- **Twins:** two sleeps that start within 5 minutes of each other and overlap are one sleep. The
  finished one stays; if both are finished, the more recently edited one. The other is deleted
  through `EventDao.delete`, so the deletion syncs and both phones keep the same one.
- **Stuck:** a sleep still open after a later sleep has started ends where the later one begins.
- **Totals:** the Day ring and its spoken summary count overlapping sleeps once, so a day never
  reads more than 24 hours.

A quick second nap a few minutes after a short one isn't a twin: they don't overlap.

## Limits, said plainly

- Without "Keep in sync when closed", both phones need Cochilo open for a change to go across.
  Opening the app catches up, so the two logs meet many times a day. With it on, the phones keep
  answering each other closed, at the cost of a little battery.
- After the phone restarts, the background sync waits until Cochilo is opened once (a start at
  boot would need another permission).
- Same Wi-Fi only. A guest network that isolates devices, or a phone on mobile data, can't see
  the other phone; the row says "Not found on this Wi-Fi".
- One partner phone per phone. Pairing again replaces the partner.

## Code

| What | Where | Tests |
|---|---|---|
| Merge | `sync/SyncMerge.kt` | `SyncMergeTest` |
| Crypto | `sync/SyncCrypto.kt` | `SyncCryptoTest` |
| Pairing and session, over any two streams | `sync/SyncProtocol.kt` | `SyncProtocolTest` (two phones over pipes) |
| Finding phones and sockets | `app/src/github/.../sync/SyncLinks.kt` (NSD); the play build's is a stub | — |
| State, pairing, auto-sync | `sync/PartnerSync.kt` | — |
| Twin and stuck sleeps, the day's total | `domain/SleepRepair.kt`, `Entries.repair` | `SleepRepairTest` |
| Deletions table | `data/CochiloDb.kt` (`DELETIONS`) | `DeletionsMigrationTest` |
| Who logged it (`events.loggedBy`, Room 5 → 6) | `data/CochiloDb.kt` (`LOGGED_BY`), `SyncMerge`, `PartnerSync.snapshot` | `DeletionsMigrationTest`, `SyncMergeTest` |
| With Cochilo closed | `app/src/github/.../sync/SyncBackground.kt` (`SyncService`); the play build's is a stub | — |
| Your initial, the switch | `ui/screens/Sync.kt` (`InitialSheet`) | `sync_settings_light`, `initial_sheet_light`, `entries_logged_by_partner_light` |
| Settings › Sync, the pairing sheet | `ui/screens/Sync.kt` | `sync_settings_light`, `pair_*` |
