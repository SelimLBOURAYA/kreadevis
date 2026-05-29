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
