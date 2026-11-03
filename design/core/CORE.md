# The vendored core

`design/core/` is a copy of the Gabriel Design System's core. Cochilo builds against it, so it
doesn't need the design system's repo to build.

| | |
|---|---|
| Copied from | `gabrielbr/Gabriel-Deisgn-System`, version **0.27.0** |
| What that is | The design system's `main` at `26e4f2d` (0.4.0), plus Cochilo's patches 0001–0024 (`../../design-system/`), applied locally at `cc86ad3` |
| Files | `core.json` (the `core` section of `tokens/tokens.json`); `scripts/tokens_lib.py`, `gen_tokens.py`, `check_tokens.py` (unchanged copies) |
| Copied on | 2026-11-03 (Cochilo 0.40.1) |

**Not yet an upstream release.** 0.27.0 exists only with Cochilo's patches applied: the design
system's own `main` is still at 0.4.0. [../UPSTREAM.md](../UPSTREAM.md) is the plan to fix that.
After that PR is merged and released, re-sync from the released tag and update this table.

## Rules

- **Never edit these files here.** A core change is made in the design system and copied in. If
  `build.py` needs something the core doesn't offer, add it to the core upstream (or, if it's
  Cochilo's own, to `../product/cochilo/`).
- Brand and product data (`brands`, `layers`, `contrast` for Cochilo) are **not** in `core.json`:
  they live in `../brand/` and `../product/`.

## Re-syncing to a new core release

```sh
DS=../Gabriel-Deisgn-System   # a checkout of the design system at the release tag
cp $DS/scripts/tokens_lib.py $DS/scripts/gen_tokens.py $DS/scripts/check_tokens.py design/core/scripts/
python3 -c 'import json,sys; t=json.load(open(sys.argv[1])); json.dump({"$comment": "Vendored from the Gabriel Design System (design/core/CORE.md).", "name": t["name"], "version": t["version"], "core": t["core"]}, open("design/core/core.json","w"), ensure_ascii=False, indent=2)' $DS/tokens/tokens.json
python3 design/build.py && python3 design/build.py --check
```

Then:
1. Update the table above (version, commit, date).
2. Look at what the new core changes in `CTokens.kt` (`git diff`).
3. Re-record the screenshots if any value moved, and note it in the CHANGELOG.

If the core renamed or removed a token Cochilo pulls with `"@…"`, the build says which one. Fix
it in `../product/cochilo/layer.json`.
