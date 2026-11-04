# Vendored skills

The four design skills live here, so every Claude Code session opened in Cochilo loads them on
any account or machine. AGENTS.md requires all four on every screen and flow ("Four skills on
every screen and flow"). They come from the Gabriel Design System, whose AGENTS.md requires them
in every app built on it.

| Skill | Source | Version |
|---|---|---|
| `ux-user-flows/` | Gabriel Design System 0.31.0, `.claude/skills/ux-user-flows` (the owner's own skill) | — |
| `impeccable/` | Gabriel Design System 0.29.0, `.claude/skills/impeccable` (pbakaus/impeccable skill 4.8.0; Apache-2.0, see its `LICENSE` and `NOTICE.md`) | 4.8.0 |
| `graphic-design-fundamentals/` | Gabriel Design System 0.29.0, `.claude/skills/graphic-design-fundamentals` (the owner's own skill) | — |
| `less-is-more/` | Cochilo's own copy (0.43.0), with Cochilo's examples; the generic one is in the design system's `.claude/skills/less-is-more` | 1.0 |

**To update:** when a design-system release changes a skill, copy its folder over this one and
update the table. Cochilo's `less-is-more` keeps the design system's five questions and output.
Only the examples are Cochilo's, so a change to the questions there is made here too.

**Impeccable on Android:**
- use `reference/audit.native.md` and `reference/android.md`;
- `detect` (a web scanner) doesn't apply to Compose;
- if its launcher can't run, read `SKILL.md` and the references by hand.
