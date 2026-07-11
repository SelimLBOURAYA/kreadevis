# kreadevis-backend

> Cross-cutting conventions (lot workflow, commits, branches, PRs, forbidden patterns, ask-before-doing, secrets, REST architecture, rich-domain, session startup, pre-commit gate, English-only docs) are loaded from `~/.claude/coding-conventions.md`. This file holds **only what is specific to kreadevis-backend**.

## Project
Quote-authoring application — migration of the legacy `kreadevis` (Spring Boot 3 MVC + JSP) to a pure REST API consumed by a separate Angular frontend.

## Stack
- **Backend**: Spring Boot 4, Java 25
- **Frontend**: Angular 21 (separate project, to be done once backend is stable)
- **Database**: PostgreSQL 17 (Docker)
- **Build**: Maven
- **Mapping**: MapStruct
- **Auth**: Spring Security + JWT (lot 3, implemented)
- **PDF / CSV**: OpenPDF, Apache Commons CSV

## Validation gate
```
./mvnw verify
```

## Language
- Code, comments, identifiers: **English**
- Commit messages: **English**
- Internal docs (`lots.md`, PR descriptions): **French accepted**
- Agent-facing instruction docs (`CLAUDE.md`, `AGENTS.md`, memory files): **English only** (global §11)

## Project-specific rules
- **Legacy banned**: no trace of the old project's view layer (JSP, JSTL, Servlets, Webjars). If you still see a suspicious import, it is a migration bug to handle in a dedicated lot.
- **In-progress FR → EN translation** of the legacy code: entities already translated (`Devis→Quote`, `Reservation→QuoteItem`, etc.). Continue translation lot by lot, never as a standalone task.
- **Migrations**: Liquibase (lot 10) — every schema change is a new changeset, never edit a merged one.
- **No `lot-ship` without `lot-audit`** — no push or PR without `docs/audits/lot-XX.md`.

## Skills

| # | Skill | File | Trigger |
|---|-------|------|---------|
| 1 | **sprint** | `skill/sprint/SKILL.md` | Start or continue a full sprint of chained lots. |
| 2 | **lot-test** | `skill/lot-test/SKILL.md` | Lot code complete — tests with coverage above the JaCoCo gate. |
| 3 | **lot-audit** | `skill/lot-audit/SKILL.md` | After `lot-test` — security / performance / architecture audit. |
| 4 | **lot-ship** | `skill/lot-ship/SKILL.md` | After `lot-audit` with no Critical — push + PR. |
| 5 | **harness-sync** | `skill/harness-sync/SKILL.md` | Docs/harness drifted from code, or harness update/improvement requested. |
| 6 | **dep-update** | `skill/dep-update/SKILL.md` | Before a PR or on demand — Maven dependency refresh (patch/minor auto, major on approval). |

**LOTD gate** (mandatory order per lot): `lot-test → lot-audit → lot-ship`

> **⛔ `lot-ship` gate is blocked** until `lot-audit` is complete for the current lot.
> `git push` and `gh pr create` are **forbidden** without a written audit report
> (`docs/audits/lot-XX.md`) with **no Critical** finding.

### Coverage threshold — ratchet

The JaCoCo gate currently **fails the build under 70 %** line coverage
(generated MapStruct mappers, `dto/`, `config/` and the main class excluded).
Target is **80 %**, to be reached by a dedicated test-coverage lot
(controllers + security). The threshold is a ratchet: it only goes **up**,
never down, and is never disabled to make a build pass.

### LOTD checklist (before every push)

| Step | Skill | Pass criterion | Deliverable |
|------|-------|----------------|-------------|
| 1 | `lot-test` | `./mvnw verify` green (tests + JaCoCo gate) | tests on the lot branch |
| 2 | **`lot-audit`** | consolidated report written, security-review executed | **`docs/audits/lot-XX.md`** |
| 3 | `lot-ship` | only if audit ✅ or ⚠️ without Critical | push + PR |

**Never skip step 2** — even in a chained sprint, even if previous lots are audited.
Each lot gets its own report. A Critical finding in `lot-audit` blocks `lot-ship`
and suspends the sprint. Recurring findings across lots → propose a dedicated lot
in `lots.md` — never modified without user approval.

Sprint chaining: first lot branches from `main`, each next lot from the previous
lot's branch, without waiting for merge.

## Commands

```bash
docker compose up -d       # PostgreSQL 17 on :5432
./mvnw spring-boot:run     # API on :8080
./mvnw verify              # compile + tests (H2) + JaCoCo gate
```

## Migration context
- Legacy project: `/home/selim/ENV/projets/kreadevis/` (mostly functional, main blocker on Spring Security)
- View layer (JSP, JSTL, Servlets, Webjars) to be abandoned entirely
- Quote reference format: `DDMMYY-NNN` with daily sequence

## Secrets / environment
| Variable | Lot | Usage |
|---|---|---|
| `POSTGRES_PASSWORD` | 1 | DB password |
| `JWT_SECRET` | 3 | JWT signing key |
| `MAILJET_API_KEY` | 10 | Mailjet API key (email reminders) |
| `MAILJET_API_SECRET` | 10 | Mailjet API secret |
