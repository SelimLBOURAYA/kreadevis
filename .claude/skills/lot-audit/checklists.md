# Lot Audit — Checklists

Checks specific to the kreadevis-backend Spring Boot REST API. Apply only to
the files modified in the current lot.

---

## Security

### Auth & secrets

- [ ] No secret, JWT key, Mailjet key or password hard-coded in committed code or config
- [ ] `JWT_SECRET`, `MAILJET_API_KEY`, `MAILJET_API_SECRET` read from env vars — no production default
- [ ] Passwords hashed with BCrypt — never plain in logs, responses or test assertions
- [ ] JWT validated (signature + expiry) on every protected route
- [ ] CORS restricted to `CORS_ORIGINS` — no `*` in production

### Input validation

- [ ] `@Valid` on every controller `@RequestBody`
- [ ] Quote reference `DDMMYY-NNN` validated/generated server-side, never trusted from the client
- [ ] Enum-like statuses validated at the boundary

### Exports & documents

- [ ] CSV export neutralizes formula injection (`=`, `+`, `-`, `@` cell prefixes)
- [ ] PDF/document output paths resolved under `DOC_OUTPUT_DIR` / `DOC_FACTURE_DIR` — no path traversal
- [ ] No user-controlled content interpreted as file path or template

### Persistence & JPA

- [ ] No JPQL string concatenation — named parameters only
- [ ] Native queries with bound parameters only
- [ ] JPA entities never serialized over HTTP (always DTO)
- [ ] No Liquibase changeset modified after merge — new change = new changeset

### Business rules

- [ ] Finalized/cancelled quotes reject item mutations → 409
- [ ] Totals HT/VAT/TTC recomputed server-side on every item change — client totals never trusted
- [ ] `vatRate` snapshot on items preserved

---

## Performance

### JPA queries

- [ ] No N+1: check `@OneToMany`/`@ManyToOne` lazy fetches and add explicit joins where needed
- [ ] `findAll()` never called without pagination on potentially large collections (quotes, clients)
- [ ] List filters applied in the database, not in memory

### Transactions

- [ ] Service write methods annotated `@Transactional`
- [ ] No `@Transactional` on controllers
- [ ] `@Transactional(readOnly = true)` for read paths

### Documents

- [ ] PDF/CSV generation streamed where possible — no unnecessary full buffering
- [ ] Email sending (Mailjet, lot 11) never blocks a request thread without timeout

---

## Architecture

### Package structure

- [ ] New code in the right package: `config/`, `controller/`, `dto/`, `entity/`, `exception/`, `mapper/`, `repository/`, `security/`, `service/`
- [ ] No business logic in controllers — delegate to services
- [ ] No persistence logic in services — delegate to repositories
- [ ] Entity ↔ DTO conversion via MapStruct mappers

### Domain model

- [ ] Business behavior with no external dependency lives on the entity (rich domain model, e.g. `quote.recomputeTotals()`)
- [ ] No Lombok `@Data` on entities with relations — `@Getter @Setter @EqualsAndHashCode(of = "id")`
- [ ] FR → EN translation continued on touched entities/fields

### Migration hygiene

- [ ] No legacy view-layer import (JSP, JSTL, Servlets, Webjars)
- [ ] Schema changes via new Liquibase changesets only

### Spring conventions

- [ ] `@RestController` + `/api/` prefix on routes
- [ ] Centralized error handling via `@RestControllerAdvice` — business `IllegalStateException` → 409
- [ ] Semantic HTTP codes: 201 on create, 204 on delete, 409 on business conflict
- [ ] Constructor injection only — no field `@Autowired`

### Tests

- [ ] Every endpoint covered by at least one integration test (`@WebMvcTest` / `@SpringBootTest` + H2)
- [ ] Business rules of the lot tested (see lot-test matrix)
- [ ] JaCoCo gate green — threshold never lowered

### Lot scope

- [ ] Changes limited to the lot's scope in `lots.md` — no scope creep
- [ ] No half-implemented features belonging to a future lot

---

## Lot-specific triggers

| Lot | Extra focus |
|-----|-------------|
| 11 / 11b | Mailjet credentials, email content injection, async sending |
| 12 | Status guards, VAT snapshot, totals integrity |
| 13 | Pagination correctness, page-size bounds |
| 14 | API hygiene — error envelope, HTTP codes, validation coverage |
| 15 | RBAC/ownership — horizontal privilege escalation |
| 16 | Security hardening — headers, rate limiting, secrets defaults removal |
