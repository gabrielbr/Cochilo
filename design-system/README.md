# Design-system change for Cochilo

The design system's rules say a new app gets a layer there before its first screen. This folder
holds that change as a patch, because this session couldn't push to the design-system repo.

**[0001-Cochilo-joins-the-system-0.4.0.patch](0001-Cochilo-joins-the-system-0.4.0.patch)**, made on
top of the design system's `main` at 0.3.0 (Fairy mode):

- a `cochilo` layer in `tokens/tokens.json` (Compose, light + dark), with six event data hues;
- the generator for `generated/cochilo/CTokens.kt` and `cochilo.css`, and `drift.py --cochilo`;
- the `CO_` mark and three icons (chart, moon, sun) in the existing line set;
- docs and the audit `audits/2026-11-01-v0.4.0-cochilo.md` (both skills, no P0/P1);
- version 0.4.0 and its CHANGELOG entry.

Checked on a fresh clone of the design system: `git am` applies it, `check_tokens.py` reports 0
errors, the tests pass, and `drift.py --cochilo` against this repo reports 0 differences.

To merge it there:

```sh
cd Gabriel-Deisgn-System
git checkout -b cochilo-layer
git am ../Cochilo/design-system/0001-Cochilo-joins-the-system-0.4.0.patch
python3 scripts/check_tokens.py && python3 -m unittest discover -s tests
python3 scripts/drift.py --cochilo ../Cochilo
git push -u origin cochilo-layer   # then open the PR with the repo's template
```

Once it's merged, delete this folder.
