# Lot Audit — Lot 15 — feat/lot-15-rbac-ownership

**Scope:** 33 modified files (19 main, 14 test) | **Verdict:** ✅ Ready for PR

Note: the `security-review` subagent type is not available in this
environment. This audit was performed manually against the checklist in
`skill/lot-audit/checklists.md`, with a full grep sweep of every
`quoteRepository`/`clientRepository`/`productRepository` owner-lookup call
site in `src/main` to confirm no unscoped path remains outside the
`isAdmin()` bypass branch.

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 0       | 1    |
| Performance  | 0        | 0       | 1    |
| Architecture | 0        | 0       | 1    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info | `service/impl/*ServiceImpl.java` (8 files) | ROLE_ADMIN bypass is re-derived from the JWT-backed `Authentication` on every call via `SecurityUtils.isAdmin()` — not spoofable client-side, but worth noting the role claim trust boundary is the JWT signature (unchanged from earlier lots). | No action — documented for awareness. |

Verified during this audit (all owner-scoped, no bypass found outside the
`isAdmin()` branch):
- `QuoteServiceImpl`: findAll, findById, findByClientId, findByReferenceCode, create (client lookup), finalize, pending, cancel, delete.
- `ClientServiceImpl` / `ProductServiceImpl`: findAll, findById, update, delete.
- `QuoteItemServiceImpl`: quote lookup (`loadModifiableQuote`) and product lookup (`getOwnedProduct`) for addItem/updateItem/deleteItem.
- `PdfServiceImpl.generateQuotePdf`, `QuoteEmailServiceImpl.sendQuoteToClient` — both were still using the unscoped `findByIdAndActiveTrue` after the first two commits; fixed in the third commit (`4ac6cc2`).
- `CsvImportServiceImpl` — reference-code lookup on import now checks `createdBy` for non-admins. A collision with another user's reference code is **rejected as a row-level error**: `reference_code` carries a global unique constraint (`uq_products_reference_code`), so the initial "create it as a new product" fallback would have violated that constraint and surfaced as a 500. Found during PR #26 review; the unit test mocked `save`, so the DB constraint was never exercised.
- `UserController` — `/me` stays self-service (no `@PreAuthorize` needed, scoped by JWT identity); `getAll`/`getById`/`delete` gated `hasRole('ADMIN')`.
- `AdminUserController` (`POST /api/admin/users`) — class-level `@PreAuthorize("hasRole('ADMIN')")`, creates `ROLE_ADMIN` users only (public registration still only grants `ROLE_USER`).
- 404 vs 403: every owner-scoped lookup throws `EntityNotFoundException` (→ 404) rather than an authorization exception when the resource belongs to another user — matches the OWASP guidance in the lot spec.
- `GlobalExceptionHandler` gained an explicit `AccessDeniedException → 403` handler. Without it, `@PreAuthorize` denials were being swallowed by the pre-existing `Exception.class` catch-all and returned as 500 — caught by the new `UserControllerTest`/`AdminUserControllerTest` 403 assertions during `lot-test`, fixed before this audit.
- New `@Query` methods (`searchByOwner`, `findByFiltersForOwner`) use named parameters only, no string concatenation — consistent with the existing `search`/`findByFilters` methods they mirror.
- No hard-coded secret, no password logged/serialized, no legacy view-layer import introduced.

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info | `security/SecurityUtils.getCurrentUser()` | Adds one extra `SELECT` on `users` per authenticated request for non-admins (on top of the JWT filter's own `UserDetailsService` lookup). Pre-existing pattern from `QuoteServiceImpl` (lot 10), now reused by 7 more services. | No action for this lot — if it becomes measurable at scale, cache the resolved `User` on the request or carry the id-only from `UserDetailsImpl` instead of re-querying. |

- No N+1 introduced: the new owner filters (`c.createdBy.id = :ownerId`) compile to a plain FK column predicate, no extra join fetch.
- All list endpoints stay paginated (unchanged from lot 13).
- Read paths keep `@Transactional(readOnly = true)`; writes keep `@Transactional`.

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info | `service/impl/*ServiceImpl.java` | The `securityUtils.isAdmin() ? unscoped : scoped` ternary was duplicated across 8 services / 12 call sites. | **Resolved** (PR #26 review) — extracted into `SecurityUtils.resolveOwned(Supplier<T>, Function<Long, T>)`, a single decision point. Accepted trade-off: `SecurityUtils` is mocked in unit tests, so `resolveOwned` has to be re-wired through `SecurityUtilsTestSupport.wireResolveOwned()` — one test shim against 12 production ternaries. |

- Layering respected: controllers stay thin, business logic in services, persistence in repositories.
- `SecurityUtils` is the single `getCurrentUser()`/`isAdmin()` source of truth now — the duplicate copy in `QuoteServiceImpl` was removed as part of this lot (first commit's follow-up).
- DTOs/entities separation unchanged; no entity returned over HTTP.
- Migration is a new changeset (`005-client-product-ownership.yaml`), no edit to a merged one; backfills existing rows before adding the `NOT NULL` constraint.
- No legacy view-layer import found.
- Tests co-located with the code they cover, added/updated in the same commits.

## Lot-specific notes

- **Scope note vs `lots.md`**: the lot 15 spec explicitly scopes "services `findAll`/`findById`" for read filtering, but the stated objective ("un utilisateur ne voit et **ne modifie** que ses propres ressources") requires the same scoping on mutations. This audit therefore treats the PDF/email/CSV-import/quote-item fixes in commit `4ac6cc2` as in-scope closures of the same objective, not scope creep — leaving them unscoped would have been a direct IDOR (a user finalizing/cancelling/downloading/emailing another user's quote by guessing the id).
- `./mvnw verify`: green, 162 tests, JaCoCo gate passed (see `lot-test` step).
- **Product reference codes stay globally unique.** Products are per-user (no sharing), so scoping `reference_code` uniqueness to `(created_by, reference_code)` was considered and deliberately not done in this lot — decision 2026-08-06. Consequence: two users cannot use the same reference code, and a CSV row hitting a foreign code gets an explicit error. Revisit if per-user reference namespaces become a requirement.
- Postman cross-account scenario from the lot spec (admin + user1 + user2, full cross-cutting check) is a manual QA step — not automated in this repo's test suite style (all existing controller tests are `@WebMvcTest` with mocked services). Recommend running it manually before merge if this ships to a shared environment.

## Recommended next steps
1. Merge this PR once reviewed.
2. Manually run the Postman cross-account scenario from the lot 15 spec before any shared-environment deploy.
3. (Optional, not blocking) Extract the repeated `isAdmin() ? … : …` ownership-lookup pattern into a `SecurityUtils` helper if a future lot touches these services again.
