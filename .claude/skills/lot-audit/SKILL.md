---
name: lot-audit
description: >-
  Audits kreadevis-backend lot branches for security, performance and
  architecture, according to CLAUDE.md/AGENTS.md and lots.md. Use at the end
  of a lot (before opening a PR), or when the user asks for an audit, a
  security or architecture review of lot work.
metadata:
  project: kreadevis-backend
  version: "1.0"
---

# Lot Audit — Security, Performance, Architecture

Audits the **current lot branch** before commit or PR. One consolidated
report. Fix every **Critical** finding before opening the PR.

## Prerequisites

1. Read `AGENTS.md` and the active lot section in `lots.md`.
2. Identify the lot from the branch name (`feat/lot-XX-slug`) or the context.
3. Scope the diff to the **branch changes** vs `main`.

## Workflow

```
Task Progress:
- [ ] Step 1 — Context (lot, diff, touched files)
- [ ] Step 2 — Security audit
- [ ] Step 3 — Performance audit
- [ ] Step 4 — Architecture audit
- [ ] Step 5 — Consolidated report
```

### Step 1 — Context

- `git diff main...HEAD --stat` — list the modified files under `src/`.
- Identify the lot's specific risks (e.g. lot 11 → Mailjet credentials;
  lot 15 → RBAC/ownership; PDF/CSV lots → injection and file paths).
- Read only the files touched by the lot and their direct dependencies.

### Step 2 — Security audit

Launch exactly one `security-review` subagent:

- `readonly: true`
- `run_in_background: false`
- `description: "Security Review"`
- `subagent_type: "security-review"`

Prompt:

```text
Full Repository Path: <absolute repo path>
Diff: branch changes vs main
Custom Instructions: Spring Boot 4 / Java 25 REST API for quote authoring,
migrated from a legacy MVC/JSP app. Check: hard-coded secrets (JWT_SECRET,
Mailjet keys), passwords leaked in logs/DTOs/responses, BCrypt usage, JWT
validation on protected routes, SQL injection via JPQL/native queries,
missing @Valid on HTTP inputs, CSV formula injection in exports, PDF
generation path handling (DOC_OUTPUT_DIR traversal), CORS configuration,
quote status guards bypassed (mutating finalized/cancelled quotes), legacy
view-layer remnants (JSP/JSTL/Servlets imports).
```

If the subagent fails, retry once, then fall back to the manual checklist in
[checklists.md](checklists.md).

### Step 3 — Performance audit

Review the diff against the **Performance** checklist in
[checklists.md](checklists.md). Focus:

- N+1 queries (lazy fetch in loops, repository calls inside iterations)
- Missing or oversized transactions on writes
- Unpaginated list endpoints (quotes, clients, professionals)
- PDF/CSV generation buffering whole documents unnecessarily

No subagent — direct diff review.

### Step 4 — Architecture audit

Review the diff against the **Architecture** checklist in
[checklists.md](checklists.md). Verify:

- Layering `controller → service → repository`, no business logic in controllers
- DTOs in `dto/`, MapStruct mappers in `mapper/`, no JPA entity over HTTP
- Business behavior without external dependency on the entity (rich domain)
- Schema change = new Liquibase changeset, never an edited merged one
- FR → EN translation continued on touched code, no legacy imports

No subagent — direct diff review.

### Step 5 — Consolidated report

Write the report to **`docs/audits/lot-XX.md`** using this template, in English:

```markdown
# Lot Audit — Lot XX — [branch name]

**Scope:** N modified files | **Verdict:** ✅ Ready for PR / ⚠️ Fix warnings / 🛑 Blocked (critical)

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     |          |         |      |
| Performance  |          |         |      |
| Architecture |          |         |      |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| …        | `path:line` | …    | …      |

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|

## Lot-specific notes
[JWT, Liquibase changesets, PDF/CSV, Mailjet, etc.]

## Recommended next steps
1. …
```

**Severity levels**

| Level | Meaning |
|-------|---------|
| **Critical** | Blocks the PR (security flaw, AGENTS.md violation, corruption risk) |
| **Warning** | Fix in this lot or track explicitly |
| **Info** | Optional improvement |

## lots.md enrichment

After writing the report, analyze the findings for cross-cutting patterns that
justify a dedicated lot in `lots.md`.

Propose a dedicated lot if **at least one** of these holds:

- A **Critical** finding cannot be fixed within the current lot's scope.
- **Warning** findings of the same dimension appear on **2+ consecutive lots**.
- A cross-cutting theme is detected: secrets, validation, entity exposure,
  CORS, JWT, export injection.

Procedure: add a **Recommended lot** section at the end of the report
(reason, proposed lot table, affected files) and **wait for user approval
before touching `lots.md`**.

## Rules

- Do not fix findings unless the user asks — report first.
- Stay within the lot's diff and its direct dependencies.
- Empty diff → one sentence: nothing to audit.
- Remind after the audit: `./mvnw verify` must be green before the PR.
- Never modify `lots.md` without explicit approval.

## Resources

- Detailed checklists: [checklists.md](checklists.md)
- Project conventions: `AGENTS.md`, `lots.md`, `security.md`
