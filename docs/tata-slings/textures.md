# Tatá's fabrics in Cochilo (textures)

Cochilo 0.9.0 lets the person pick one of Tatá's real fabrics; it wraps the Today card like a
sling wraps the baby. Settings › Appearance › Fabric: one tap on a swatch, applied at once. The
picker opens on the current fabric (remember actions). Default: Plain.

## The fabrics

Eleven crops from the product photos on tataslings.com.br (2026-11-02), fabric only (no label,
seam, face or hand), 384 × 384 px WebP, 244 KB together, in `app/src/main/res/drawable-nodpi/`.
Since 0.26.0 each is a true repeating tile, so it repeats like cloth: before, the photos were
mirrored at their edges, which turned prints like Tropical and Balão into a kaleidoscope.
`scripts/seamless_tile.py` makes the tile from the crop: a thin border off (edge folds and lens
falloff), the light flattened (divided by a very soft blur of itself, so tiles don't show as
lighter and darker blocks), then blended with its own half-offset copy, so the tile's edges are
the photo's middle and join across the wrap. © Tatá Slings, used with the owner's
permission; source photo per file in the design system's `assets/textures/tata-slings/README.md`.

| Plain fabrics | Prints |
|---|---|
| Pérola, Rosa Chá (Dry Fit), Capuccino, Mel, Trevo, Cereja (Dry Fit), Crepúsculo, Chumbo (Dry Fit) | Balão, Folhagem, Tropical |

The names are her product names, the same in English and Portuguese; "Plain" / "Liso" is no
fabric.

## Where a fabric shows, and why there

Only as the band of the **Fabric frame** round the Today card (`space16` on top since 0.45.0, like the fold of
the sling, `space4` on the sides) and as the picker's swatches. No word is ever drawn on a fabric.

Measured first, and rejected: the fabric washed behind the whole page. For 12–13sp muted text to
keep 4.5:1 over the darkest part of each photo, the page colour had to cover 84–96% of it in
light (93–96% for the prints), so the fabric no longer read. Washed behind the card's own text,
ink still needed 74–93% cover. A frame with no words shows the fabric at full strength and needs
no contrast trade at all.

## Code

- `ui/kit/Fabric.kt`: `Fabric` (key, name, drawable), `LocalFabric`, `FabricFrame`,
  `FabricPicker`.
- `data/Prefs.kt` `fabric`; `MainActivity` provides `LocalFabric`; `Settings.kt` shows the picker;
  `Today.kt` wraps the sleep panel.
- Screenshots: `today_fabric_balao_light`, `today_sleeping_fabric_tropical_dark`,
  `fabric_picker_ptBR_light`.

## Adding a fabric

1. Crop a square of fabric only from her photo (no label or seam), 384 px, then
   `python3 scripts/seamless_tile.py crop.png tex_<key>.webp` (WebP q78, a repeating tile).
2. Add it to the design system's `assets/textures/tata-slings/` with its source, then to
   `res/drawable-nodpi/tex_<key>.webp`.
3. Add the `Fabric` entry and `fabric_<key>` (her product name, `translatable="false"`) to both
   strings files; re-record the screenshots.
