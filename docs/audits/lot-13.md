# Lot Audit — Lot 13 — feat/lot-13-pagination

**Scope:** 21 modified files (13 main, 8 test) | **Verdict:** ✅ Ready for PR

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 0       | 0    |
| Performance  | 0        | 0       | 2    |
| Architecture | 0        | 0       | 1    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| —        | —        | No finding. All new `@Query` methods (`ClientRepository.search`, `ProductRepository.search`, `QuoteRepository.findByFilters`) use named JPQL bind parameters exclusively; `search` is never concatenated into the query string. Sort/pagination resolved by Spring Data against JPA entity property paths, not raw SQL. No auth, secrets, or data-exposure changes. | — |

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info     | `QuoteController.java:42` (`getByClient`) | `GET /api/clients/{clientId}/quotes` still returns an unbounded `List<QuoteResponse>` — out of this lot's scope by explicit user decision (2026-08-06), but the same unbounded-list risk this lot fixes elsewhere on a per-client basis. | Track for lot 14 or a follow-up if a client can accumulate many quotes. |
| Info     | `Client/ProductServiceImpl.findAll` | No `@Transactional(readOnly = true)` on the new paginated `findAll` (pre-existing pattern, unchanged by this lot — `QuoteServiceImpl.findAll` does have it). | No action required in this lot; consistent with prior state. |

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info     | `Client/ProductController.getAll`, `QuoteController.getAll` | Returning `Page<T>` directly triggers Spring's `PageImpl` serialization-stability warning at runtime (`Serializing PageImpl instances as-is is not supported`). This matches the lot spec's explicit choice to keep the default Spring format, so it is intentional, not a defect. | Revisit with `PagedModel`/`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` if the front-end needs a stable contract (candidate for lot 14 — API hygiene). |

## Lot-specific notes
- Pagination scope was explicitly restricted to `clients`/`products`/`quotes` per user decision recorded in `lots.md` (2026-08-06); `Address`/`User`/`Professional` `findAll()` remain `List<>`, out of scope.
- `application.yaml`: `spring.data.web.pageable.default-page-size: 20` / `max-page-size: 100` — verified via `ClientControllerTest.getAll_shouldCapPageSizeAt100` (`?size=1000` → capped to 100).
- No Liquibase changeset needed (no schema change).
- No legacy view-layer remnants introduced.

## Recommended next steps
1. Ship this lot as-is — no Critical or Warning findings.
2. Consider the two Info notes above when scoping lot 14 (API hygiene / OpenAPI).
