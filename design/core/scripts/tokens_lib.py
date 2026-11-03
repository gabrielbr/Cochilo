"""Load tokens/tokens.json and resolve each layer into flat, ordered token lists.

A layer lists its tokens in output order. A string "@name" pulls the core token of that name; an
object is a layer-only token. Colour values are {"light": hex, "dark": hex} (a dark-only layer may
omit light), or "{other}" to alias another colour of the same layer. Shared by gen_tokens.py,
check_tokens.py and drift.py so all three read the file the same way.
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
TOKENS = ROOT / "tokens" / "tokens.json"
GROUPS = ("color", "spacing", "radius", "size", "motion")


def load(path=TOKENS):
    return json.loads(pathlib.Path(path).read_text())


def core_index(t):
    """{group: {name: token}} for the core tokens."""
    return {g: {tok["name"]: tok for tok in t["core"].get(g, [])} for g in GROUPS}


def variant_colours(t):
    """{name: (variant name, token)} for every colour a variant defines (they can be pulled with "@")."""
    return {tok["name"]: (vname, tok) for vname, v in t.get("variants", {}).items() for tok in v.get("color", [])}


def resolve(t, layer_name):
    """{group: [token, ...]} for one layer, in the layer's order, "@" refs replaced by the core (or,
    for colours, variant) token they name."""
    core = core_index(t)
    for name, (vname, tok) in variant_colours(t).items():
        core["color"].setdefault(name, dict(tok, variant=vname))
    layer = t["layers"][layer_name]
    out = {}
    for g in GROUPS:
        items = []
        for entry in layer.get(g, []):
            if isinstance(entry, str):
                if not entry.startswith("@"):
                    raise ValueError(f"{layer_name}.{g}: bad entry {entry!r}")
                name = entry[1:]
                if name not in core[g]:
                    raise ValueError(f"{layer_name}.{g}: no core token {name!r}")
                items.append(dict(core[g][name], core="variant" not in core[g][name]))
            else:
                items.append(dict(entry, core=False))
        out[g] = items
    return out


def colour_map(t, layer_name):
    """{name: {theme: hex}} for one layer, aliases followed."""
    layer = t["layers"][layer_name]
    raw = {tok["name"]: tok["value"] for tok in resolve(t, layer_name)["color"]}
    out = {}

    def value(name, seen=()):
        if name in seen:
            raise ValueError(f"{layer_name}: alias loop at {name}")
        v = raw[name]
        if isinstance(v, str) and v.startswith("{"):
            return value(v.strip("{}"), seen + (name,))
        if isinstance(v, str):
            return {th: v for th in layer["themes"]}
        return v

    # A theme a token doesn't spell out falls back to another (layer "theme_fallback", e.g.
    # a night theme that falls back to dark), so a theme only lists the roles it changes.
    fallback = layer.get("theme_fallback", {})

    def filled(v):
        v = dict(v)
        for th in layer["themes"]:
            if th not in v and th in fallback and fallback[th] in v:
                v[th] = v[fallback[th]]
        return v

    for name in raw:
        out[name] = filled(value(name))
    # A brand (docs/brands.md) swaps colour roles after aliases are followed, so every reader
    # (generator, checks, drift) sees the app as it ships.
    b = brand(t, layer_name)
    if b:
        for tok in b.get("color", []):
            if tok["replaces"] in out:
                out[tok["replaces"]] = filled({th: tok["value"][th] for th in layer["themes"] if th in tok["value"]})
    return out


def brand(t, layer_name):
    """The brand a layer is dressed in (its "brand" key), or None."""
    name = t["layers"][layer_name].get("brand")
    return dict(t["brands"][name], key=name) if name else None


def brand_usage(t, layer_name):
    """{colour name: usage} for the colours a layer's brand replaces."""
    b = brand(t, layer_name)
    return {tok["replaces"]: tok.get("usage", "") for tok in b.get("color", [])} if b else {}


def const(name):
    return re.sub(r"[^A-Za-z0-9]+", "_", name).upper().strip("_")


def camel(name):
    parts = re.split(r"[^A-Za-z0-9]+", name)
    return parts[0].lower() + "".join(p[:1].upper() + p[1:] for p in parts[1:])


def num(value):
    """'16px' -> 16, '150ms' -> 150, '1.3em' -> 1.3, 700 -> 700."""
    s = str(value)
    for unit in ("px", "ms", "em", "dp", "sp"):
        s = s.replace(unit, "")
    f = float(s)
    return int(f) if f.is_integer() else f


def argb(hex_):
    """'#RRGGBB' or '#AARRGGBB' -> '0xAARRGGBB'."""
    h = hex_.lstrip("#")
    if len(h) == 6:
        h = "FF" + h
    return f"0x{h.upper()}"


def rgb(hex_):
    h = hex_.lstrip("#")
    if len(h) == 8:  # #AARRGGBB: composite is out of scope, read the colour itself
        h = h[2:]
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def luminance(hex_):
    def ch(c):
        c = c / 255
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (ch(c) for c in rgb(hex_))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    la, lb = sorted((luminance(a), luminance(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)
