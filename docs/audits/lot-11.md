# Lot Audit — Lot 11 — feat/lot-11-quote-email-send

**Scope:** 28 changed files (13 main sources, 3 tests, 4 config/yaml, 1 changeset, 1 template, 2 repo hygiene) | **Verdict:** ✅ Ready for PR (0 Critical)

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 1       | 2    |
| Performance  | 0        | 1       | 0    |
| Architecture | 0        | 0       | 1    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info | `application.yaml:41-42` | Mailjet `api-key` / `api-secret` read from env with **no default value** (§5 compliant). | None — correct. |
| Info | `MailjetEmailServiceImpl.java:41-46` | Only HTTP outcome and recipient are logged; credentials (Basic Auth) never logged. | None — correct. |
| Warning | `QuoteDocumentController.java:sendQuote` | No ownership check: any authenticated user can email any quote by id. | Consistent with current pre-RBAC state; ownership scoping is **lot 15**. Track, do not fix here. |

Additional verified points (no finding):
- **XSS**: template `quote.html` uses `th:text` only (no `th:utext`) for `customMessage`, `merchantName`, `clientName` → Thymeleaf autoescaping active.
- **Input validation**: `SendQuoteRequest` has `@Email` on `recipientOverride` and `@Size(max=1000)` on `customMessage`; controller uses `@Valid`.
- **Header/CRLF injection**: recipient is placed in a JSON body field (Mailjet v3.1), not an SMTP header → no CRLF injection surface.
- **Transport**: Mailjet URL defaults to `https://`; Basic Auth over TLS.
- **Kill switch**: `app.email.enabled=false` → 409 before any provider call.
- No real secret committed; `.env.example` holds empty placeholders and `.env` is gitignored.

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Warning | `QuoteEmailServiceImpl.java:49-79` | `sendQuoteToClient` is `@Transactional` and the synchronous Mailjet HTTP call runs **inside** the transaction, holding the DB connection for the duration of the external call. Under load this can exhaust the connection pool. | Acceptable at current single-user/low volume. Candidate refactor (send outside the tx, then persist `sentAt/sentTo` in a short tx) — track for a hardening pass, e.g. alongside lot 11b async reminders. |

Verified: no N+1 introduced; the quote is loaded once and `client` is read within the open session; `pdfService` reuses the ambient transaction.

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info | `service/EmailService.java` + `impl/MailjetEmailServiceImpl.java` | Provider-agnostic `EmailService` abstraction with a Mailjet implementation; `QuoteEmailService` composes PDF + template + send. Clean layering, swappable provider. | None — good design. |

Verified:
- Layering `controller → service → repository`, no business logic in the controller.
- DTOs live in `dto/`; no JPA entity crosses the HTTP boundary (`SendQuoteResponse` returned).
- Schema change is a **new** changeset (`003-add-quote-sent-tracking.yaml`) appended to the master changelog; no merged changeset edited.
- No legacy view-layer imports (JSP/JSTL/Servlets) introduced.

## Lot-specific notes
- **Mailjet credentials**: env-only (`MAILJET_API_KEY`, `MAILJET_API_SECRET`), no fallback, never logged. Fail-fast validation on these properties is deferred to **lot 16** (`@Validated` on config properties).
- **Liquibase**: changeset 003 exercised on H2 via the integration profile is still gated by the known SB4 Liquibase-inert reserve (lot 12 note); `KreadevisBackendApplicationTests` context load validates the new columns against `ddl-auto` schema.
- **Test coverage**: 121 tests green, JaCoCo gate met. Email send path covered at service (Mailjet payload + provider error), orchestration (trace persistence, override, 409, 422) and controller (200 / 401 / 422) levels.

## Recommended next steps
1. Open the PR for lot 11 (verdict ✅, no Critical).
2. Track the in-transaction external-call Warning for a later hardening pass (not blocking).
3. Ownership on `/send` will be closed by lot 15 (RBAC & ownership).

> Reminder: `./mvnw verify` must be green before the PR — verified green at audit time (121 tests, 0 failures, coverage met).
