# Lot Audit — Lot 12 — feat/lot-12-quote-integrity

**Scope:** 16 modified files (645 +, 33 −) | **Verdict:** ✅ Ready for PR

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 0       | 1    |
| Performance  | 0        | 2       | 0    |
| Architecture | 0        | 1       | 1    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info     | `QuoteItemServiceImpl.java:36-99` | Ownership check (`item.getQuote().getId().equals(quoteId)`) prevents horizontal privilege escalation: an authenticated user cannot mutate another user's quote items via a mismatched `quoteId` path parameter. | Already fixed in this lot. |
| —        | —        | No hard-coded secrets, no password leakage, no SQL injection, no path traversal. JWT validation unchanged, BCrypt usage consistent, `@Valid` on DTOs maintained, no legacy imports. | — |

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Warning  | `QuoteServiceImpl.java:37-52` | `findAll` and `findByClientId` trigger lazy loading of `Quote.items` during MapStruct mapping → N+1 queries. Open-in-view is `false`, but `@Transactional(readOnly=true)` keeps the session open for the mapper. Each quote issues a separate SQL query for its items. | Pre-existing (from lot 6). Tracked for lot 13 (pagination will naturally cap list size) or a dedicated query-optimization lot. Not a regression. |
| Warning  | `QuoteServiceImpl.java:37` | `findAll(startDate, endDate)` has no pagination — returns all quotes in the date range. | Pre-existing. Lot 13 (pagination) is explicitly planned to address this. Not a regression. |

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Warning  | `db.changelog/db.changelog-master.yaml:3` | **Liquibase is inert** — Spring Boot 4 removed built-in Liquibase auto-configuration (`LiquibaseAutoConfiguration` absent from all SB4 jars). The changelog files exist and are well-structured but are NEVER executed. The `002-quote-vat-totals.yaml` changeset (new in this lot) will never be applied to production. The `file:` include path in the master (`db/changelog/changes/002-quote-vat-totals.yaml` relative to `db/changelog/db.changelog-master.yaml` → resolves to `db/changelog/db/changelog/changes/002-quote-vat-totals.yaml`, incorrect) is also masked by this. | Pre-existing project-level issue (from lot 10). Requires a dedicated lot: add the missing Liquibase autoconfiguration module (the SB4 equivalent of `spring-boot-starter-liquibase` or the new split-module name) and fix the include paths. Documented in the integration test Javadoc. |
| Info     | `Quote.java:68-82` | `recomputeTotals()` placed on the entity as an instance method — clean application of the rich domain model convention (§1). Replaces the anemic `QuoteTotals` utility class removed during this lot's review. | Already done. Sets the right pattern for future lots. |

## Lot-specific notes

**Status guards (items 1, 6, 7):**
- `LOCKED_STATUSES = {FINALIZED, CANCELLED}` — centralized, clean
- `loadModifiableQuote()` extracts the guard + throws `IllegalStateException` → 409
- Item ownership check (`item.quote.id == quoteId`) in `updateItem`/`deleteItem` → 404 if mismatch (OWASP: no resource-existence leak)
- Delete guard on FINALIZED quotes → 409

**VAT / totals (items 2, 3):**
- `vatRate` snapshotted on `QuoteItem` at creation
- `totalPriceHt` / `totalVat` / `totalPriceTtc` on `Quote`
- `recomputeTotals()` called after every item mutation (add/update/delete) and on finalize

**Date realignment (item 4):**
- `quote.setDate(LocalDate.now())` in `finalize()` before `assignReferenceCode`
- Test confirms 2020-date quote gets today's date on finalize

**Transactional reads (item 0):**
- `@Transactional(readOnly=true)` on all `QuoteServiceImpl` read methods + `PdfServiceImpl`
- Non-transactional integration test (`QuoteReadIntegrationTest`) catches regressions

**Tests:**
- 106 tests, 0 failures, JaCoCo gate green
- Integration test with real JWT token (login → use token), no `@Transactional` on class → proves real lazy-loading fix works
- New unit tests for ownership violation and delete guard

## Recommended next steps

1. **Dedicated lot: Restore Liquibase execution.** Spring Boot 4 split Liquibase out of the core autoconfigure. The project needs the correct module and a path fix in `db.changelog-master.yaml`. Without this, every Liquibase changeset (including `002-quote-vat-totals.yaml` from this lot) is dead code. This is the pre-existing issue from the lot 10 audit note — confirmed and scoped here.

2. **Lot 13 (pagination)** will naturally cap the N+1 impact on `findAll` by limiting page size to 100. If the N+1 remains noticeable after pagination, add `@EntityGraph` or `JOIN FETCH` in the repository.

3. **Lot 14 (API hygiene)** should add the `201 Created` + `Location` header for POST endpoints — `QuoteServiceImpl.create` already returns the created quote but the controller should set 201.
