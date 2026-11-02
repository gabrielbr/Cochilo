# Design-system change for Cochilo

The design system's rules say an app's values are decided there first. This folder holds that
change as a patch, because this session can only read the design-system repo.

**[0001-Cochilo-joins-and-wears-Tata-Slings-0.6.0.patch](0001-Cochilo-joins-and-wears-Tata-Slings-0.6.0.patch)**,
one commit on top of the design system's `main` at 0.4.0 (self-update):

- **0.5.0, the `cochilo` layer:** Compose, light and dark. `generated/cochilo/CTokens.kt` and
  `cochilo.css`, `drift.py --cochilo`, the `CO_` mark and three icons, audit
  `audits/2026-11-02-v0.5.0-cochilo.md`.
- **0.6.0, brands:** see `docs/brands.md` there. A brand swaps an app's colour roles and its one
  type family, nothing else. `brands.tata-slings` is worn by `cochilo`: teal accent, navy ink,
  Pérola page, Libre Franklin. `check_brands` enforces the limits. Also Libre Franklin + OFL and
  `assets/logos/cochilo-tata-mark.svg`. Audit `audits/2026-11-02-v0.6.0-brands.md`.
- **Components:** `docs/components.md` lists Cochilo's Slide row and Log button (the main action in
  the thumb's corner, press and drag to log).

**[0002-Cochilo-in-her-fabrics-0.7.0.patch](0002-Cochilo-in-her-fabrics-0.7.0.patch)**, on top of 0001:

- **0.7.0, "Tecidos":** Cochilo's data colours become Tatá's fabrics, each kind of entry gets a
  soft round for its icon (component *Kind icon*), a `fab` role for the Log button (Amora under the
  brand), rounder corners (core values), and the brand's ink, page, line and panel warm to plum.
  New contrast pairs pass. Audit `audits/2026-11-02-v0.7.0-tecidos.md`.

Both checked on a fresh checkout of the design system's `main`: `git am` applies it,
`check_tokens.py` reports 0 errors, the tests pass, and `drift.py --cochilo` against this repo
reports 0 differences.

To merge it there:

```sh
cd Gabriel-Deisgn-System
git checkout -b cochilo-tata
git am ../Cochilo/design-system/0001-*.patch ../Cochilo/design-system/0002-*.patch
python3 scripts/check_tokens.py && python3 -m unittest discover -s tests
python3 scripts/drift.py --cochilo ../Cochilo
git push -u origin cochilo-tata   # then open the PR with the repo's template
```

Once it's merged, delete this folder.
