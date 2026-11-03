# What goes back into the Gabriel Design System

The owner's decision (2026-11-03):
- The Gabriel Design System is his **boilerplate**, generic and in Gabriel's brand.
- Tatá Slings (a partner brand) and Cochilo's own pieces live **in Cochilo** (`design/brand`,
  `design/product`).

Before that, 24 patches in `../design-system/` carried everything to the design system, brand and
product included. This file sorts them, and is the brief for the one PR that makes the design
system's `main` the real source of the core again.

**Status:** not started. The design system repo has to be opened for it, and the owner asked to
be asked first (AGENTS.md "Never guess a repo").

## The patches, sorted

| Patch (version) | What it holds | Goes to |
|---|---|---|
| 0001 (0.5.0, 0.6.0) | The `cochilo` layer, `drift.py --cochilo`, the `CO_` mark; **the brands mechanism** (`docs/brands.md` rules, `brands` in tokens.json, `check_brands`, brand-aware `colour_map` and generators); the Tatá Slings brand and Libre Franklin | **Core:** the brands mechanism (with a neutral example). **Here:** the layer and the Tatá brand (done) |
| 0002 (0.7.0) | Her fabrics as data colours, the Kind icon, `fab`, rounder core radii, warmer brand neutrals | **Core:** the rounder radii, if the owner wants them for every app (else revert to the core's). **Here:** the rest |
| 0003 (0.8.0) | Textures, the Fabric frame and picker, "no text on a texture" | **Core:** the rule "no text on a texture" (generic). **Here:** the textures and the two components |
| 0004 (0.9.0) | `kind-badge-lead`, `outline`, the Last mark, Cochilo's motion written down | **Core:** the motion section (two durations, system easing, Remove animations). **Here:** sleep leads and the Last mark |
| 0005 (0.10.0) | Home widget | Here |
| 0006 (0.11.0) | Cochilo softer (palette, frame hem) | Here (brand and product) |
| 0007 (0.12.0) | Baby avatar, `illustration-*` | Here |
| 0008 (0.13.0) | Celebration | Here |
| 0009 (0.14.0) | Night light theme, `theme_fallback` | **Core:** `theme_fallback` (a layer theme falling back to another) in `tokens_lib`. **Here:** the night values |
| 0010 (0.15.0) | Earlier chips, quick-settings tile | Here |
| 0011 (0.16.0) | Filter chips on the night panel | **Core:** Filter chips as a component. **Here:** the night-panel use |
| 0012 (0.17.0) | Checklist | Core |
| 0013 (0.18.0) | Printable report | Core (a printable page from an app's values) |
| 0014 (0.19.0) | Expected window on the day ring | Here |
| 0015 (0.20.0) | Large text, the label scale cap | Core |
| 0016 (0.21.0) | Wide screens: rail, content width, width classes | Core |
| 0017 (0.21.1) | Seamless fabric tiles | Here |
| 0018 (0.22.0) | Drag to adjust | Here |
| 0019 (0.23.0) | The Conversation pattern | Core |
| 0020 (0.23.1) | A model's answer in a Conversation, always labelled | Core |
| 0021 (0.24.0) | The Quiet link | Core |
| 0022 (0.25.0) | The Tatá note | Here |
| 0023 (0.26.0) | `motion-stagger`, the cascade; asleep and awake | **Core:** a stagger duration, if generic. **Here:** the cascade and the timeline |
| 0024 (0.27.0) | A header's second icon | Core |

"Here" items are already in `design/` (done in Cochilo 0.40.1); nothing more to do for them.

## The PR on the design system, step by step

1. Branch from its `main` (0.4.0). Apply the **core** rows above: the brands mechanism,
   `theme_fallback`, motion, large text, wide screens, Filter chips, Checklist, Printable report,
   Conversation and model answer, Quiet link, the header's second icon, "no text on a texture",
   and the stagger duration if wanted. Take them from the patches, without the Cochilo layer or the
   Tatá brand.
2. In `tokens.json`, drop the `cochilo` layer and `brands.tata-slings`. Keep the brands mechanism
   with Gabriel's own brand as the default and the example. Drop `generated/cochilo/` and
   `generated/web/cochilo.css`, the Tatá textures, `cochilo-tata-mark.svg`, and the Cochilo-only
   rows of `docs/components.md`. Also drop `drift.py --cochilo`: Cochilo now checks itself with
   `design/build.py --check`.
3. Add a doc there for future agents, `docs/products.md`; the draft is below. Link it from its
   AGENTS.md and README, and add a rule to its AGENTS.md: "a partner brand and a product's own
   tokens and components live in the product's repo; only what every app can use comes here."
4. Version it (0.5.0 there, or whatever comes next), CHANGELOG, audit, checks green. Merge and
   release.
5. Back in Cochilo, re-sync `design/core/` from that release ([core/CORE.md](core/CORE.md)) and
   check that `CTokens.kt` didn't change, or understand every line that did. Then delete
   `../design-system/` and this file's table, and keep a line in the CHANGELOG.

## Draft: `docs/products.md` for the design system

> # Products, brands and the core
>
> This repo is the **core**: the scale, semantic roles, type rules, the brands mechanism, generic
> components and rules, the generator and the checks. It's dressed in Gabriel's brand.
>
> A product built on it keeps two layers in its own repo:
> - its **brand** (a partner's colours, one type family, mark and assets, within the brands rule:
>   colour roles and the family only);
> - its **product layer** (its own tokens and components, pulling core tokens with `"@name"`).
>
> The product vendors a tagged copy of this repo's `core` and `scripts/` and builds its tokens
> file from the three layers with this generator, unchanged. Cochilo does this in
> `Cochilo/design/` (`build.py`, `core/CORE.md`).
>
> **Where a change goes:** anything every app could use comes here first, as a PR and a release;
> the product re-syncs. A brand's or a product's own change stays in the product.

## Things to decide during that PR

- **Rounder core radii (0.7.0).** For every app, or Cochilo only? If Cochilo only, they become
  Cochilo tokens in `design/product/cochilo/layer.json`.
- **The `CO_` mark** (`assets/logos/co-mark.svg`). Cochilo's Gabriel-branded mark from 0.5.0,
  unused since Cochilo wears Tatá's mark. Remove it, or keep it as a product sample?
