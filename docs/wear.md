# On the watch (0.53.0)

For when both hands hold the baby: a Wear OS tile with **Start sleep / Woke up** and the two
breasts, one tap each from the wrist.

## For the parents

1. **Install it once.** Each release has `cochilo-wear-<version>.apk`, the watch's own app. It
   installs from a computer (`adb install` to the watch over Wi-Fi), or with a "Wear installer"
   app on the phone. The phone needs Cochilo 0.53.0 or later, from the same releases.
2. **Add the tile.** On the watch, swipe to the tiles, then add **Cochilo** at the end.
3. **Tap.** The tile shows "Awake since 14:20" (or "Asleep since…") with one big button, and the
   two breasts below it. The one used last has the Last mark, as on the phone: an outline and a
   dot in the feed colour (0.56.0). While a breastfeed runs, its side is filled and reads
   "Finish"; TalkBack says "Finish Left". A tap goes to the phone, which logs it, and the tile
   answers at once.

| Flow | Taps (on the watch) |
|---|---|
| Start sleep / Woke up / Back to sleep | 1 (swipe to the tile, tap) |
| Start a breastfeed on a side, or switch to it | 1 |
| Finish the breastfeed | 1 |

The live notification already shows on a paired watch with its buttons, and still does. The tile
is quicker to reach, and it stays put.

## How it's safe

- **The log stays on the phone.** The watch keeps only the last state the phone sent (asleep or
  awake, since when, which breast), in its own private storage, and sends taps.
- **Directly, never through the cloud.** Phone and watch talk through Google's wearable services
  (the Message API) only when the watch is connected directly (`isNearby`, Bluetooth). Google's
  services can also route over the internet when the two are apart; Cochilo never sends then.
  Nothing is written to the Data Layer, which syncs through the cloud.
- **Only Cochilo's own watch app.** Google's services deliver a message only between apps with the
  same application id and the same signing key. The phone's listener reads only `/cochilo/action`
  and `/cochilo/ask`, and does only what a tile button does (`WearBridge.act`), through `Entries`
  like every other button.
- No new permission on either side.

## Limits, said plainly

- The tile uses the watch's own font: a tile can't load Libre Franklin. It's recorded as the one
  product exception to the brand's family (design/product/cochilo/components.md, Watch tile).
- The time doesn't tick on the tile: it says "since 14:20", redrawn each minute and after every
  change.
- No phone nearby: the tile says "Open Cochilo on the phone, nearby." and sends nothing.
- The watch app is installed by hand for now. It could go to Play with the play build later.

## Code

| What | Where | Tests |
|---|---|---|
| The message format | `app/src/main/java/.../wear/WearWire.kt` (shared by both apps) | `WearWireTest` |
| The phone's side | `wear/WearBridge.kt` (`WearBridge`, `WearListener`) | — |
| The tile | `wear/` module: `CochiloTile`, `TileLayout`, `Watch` | `TileSnapshotTest` (`tile_*`) |
| Colours | `CTokens.Dark`, copied in at build time from the phone's generated tokens | `design/build.py --check` |
