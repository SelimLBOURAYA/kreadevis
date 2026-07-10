---
name: harness-sync
description: >-
  Keeps the kreadevis-backend harness and documentation aligned with the code (CLAUDE.md,
  AGENTS.md, lots.md, README.md, skill/*/SKILL.md). Use after any edit to
  CLAUDE.md or the skill/ directory, when the code, stack, conventions or lot
  scope evolved and docs may have drifted, after a lot or a sprint, or when
  the user asks to refresh, fix or improve the harness.
metadata:
  project: kreadevis-backend
  version: "1.0"
---

# Harness Sync — Keep and improve the harness

Detects **drift** between the harness documentation and the reality of the
code, fixes it, and proposes harness **improvements** when relevant. Never
touches application code — only harness and documentation files.

## Managed files

| File | Role | Rule |
|------|------|------|
| `CLAUDE.md` | Conventions (the **how**) + documents census | Source of truth for conventions |
| `AGENTS.md` | Mirror of `CLAUDE.md` | **Always byte-identical** to `CLAUDE.md` (auto-synced by the `sync-claude-agents.sh` hook — verify, don't fight it) |
| `CONVENTIONS.md` | Cross-cutting conventions | **Always byte-identical** to the master `~/.claude/coding-conventions.md` — never edited locally, only re-copied from the master |
| `lots.md` | Scope, lots, contracts (the **what**) | Modified **only** with user approval (Status excepted) |
| `README.md` | Presentation, startup | Aligned with the real stack |
| `skill/*/SKILL.md` | Project skills | Versions, paths and rules consistent with `CLAUDE.md` |

> **Invariant `AGENTS.md = CLAUDE.md`**: any edit to one is replicated byte
> for byte in the other. Verify with `cmp CLAUDE.md AGENTS.md` at the end —
> the command must be silent.
>
> **Invariant `CONVENTIONS.md = ~/.claude/coding-conventions.md`**: verify
> with `cmp CONVENTIONS.md ~/.claude/coding-conventions.md` — must be silent.
> If it diverges, the master changed: re-copy it, never hand-edit the local
> copy.
>
> **Invariant documents census**: every file in the table above, every
> `skill/*/SKILL.md`, and every real `docs/audits/*.md` report must appear in
> `CLAUDE.md`'s `## Project documents` section. Add missing entries as a fix.

## Workflow

```
Task Progress:
- [ ] Step 1 — Fact collection (code reality)
- [ ] Step 2 — Drift detection
- [ ] Step 3 — Proposal (diff + verdict)
- [ ] Step 4 — Application after approval
- [ ] Step 5 — Consistency verification
```

### Step 1 — Fact collection

Read the repository reality, assuming nothing:

- **Stack**: `pom.xml` → Java version, Spring Boot parent version, key
  dependencies (Liquibase, MapStruct, OpenPDF, Commons CSV, jjwt).
- **Architecture**: real packages under `src/main/java/com/slim/kreadevis_backend/`.
- **Config**: `application.yaml`, env vars (`DATASOURCE_*`, `JWT_SECRET`, `DOC_*`, `CORS_ORIGINS`).
- **Lots**: current branch, `git log --oneline`, lot status in `lots.md`,
  reports in `docs/audits/`.
- **Commands**: Maven scripts, `docker-compose.yml`, CI (`.github/`).

### Step 2 — Drift detection

Compare Step 1 facts to the managed files. Flag every gap:

| Drift type | Example |
|------------|---------|
| **Stack** | Doc says "Spring Boot 3 / Java 21" but `pom.xml` says 4.0.6 / 25 |
| **Architecture** | Package added/renamed but missing from the layout section |
| **Commands** | Documented command obsolete or script renamed |
| **Lots** | `lots.md` doesn't reflect delivered/audited lots |
| **Skill** | A `SKILL.md` cites an outdated version, path or rule |
| **Skills table** | `CLAUDE.md` skills table ⇔ `skill/` directory mismatch |
| **Mirror** | `cmp CLAUDE.md AGENTS.md` differs |

Always cross-check: a version can appear in `CLAUDE.md`, `AGENTS.md`,
`README.md` **and** a `SKILL.md` — fix **all** occurrences.

### Step 3 — Proposal

Present a short report **before** any write:

```markdown
## Harness Sync — Report

**Detected drifts:** N

| # | File(s) | Drift | Proposed fix |
|---|---------|-------|--------------|
| 1 | CLAUDE.md, AGENTS.md | "Java 21" → "Java 25" | align with pom.xml |

**Harness improvements (optional):** [list or none]
```

Distinguish clearly:
- **Fixes** (factual drift) → apply after approval.
- **Improvements** (make the harness perform better) → suggest, never impose.

### Step 4 — Application

- Apply validated fixes with targeted edits (no massive rewrite).
- Every edit to `CLAUDE.md` is **replicated identically** to `AGENTS.md` (and vice versa).
- **`lots.md` is never modified without explicit user approval** (Status column excepted).
- No application code, Liquibase changeset or secret modification.

### Step 5 — Verification

- `cmp CLAUDE.md AGENTS.md` → must be silent.
- Re-read the fixed occurrences (`grep`) to confirm none was missed.
- Every file path referenced in any doc exists; every README command runs.
- Recap the touched files and invite to commit (`docs:` or `chore:`).

## Harness performance improvements

Beyond fixes, propose — without applying without approval — when relevant:

- **Clarity**: ambiguous rule, redundancy between files, contradictory instruction.
- **Concision**: long section diluting the important constraints.
- **Coverage**: convention actually followed in the code but undocumented.
- **Skill triggering**: a `SKILL.md` `description` too vague to fire at the right time.
- **Skills table consistency** in `CLAUDE.md`: every new skill must appear there.

## Rules

- Report drifts **before** fixing — no surprise writes.
- Never desynchronize `CLAUDE.md` and `AGENTS.md`.
- Never modify `lots.md` without explicit approval.
- Do not touch application code, migrations or secrets.
- Nothing to fix → one sentence: harness up to date.
- Do not commit or push unrequested (respect the `lot-ship` gate).

## Resources

- Conventions and skills table: `CLAUDE.md` / `AGENTS.md`
- Scope and lots: `lots.md`
- Stack reality: `pom.xml`, `application.yaml`, `docker-compose.yml`, `.github/`
