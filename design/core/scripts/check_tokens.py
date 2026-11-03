#!/usr/bin/env python3
"""The design system's blockers, as a check. CI runs it on every PR; run it before you push.

    python3 scripts/check_tokens.py

Fails on: a malformed or unresolvable token, a core token missing from a layer, a spacing or
radius value outside the core scale, text under the 12px floor, a size off a layer's type scale,
a contrast pair under its minimum, a version that disagrees between tokens.json / VERSION /
CHANGELOG.md, or generated files that are stale. A contrast pair marked "known" is a tracked audit
finding: it warns while it fails, and fails once it passes (so the exception gets removed).
"""
import re
import subprocess
import sys

from tokens_lib import GROUPS, ROOT, colour_map, contrast, core_index, load, num, resolve

HEX = re.compile(r"^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
errors, warnings = [], []


def err(msg):
    errors.append(msg)


def check_layers(t):
    core = core_index(t)
    scale = {num(tok["value"]) for tok in t["core"]["spacing"]}
    radii = {num(tok["value"]) for tok in t["core"]["radius"]}
    floor = t["core"]["type"]["floor"]
    for name, layer in t["layers"].items():
        try:
            r = resolve(t, name)
            colours = colour_map(t, name)
        except (ValueError, KeyError) as e:
            err(f"{name}: {e}")
            continue
        for g in GROUPS:
            seen = [tok["name"] for tok in r[g]]
            dupes = {n for n in seen if seen.count(n) > 1}
            if dupes:
                err(f"{name}.{g}: duplicate {sorted(dupes)}")
            missing = set(core[g]) - set(seen)
            if missing:
                err(f"{name}.{g}: core tokens missing from the layer: {sorted(missing)}")
        for cname, v in colours.items():
            for th in layer["themes"]:
                if th not in v:
                    err(f"{name}.color.{cname}: no {th} value, and the layer ships {th}")
                elif not HEX.match(v[th]):
                    err(f"{name}.color.{cname}.{th}: {v[th]!r} is not #RRGGBB or #AARRGGBB")
        for tok in r["spacing"]:
            if num(tok["value"]) not in scale:
                err(f"{name}.spacing.{tok['name']}: {tok['value']} is not on the core scale")
        for tok in r["radius"]:
            if num(tok["value"]) not in radii:
                err(f"{name}.radius.{tok['name']}: {tok['value']} is not a core radius")
        type_scale = set(layer.get("typeScale", []))
        for group in layer.get("type", []):
            for st in group["styles"]:
                size = num(st["fontSize"])
                if size < floor:
                    err(f"{name}.type.{st['name']}: {size}px is under the {floor}px floor")
                if type_scale and size not in type_scale:
                    err(f"{name}.type.{st['name']}: {size}px is not on the layer's typeScale")


def check_shared_light(t):
    """Views kits look a dark value up by its light value (UiTokens.dark(int)). Two tokens with one
    light value and different dark values collapse to the first one, so the later one must be marked
    "pair" (read as color(context, X, X_DARK)), and a "pair" mark with no collision is stale.
    Only the Views kit (uikit) looks colours up this way; Compose layers read Light and Dark palettes."""
    for name, layer in t["layers"].items():
        if name != "uikit" or "light" not in layer["themes"] or "dark" not in layer["themes"]:
            continue
        colours = colour_map(t, name)
        tokens = resolve(t, name)["color"]
        seen = {}
        for tok in tokens:
            if isinstance(tok["value"], str):  # an alias shares its target's dark value too
                continue
            light, dark = colours[tok["name"]]["light"].upper(), colours[tok["name"]]["dark"].upper()
            first = seen.get(light)
            clash = first is not None and first[1] != dark
            if clash and not tok.get("pair"):
                err(f"{name}.color.{tok['name']}: same light value as {first[0]} with a different dark value; mark it \"pair\": true")
            if tok.get("pair") and not clash:
                err(f"{name}.color.{tok['name']}: marked pair but shares no light value with an earlier token")
            seen.setdefault(light, (tok["name"], dark))


DATA_PREFIXES = ("event-", "metric-", "stage-", "zone-", "fairy-")


def check_brands(t):
    """A brand swaps colour roles and the type family, nothing else (docs/brands.md): each colour it
    defines replaces a colour the layers that wear it have, never a data colour, with a value for
    every theme those layers ship; and it names its family and faces."""
    brands = t.get("brands", {})
    for bname, b in brands.items():
        if bname.startswith("$"):
            continue
        if not b.get("name"):
            err(f"brands.{bname}: no name")
        ty = b.get("type", {})
        if not ty.get("family") or not ty.get("faces"):
            err(f"brands.{bname}.type: needs family and faces")
        extra = set(b) - {"name", "doc", "source", "type", "color", "$comment"}
        if extra:
            err(f"brands.{bname}: a brand swaps colours and the type family only, not {sorted(extra)}")
    for lname, layer in t["layers"].items():
        bname = layer.get("brand")
        if not bname:
            continue
        if bname not in brands:
            err(f"{lname}: brand {bname!r} is not in brands")
            continue
        names = {tok["name"] for tok in resolve(t, lname)["color"]}
        for tok in brands[bname].get("color", []):
            target = tok.get("replaces")
            if target not in names:
                err(f"brands.{bname}: replaces {target!r}, which {lname} doesn't have")
            if target and target.startswith(DATA_PREFIXES):
                err(f"brands.{bname}: replaces {target}, a data colour; data colours stay the system's")
            for th in layer["themes"]:
                if th not in tok.get("value", {}) and th not in layer.get("theme_fallback", {}):
                    err(f"brands.{bname}.{target}: no {th} value, and {lname} ships {th}")


def check_variants(t):
    """A variant colour replaces one token. A layer that carries any colour of a variant carries the
    replacement for every replaced token it has, so the swap can't leave half the brand behind."""
    for vname, variant in t.get("variants", {}).items():
        for tok in variant.get("color", []):
            if not tok.get("replaces"):
                err(f"variants.{vname}.{tok['name']}: says nothing about what it replaces")
        for lname in t["layers"]:
            names = {tok["name"] for tok in resolve(t, lname)["color"]}
            carried = [tok for tok in variant.get("color", []) if tok["name"] in names]
            if not carried:
                continue
            for tok in variant.get("color", []):
                if tok.get("replaces") in names and tok["name"] not in names:
                    err(f"{lname}: carries variant {vname} but not {tok['name']} (replaces {tok['replaces']})")
            for tok in carried:
                if tok.get("replaces") not in names:
                    err(f"{lname}: {tok['name']} replaces {tok.get('replaces')}, which the layer doesn't have")


def with_variant(t, lname, colours, vname):
    """The layer's colours with the variant's replacements swapped in."""
    swapped = dict(colours)
    for tok in t["variants"][vname].get("color", []):
        if tok["name"] in colours and tok.get("replaces") in colours:
            swapped[tok["replaces"]] = colours[tok["name"]]
    return swapped


def contrast_cases(t, layer_names):
    """Each layer as shipped, then once per variant it carries, with the variant swapped in."""
    for lname in layer_names:
        colours = colour_map(t, lname)
        yield lname, None, colours
        for vname, variant in t.get("variants", {}).items():
            if any(tok["name"] in colours for tok in variant.get("color", [])):
                yield lname, vname, with_variant(t, lname, colours, vname)


def check_contrast(t):
    for pair in t["contrast"]:
        for lname, vname, colours in contrast_cases(t, pair["layers"]):
            layer = t["layers"][lname]
            for th in pair.get("themes", layer["themes"]):
                def pick(ref):
                    if ref.startswith("#"):
                        return ref
                    if ref not in colours:
                        raise KeyError(ref)
                    return colours[ref][th]
                try:
                    fg, bg = pick(pair["fg"]), pick(pair["bg"])
                except KeyError as e:
                    err(f"contrast: {lname} has no colour {e}")
                    continue
                ratio = contrast(fg, bg)
                label = f"{lname}{'+' + vname if vname else ''}/{th}: {pair['fg']} on {pair['bg']} = {ratio:.2f}:1 (min {pair['min']})"
                ok = ratio >= pair["min"]
                if pair.get("known"):
                    if ok:
                        err(f"{label} now passes: remove known={pair['known']} and close the audit item")
                    else:
                        warnings.append(f"{label} [tracked: {pair['known']}]")
                elif not ok:
                    err(label)


def check_version(t):
    v = t["version"]
    file_v = (ROOT / "VERSION").read_text().strip()
    if file_v != v:
        err(f"VERSION is {file_v}, tokens.json is {v}")
    m = re.search(r"^## \[?(\d+\.\d+\.\d+)", (ROOT / "CHANGELOG.md").read_text(), re.M)
    if not m or m.group(1) != v:
        err(f"CHANGELOG.md's latest entry is {m.group(1) if m else 'missing'}, tokens.json is {v}")


def check_generated():
    res = subprocess.run([sys.executable, str(ROOT / "scripts/gen_tokens.py"), "--check"],
                         capture_output=True, text=True)
    if res.returncode:
        err(res.stdout.strip() or res.stderr.strip())


def main():
    t = load()
    check_layers(t)
    check_shared_light(t)
    check_brands(t)
    check_variants(t)
    check_contrast(t)
    check_version(t)
    check_generated()
    for w in warnings:
        print("warning:", w)
    for e in errors:
        print("error:", e)
    print(f"check-tokens: {len(errors)} error(s), {len(warnings)} tracked warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
