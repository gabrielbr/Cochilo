# design/: how Cochilo looks, in three layers

Read this before changing a colour, a size, a duration, a type role, an icon or a component.

Cochilo is a **Tatá Slings** product built on the **Gabriel Design System**. That system is the
owner's boilerplate for all of his projects and is dressed in Gabriel's own brand. So the design
is split by who owns each decision:

| Layer | Owns | Lives in | Changed how |
|---|---|---|---|
| **Core** | The scale (spacing, radii, sizes, motion), semantic colour roles, type rules and floor, the brands mechanism, generic components and rules, the generator and the checks | The [Gabriel Design System](https://github.com/gabrielbr/Gabriel-Deisgn-System); here a pinned copy in [core/](core/) | In the design system first, then re-synced here ([core/CORE.md](core/CORE.md)). Never edited here |
| **Brand** | Tatá Slings' colour values, the one type family, the mark, her fabric textures | [brand/tata-slings/](brand/tata-slings/) | Here. A brand may swap colour roles and the family only (the core's `check_brands`) |
| **Product** | Cochilo's own tokens (kind colours, the Log button, the night light theme, sleep's bigger round…), its contrast pairs, its components | [product/cochilo/](product/cochilo/) | Here |

Until Cochilo 0.40.0 the brand and the product lived in the design system too, and every change
went there as a patch (`../design-system/`). The owner decided (2026-11-03) that the boilerplate
stays generic and Gabriel-branded, and a partner's brand and a product's own pieces live with the
product. The brand lives here, not in its own repo, until a second Tatá Slings product exists.

## Where does my change go?

1. **Could any of the owner's apps use it?** A new spacing step, a motion duration, a generic
   component (a link style, a checklist, a conversation pattern), an accessibility rule. That's
   **core**. Make it in the design system (a PR there), release it, then re-sync `core/`. If it
   can't wait, note it in [UPSTREAM.md](UPSTREAM.md) and say so in the PR.
2. **Is it about Tatá Slings' look?** A brand colour value, the type family, the mark, a texture.
   That's **brand**: edit `brand/tata-slings/`.
3. **Is it only Cochilo's?** A kind's colour, the Log button, the night light, a component only
   Cochilo has. That's **product**: edit `product/cochilo/` (tokens in `layer.json`, the component
   row in `components.md`).

## Build and check

```sh
python3 design/build.py           # writes app/src/main/java/br/gabriel/cochilo/ui/theme/CTokens.kt
python3 design/build.py --check   # the checks, and CTokens.kt must match (CI runs this)
python3 -m unittest discover -s design -p "test_*.py"
```

`build.py` puts the three layers together into the shape the core's generator reads, then runs
the core's own code. So it doesn't fork the generator, and re-syncing the core is a copy:
- it writes the Kotlin with `gen_tokens.cochilo_kt`;
- it runs every check that applies: layers resolve, values sit on the core scale, the type floor,
  the brand limits, and contrast for every pair in light, dark and night.

**Never edit `CTokens.kt` by hand.** It's generated. `--check` fails if it differs, the same rule
as before with the design system's `drift.py`, now checked in this repo.

## How a token is written

`product/cochilo/layer.json` → `layer`:
- `"@space4"` pulls a core token as is.
- An object is Cochilo's own: `{"name": "kind-badge-lead", "value": "44px", "usage": "…"}`.
- Colours carry `{"light": …, "dark": …, "night": …}`; night falls back to dark when it's missing
  (`theme_fallback`).
- `"{other}"` aliases another colour.
- `contrast` lists the pairs that must pass, with their minimum.

`brand/tata-slings/brand.json`: each colour names the role it `replaces`, with a value per theme.

The full token format is the core's: `core/scripts/tokens_lib.py`, and the design system's
`docs/tokens.md`.

## Docs here

- [core/CORE.md](core/CORE.md): which core version is vendored, from where, and how to re-sync it.
- [brand/tata-slings/brand.md](brand/tata-slings/brand.md): the brand, its values and its history;
  [textures/](brand/tata-slings/textures/) with source and permission.
- [product/cochilo/README.md](product/cochilo/README.md) and
  [components.md](product/cochilo/components.md): Cochilo's layer and its components.
- [UPSTREAM.md](UPSTREAM.md): what still has to go into the design system, and the plan for that PR.
- [audits/](audits/): the design audits written in the design system for Cochilo (0.5.0 to
  0.27.0). New ones go in `../docs/audits/` with the release they belong to.
