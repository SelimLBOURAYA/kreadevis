# Lot Audit — Lot 14 — feat/lot-14-api-hygiene

**Scope:** 23 modified files (14 main, 9 test) + 3 new files (`OpenApiConfig.java`, `004-quote-fk-not-null.yaml`, `OpenApiSmokeTest.java`) | **Verdict:** ✅ Ready for PR

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 0       | 0    |
| Performance  | 0        | 0       | 0    |
| Architecture | 0        | 1       | 2    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| —        | —        | No finding (ran the full `security-review` methodology against the branch diff). `/v3/api-docs` and `/swagger-ui/**` are `permitAll` but only leak route/schema metadata, no secrets, and are disabled entirely (`springdoc.swagger-ui.enabled` **and** `springdoc.api-docs.enabled`) on the `prod` profile — a stricter lockdown than the lot spec asked for. `@JsonIgnoreProperties` removal from `Quote.client`, `Client.address`, `Client.quotes`, `QuoteItem.product`, `QuoteItem.quote` is safe: verified every controller in the diff returns a MapStruct DTO, no entity is ever serialized over HTTP. `User.password` keeps its `@JsonIgnore`. `Location` headers are built from server-generated `response.id()`, never user input. | — |

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| —        | —        | No finding. No new query, loop, or transaction boundary. PDF generation still buffers the whole document (pre-existing pattern, unrelated to this lot's `buildPdf` signature simplification). | — |

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Warning  | `pom.xml` | springdoc version is **3.1.0**, not the "2.x" the lot spec named — 2.x only targets Spring Boot 3; 3.x is the first line compatible with Spring Boot 4.1 (verified against `repo1.maven.org` metadata: latest 2.x is `2.8.17`, 3.1.0 is the current stable). Deliberate deviation, documented in `lots.md`, no action needed beyond that record. | Recorded in `lots.md` "Réalisé" block — no further action. |
| Info     | `src/main/resources/db/changelog/changes/004-quote-fk-not-null.yaml` | `addNotNullConstraint` on `quotes.client_id`/`created_by` will fail if any pre-existing row already has a null value on a real target DB (no such row can be created through the current API — `QuoteServiceImpl.create` always sets both — but a hand-edited row or an old dev DB could still violate it). | No action for this lot (no prod deployment yet, cf. lot 17 not started); flag before Liquibase runs against a shared/dev Postgres for the first time. |
| Info     | `AddressController`, `ProfessionalController` | Neither has a dedicated `@WebMvcTest` (pre-existing gap, not introduced by this lot — only `ClientController`/`ProductController`/`QuoteController` have controller tests). The 201+Location change on their `create()` endpoints is therefore only exercised indirectly (compiles, but no assertion on the new status/header). | Candidate for a coverage lot alongside the ratchet target (80%) already tracked in `CLAUDE.md`. |

## Lot-specific notes
- OpenAPI: `OpenApiConfig` (title, version, `bearerAuth` JWT `SecurityScheme`), `/v3/api-docs` reachable unauthenticated in dev, fully disabled in `prod`. Smoke-tested by `OpenApiSmokeTest` (`GET /v3/api-docs` → 200, `$.openapi` and `$.paths./api/clients` present).
- HTTP codes: `POST` create endpoints (`Client`, `Address`, `Product`, `Professional`, `Quote`, `QuoteItem.addItem`) → 201 + `Location`, body unchanged (full DTO, per explicit user decision 2026-08-06 — Location is additive, not a body reduction). `AuthController.login/register`, `finalize/pending/cancel`, `import`, `send` intentionally left at 200 — not resource-creation semantics.
- Entity hygiene: all `@JsonIgnoreProperties` removed from `entity/`; `git grep -i JsonIgnoreProperties src/main/java/.../entity/` returns nothing.
- `Quote.client` / `Quote.createdBy` → `@ManyToOne(optional = false)`, matching Liquibase changeset `004-quote-fk-not-null` (new changeset, no edit of a merged one).
- Facture scope removal verified complete: `git grep -i facture src/main` returns nothing (controller endpoint, service method, `buildPdf` "FACTURE" variant, `AppProperties.DocumentConfig.factureDir`, `application.yaml`/`application-integration-test.yaml` config, README env-var row all removed).
- `QuoteRepositoryTest.persistQuote` updated to persist a `Client`/`Address` first — required by the new NOT NULL constraint at the JPA mapping level (`ddl-auto: create-drop` in `@DataJpaTest`).
- `./mvnw clean verify`: 133 tests, 0 failures, 0 errors. JaCoCo line coverage 86.38% (596/690), threshold 70% — ratchet untouched.

## Recommended next steps
1. Ship this lot as-is — no Critical or Warning blocking findings.
2. Track the `AddressController`/`ProfessionalController` missing `@WebMvcTest` gap (Info above) against the existing coverage-lot plan in `CLAUDE.md` rather than opening a new dedicated lot — it's the same recurring theme already tracked, not a new one.
